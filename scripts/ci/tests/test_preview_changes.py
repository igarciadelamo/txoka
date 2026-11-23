import importlib.util
from pathlib import Path
from subprocess import CompletedProcess
import unittest
from unittest.mock import patch


spec = importlib.util.spec_from_file_location("preview_changes", Path(__file__).resolve().parents[1] / "preview_changes.py")
changes = importlib.util.module_from_spec(spec)
spec.loader.exec_module(changes)


class PreviewChangesTest(unittest.TestCase):
    before = "1" * 40
    base = "2" * 40
    ancestor = "3" * 40

    @patch.object(changes.subprocess, "run")
    def test_incremental_documentation_in_mixed_pr_does_not_rebuild(self, run):
        run.return_value = CompletedProcess([], 0, stdout=b"README.md\0docs/demo.png\0store/a/b.jpg\0")
        self.assertFalse(changes.relevant_changes("synchronize", self.before, self.base))
        run.assert_called_once()
        command = run.call_args.args[0]
        self.assertIn(self.before, command)
        self.assertNotIn(self.base, command)
        self.assertIn("--no-renames", command)  # A source moved into docs must include the deleted path.

    @patch.object(changes.subprocess, "run")
    def test_label_compares_the_whole_pr_including_changed_source(self, run):
        run.side_effect = [CompletedProcess([], 0, stdout=self.ancestor + "\n"),
                           CompletedProcess([], 0, stdout=b"app/build.gradle.kts\0README.md\0")]
        self.assertTrue(changes.relevant_changes("labeled", self.before, self.base))
        self.assertEqual(run.call_args_list[0].args[0], ["git", "merge-base", self.base, "HEAD"])
        self.assertIn(self.ancestor, run.call_args_list[1].args[0])

    @patch.object(changes.subprocess, "run")
    def test_source_after_more_than_three_hundred_docs_is_seen(self, run):
        docs = [f"docs/{i}.md".encode() for i in range(350)]
        run.return_value = CompletedProcess([], 0, stdout=b"\0".join([*docs, b"app/src/main/assets/license.md", b""]))
        self.assertTrue(changes.relevant_changes("synchronize", self.before, self.base))

    @patch.object(changes.subprocess, "run")
    def test_unavailable_history_is_not_treated_as_docs_only(self, run):
        self.assertTrue(changes.relevant_changes("synchronize", "", self.base))
        run.assert_not_called()
        run.return_value = CompletedProcess([], 128, stdout=b"")
        self.assertTrue(changes.relevant_changes("synchronize", self.before, self.base))
        self.assertTrue(changes.relevant_changes("reopened", self.before, self.base))
