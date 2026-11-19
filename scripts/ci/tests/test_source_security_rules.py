"""Exercise reviewed local rules against unsafe and safe native Kotlin examples."""

import contextlib
import importlib.util
import io
import json
from pathlib import Path
import shutil
import tempfile
import unittest


SPEC = importlib.util.spec_from_file_location("source_security_rules", Path(__file__).parents[1] / "source_security.py")
SCANNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCANNER)


@unittest.skipUnless(shutil.which("semgrep"), "The pinned Semgrep engine is installed in the source-security job.")
class SourceSecurityRuleTests(unittest.TestCase):
    def scan_fixture(self, code):
        with tempfile.TemporaryDirectory(prefix="txoka-semgrep-rules-") as temporary:
            root = Path(temporary)
            source = root / "app/src/main/java/example/Example.kt"
            source.parent.mkdir(parents=True)
            source.write_text(code)
            rules = root / SCANNER.RULES
            rules.parent.mkdir(parents=True)
            shutil.copyfile(SCANNER.ROOT / SCANNER.RULES, rules)
            output = root / "reports"
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(SCANNER.scan(root, output), 0)
            return json.loads((output / "Semgrep-report.json").read_text())["results"]

    def test_unsafe_native_examples_trigger_every_reviewed_rule(self):
        private_header = "-----BEGIN " + "PRIVATE KEY-----"
        fake_token = "gh" + "p_" + "A" * 36
        findings = self.scan_fixture('''
            import javax.crypto.Cipher
            import java.security.MessageDigest
            import javax.xml.parsers.DocumentBuilderFactory
            import android.net.Uri
            class Example {
                fun unsafe(view: android.webkit.WebView, file: java.io.File) {
                    Cipher.getInstance("AES")
                    MessageDigest.getInstance("MD5")
                    Runtime.getRuntime().exec("sh")
                    view.addJavascriptInterface(this, "native")
                    Uri.fromFile(file)
                    DocumentBuilderFactory.newInstance()
                }
                val privateKey = "''' + private_header + '''"
                val token = "''' + fake_token + '''"
            }
        ''')
        matched = {item["check_id"].split(".")[-1] for item in findings}
        self.assertEqual(matched, {
            "txoka-native-weak-cipher", "txoka-native-weak-digest",
            "txoka-native-unchecked-command-execution", "txoka-native-javascript-bridge",
            "txoka-native-unprotected-file-uri", "txoka-native-xml-parser-defaults",
            "txoka-embedded-private-key", "txoka-embedded-github-token",
        })

    def test_authenticated_cipher_and_sha256_do_not_trigger_weak_crypto_rules(self):
        findings = self.scan_fixture('''
            import javax.crypto.Cipher
            import java.security.MessageDigest
            class Example {
                fun safe() {
                    Cipher.getInstance("AES/GCM/NoPadding")
                    MessageDigest.getInstance("SHA-256")
                }
            }
        ''')
        self.assertEqual(findings, [])


if __name__ == "__main__":
    unittest.main()
