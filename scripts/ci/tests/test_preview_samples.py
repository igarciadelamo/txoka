import base64
import copy
import importlib.util
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest
import xml.etree.ElementTree as ET


SPEC = importlib.util.spec_from_file_location("preview_samples", Path(__file__).parents[1] / "preview_samples.py")
samples = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(samples)
ROOT = Path(__file__).parents[3]
SCHEMAS = ROOT / "app/schemas" / samples.DATABASE_CLASS
ANDROID = "{http://schemas.android.com/apk/res/android}"


class PreviewSamplesTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.source = self.directory / "TxokaDatabase.kt"
        self.appearance = self.directory / "CardAppearance.kt"
        self.schemas = self.directory / "schemas"
        self.schemas.mkdir()
        self.output = self.directory / "assets/preview-samples"
        self.schema = json.loads((SCHEMAS / "2.json").read_text())
        (self.schemas / "2.json").write_text(json.dumps(self.schema))
        self.source.write_text("@Database(entities = [ReceivedContactEntity::class], version = 2, exportSchema = true)\n"
                               "abstract class TxokaDatabase : RoomDatabase()")

    def generate(self):
        samples.generate(self.source, self.schemas, self.appearance, self.output)
        connection = sqlite3.connect(self.output / "txoka.db")
        connection.row_factory = sqlite3.Row
        self.addCleanup(connection.close)
        return connection

    def appearance_schema(self, materials=True):
        schema = copy.deepcopy(self.schema)
        database = schema["database"]
        old_identity = database["identityHash"]
        database.update(version=3, identityHash="9aa5f45b450e01f693b4cc11a905bce9")
        database["setupQueries"] = [query.replace(old_identity, database["identityHash"])
                                    for query in database["setupQueries"]]
        entity = database["entities"][0]
        entity["createSql"] = entity["createSql"][:-1] + ", `appearance` TEXT)"
        entity["fields"].append(dict(fieldPath="appearance", columnName="appearance", affinity="TEXT"))
        (self.schemas / "3.json").write_text(json.dumps(schema))
        self.source.write_text(self.source.read_text().replace("version = 2", "version = 3"))
        extra = "STAINED_GLASS(2), NACRE(3), BRUSHED_METAL(4)," if materials else ""
        self.appearance.write_text("enum class CardBackground(val wireId: Int) {"
                                   f"GRADIENT(0), HOLOGRAM(1), {extra} FLAT(6)," + "}")
        return database

    def test_legacy_fixture_matches_compiled_schema_without_appearance_source(self):
        connection = self.generate()
        self.assertEqual(2, connection.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(self.schema["database"]["identityHash"],
                         connection.execute("SELECT identity_hash FROM room_master_table WHERE id=42").fetchone()[0])
        contacts = connection.execute("SELECT * FROM received_contacts ORDER BY id").fetchall()
        self.assertEqual(8, len(contacts))
        self.assertEqual(samples.LEGACY_COLUMNS, set(contacts[0].keys()))
        self.assertEqual(8, len({row["emailKey"] for row in contacts}))
        self.assertTrue(all(row["email"].endswith("@example.com") and row["phone"] is None for row in contacts))
        self.assertEqual({"received_contacts", "room_master_table", "sqlite_sequence"},
                         {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")})
        self.assertFalse(json.loads((self.output / "manifest.json").read_text())["ownProfile"])

    def test_compiled_version_selects_exact_schema_and_ignores_higher_files(self):
        (self.schemas / "3.json").write_text("deliberately invalid newer schema")
        connection = self.generate()
        self.assertEqual(2, connection.execute("PRAGMA user_version").fetchone()[0])

    def test_appearance_fixture_matches_room_three_identity_and_known_style_contract(self):
        schema = self.appearance_schema()
        connection = self.generate()
        self.assertEqual(3, connection.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(schema["identityHash"],
                         connection.execute("SELECT identity_hash FROM room_master_table WHERE id=42").fetchone()[0])
        appearances = [row[0] for row in connection.execute("SELECT appearance FROM received_contacts ORDER BY id")]
        self.assertEqual(1, appearances.count(None))
        raw = [self.assert_valid_style(style, {0, 1, 2, 3, 4, 6}) for style in appearances if style]
        self.assertEqual({0, 1, 2, 3, 4, 6}, {value[0] & 7 for value in raw})
        self.assertEqual(16, max(map(len, raw)))
        self.assertEqual("ok", connection.execute("PRAGMA integrity_check").fetchone()[0])

    def test_styles_never_emit_material_identifiers_missing_from_compiled_enum(self):
        self.appearance_schema(materials=False)
        connection = self.generate()
        for row in connection.execute("SELECT appearance FROM received_contacts WHERE appearance IS NOT NULL"):
            self.assert_valid_style(row[0], {0, 1, 6})

    def test_unknown_version_missing_schema_and_version_mismatch_fail_closed(self):
        self.source.write_text(self.source.read_text().replace("version = 2", "version = 4"))
        with self.assertRaisesRegex(ValueError, "do not support"):
            self.generate()
        self.source.write_text(self.source.read_text().replace("version = 4", "version = 3"))
        with self.assertRaises(FileNotFoundError):
            self.generate()
        (self.schemas / "3.json").write_text(json.dumps(self.schema))
        with self.assertRaisesRegex(ValueError, "differs from the compiled"):
            self.generate()

    def test_reassigned_background_identifier_fails_generation(self):
        self.appearance_schema()
        self.appearance.write_text(self.appearance.read_text().replace("STAINED_GLASS(2)", "STAINED_GLASS(5)"))
        with self.assertRaisesRegex(ValueError, "immutable STYLE"):
            self.generate()

    def test_provider_is_nonexported_and_exists_only_in_preview_manifest(self):
        preview = ET.parse(ROOT / "app/src/preview/AndroidManifest.xml")
        provider = preview.find("application/provider")
        self.assertEqual("false", provider.get(ANDROID + "exported"))
        self.assertEqual("${applicationId}.preview-samples", provider.get(ANDROID + "authorities"))
        main = ET.parse(ROOT / "app/src/main/AndroidManifest.xml")
        self.assertFalse(any("PreviewSamples" in node.get(ANDROID + "name", "") for node in main.iter()))

    def assert_valid_style(self, style, known_backgrounds):
        self.assertTrue(style.startswith("1:"))
        encoded = style[2:]
        raw = base64.urlsafe_b64decode(encoded + "=" * (-len(encoded) % 4))
        self.assertEqual(encoded, base64.urlsafe_b64encode(raw).decode().rstrip("="))
        self.assertTrue(3 <= len(raw) <= 16)
        first, second, third = raw[:3]
        self.assertIn(first & 7, known_backgrounds)
        self.assertEqual(0, (first | third) & 0x80)
        self.assertLessEqual(second & 7, 5)
        self.assertLessEqual((second >> 3) & 7, 5)
        self.assertLessEqual((third >> 2) & 7, 4)
        self.assertFalse(first & 7 == 6 and first & 8)
        escaped = ((first >> 4) & 7) == 7
        expected = 3 + escaped + (6 if first & 8 else 0) + (3 if third & 0x20 else 0) + (3 if third & 0x40 else 0)
        self.assertEqual(expected, len(raw))
        if escaped:
            self.assertIn(raw[3], {160, 161, 162, *range(192, 212)})
        cursor = 3 + escaped + (6 if first & 8 else 0)
        for finish, flag in ((second & 7, 0x20), ((second >> 3) & 7, 0x40)):
            if third & flag:
                default = bytes((0x9B, 0xA5, 0xB5)) if finish == 4 else b"\xff\xff\xff"
                self.assertNotEqual(default, raw[cursor:cursor + 3])
                cursor += 3
        return raw


if __name__ == "__main__":
    unittest.main()
