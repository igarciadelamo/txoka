"""Determine whether this PR event changed a preview build input."""
import os
from pathlib import Path
import re
import subprocess


def relevant_path(path):
    if path.startswith((b"docs/", b"store/")):
        return False
    # Markdown in Android source sets can ship inside the APK.
    return path.startswith(b"app/src/") or not path.lower().endswith(b".md")


def relevant_changes(action, before, base):
    start = before if action == "synchronize" else base
    if not re.fullmatch(r"[0-9a-f]{40}", start):
        return True
    if action != "synchronize":
        ancestor = subprocess.run(["git", "merge-base", start, "HEAD"], capture_output=True, text=True)
        if ancestor.returncode != 0:
            return True
        start = ancestor.stdout.strip()
    # A force push may leave the previous head unavailable in this checkout.
    diff = subprocess.run(["git", "diff", "--name-only", "--no-renames", "-z", start, "HEAD", "--"],
                          capture_output=True)
    return diff.returncode != 0 or any(relevant_path(path) for path in diff.stdout.split(b"\0") if path)


if __name__ == "__main__":
    relevant = relevant_changes(os.environ["PR_ACTION"], os.environ["BEFORE_SHA"], os.environ["BASE_SHA"])
    with Path(os.environ["GITHUB_OUTPUT"]).open("a") as output:
        print("relevant=" + str(relevant).lower(), file=output)
    if not relevant:
        print("Documentation-only change; preview build and emulator skipped.")
