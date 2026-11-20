"""Generate preview-only received contacts from the schema compiled by this checkout."""

import argparse
import base64
import json
from pathlib import Path
import re
import sqlite3


DATABASE_CLASS = "es.igarciadelamo.txoka.data.received.TxokaDatabase"
LEGACY_COLUMNS = {
    "id", "name", "phone", "email", "linkedin", "instagram", "phoneKey", "emailKey",
    "note", "receivedAt", "accentHex", "accent2Hex",
}
CONTACTS = [
    ("Alex Preview", "alex.preview", "#4F46E5", "#06B6D4"),
    ("Maya Example", "maya.example", "#12314F", "#2F8F9D"),
    ("Noa Sample", "noa.sample", "#392568", "#284C70"),
    ("Leo Preview", "leo.preview", "#EEE7DC", "#EEE7DC"),
    ("Iris Example", "iris.example", "#551A49", "#165C6E"),
    ("Sam Sample", "sam.sample", "#DFCEE9", "#A7D8DD"),
    ("Ada Preview", "ada.preview", "#344457", "#0D1B2B"),
    ("Robin Example", "robin.example", "#D6BAE8", "#9FD8DD"),
]


def compiled_version(source):
    annotation = re.search(r"@Database\s*\((.*?)\)\s*abstract\s+class\s+TxokaDatabase\b", source, re.S)
    version = re.search(r"\bversion\s*=\s*(\d+)\b", annotation[1]) if annotation else None
    if not version:
        raise ValueError("Cannot determine the compiled TxokaDatabase version")
    return int(version[1])


def background_ids(source):
    enum = re.search(r"enum\s+class\s+CardBackground\b[^\{]*\{(.*?)\}", source, re.S)
    if not enum:
        raise ValueError("Appearance schema requires the compiled CardBackground declaration")
    values = {name: int(value) for name, value in re.findall(r"\b([A-Z_]+)\((\d+)\)", enum[1])}
    contract = {"GRADIENT": 0, "HOLOGRAM": 1, "STAINED_GLASS": 2, "NACRE": 3, "BRUSHED_METAL": 4, "FLAT": 6}
    if any(values.get(name) != value for name, value in contract.items() if name in values):
        raise ValueError("Background identifiers differ from the immutable STYLE contract")
    if not {0, 1, 6}.issubset(values.values()):
        raise ValueError("Missing established background identifiers")
    return set(values.values())


def styles(available):
    def encode(background, *tail):
        first = (background & ~7) | ((background & 7) if (background & 7) in available else 0)
        return "1:" + base64.urlsafe_b64encode(bytes((first, *tail))).decode("ascii").rstrip("=")

    return [
        None,
        encode(0x10, 0x59, 0x05),
        encode(0x79, 0x6C, 0x09, 192, 80, 32, 112, 24, 100, 128),
        encode(0x06, 0x43, 0x61, 22, 22, 26, 36, 36, 43),
        encode(0x4A, 0x8D, 0x01, 156, 47, 72, 37, 122, 133),
        encode(0x63, 0xD4, 0x65, 44, 30, 60, 50, 40, 66),
        encode(0x7C, 0xD4, 0x73, 211, 96, 128, 160, 30, 48, 72, 232, 238, 245, 220, 230, 240),
        encode(0x38, 0xAB, 0x2C, 177, 217, 226, 184, 159, 212, 12, 24, 40),
    ]


def generate(database_source, schema_directory, appearance_source, output):
    version = compiled_version(database_source.read_text())
    if version not in (2, 3):
        raise ValueError(f"Preview samples do not support Room version {version}")
    schema = json.loads((schema_directory / f"{version}.json").read_text())["database"]
    if schema["version"] != version:
        raise ValueError("Exported schema version differs from the compiled database")
    if len(schema["entities"]) != 1 or schema["entities"][0]["tableName"] != "received_contacts":
        raise ValueError("Preview fixtures may contain only the received library")
    entity = schema["entities"][0]
    columns = [field["columnName"] for field in entity["fields"]]
    expected = LEGACY_COLUMNS | ({"appearance"} if version == 3 else set())
    if set(columns) != expected or len(columns) != len(expected):
        raise ValueError("Unsupported received contact schema")
    identity = schema["identityHash"]
    if not re.fullmatch(r"[a-f0-9]{32}", identity):
        raise ValueError("Invalid Room identity hash")
    appearances = styles(background_ids(appearance_source.read_text())) if version == 3 else [None] * 8
    output.mkdir(parents=True, exist_ok=True)
    database = output / "txoka.db"
    database.unlink(missing_ok=True)
    with sqlite3.connect(database) as connection:
        connection.execute("PRAGMA journal_mode=DELETE")
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", "received_contacts"))
        for index in entity.get("indices", []):
            connection.execute(index["createSql"].replace("${TABLE_NAME}", "received_contacts"))
        for query in schema["setupQueries"]:
            connection.execute(query)
        connection.execute(f"PRAGMA user_version={version}")
        for index, (name, handle, accent, accent2) in enumerate(CONTACTS):
            email = f"{handle}@example.com"
            row = dict(id=index + 1, name=name, phone=None, email=email, linkedin=None, instagram=None,
                       phoneKey="", emailKey=email, note="Fictional preview contact", receivedAt=1795132800000 - index,
                       accentHex=accent, accent2Hex=accent2)
            if version == 3:
                row["appearance"] = appearances[index]
            names = ",".join(f"`{column}`" for column in columns)
            placeholders = ",".join("?" for _ in columns)
            connection.execute(f"INSERT INTO received_contacts ({names}) VALUES ({placeholders})",
                               [row[column] for column in columns])
        if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
            raise ValueError("Generated fixture failed SQLite integrity checking")
        if connection.execute("SELECT identity_hash FROM room_master_table WHERE id=42").fetchone() != (identity,):
            raise ValueError("Generated fixture differs from the Room identity hash")
    (output / "manifest.json").write_text(json.dumps({
        "databaseVersion": version, "identityHash": identity, "receivedContacts": len(CONTACTS),
        "ownProfile": False, "appearances": appearances,
    }, indent=2) + "\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-source", type=Path, required=True)
    parser.add_argument("--schema-directory", type=Path, required=True)
    parser.add_argument("--appearance-source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    generate(args.database_source, args.schema_directory, args.appearance_source, args.output)


if __name__ == "__main__":
    main()
