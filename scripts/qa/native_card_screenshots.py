"""Run the ephemeral native screenshot test and retain diagnostics on failure."""

import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import tarfile
import threading
import time


PACKAGE = "es.igarciadelamo.txoka"
TEST_CLASS = PACKAGE + ".review.NativeReviewScreenshotsTest"
RENDER_TEST_CLASS = PACKAGE + ".ui.components.artwork.CardMosaicRenderingTest"
MATERIAL_TEST_CLASS = PACKAGE + ".ui.components.CardMaterialRenderingTest"
EXPECTED_TESTS = 17
ROOM_TEST_CLASSES = [PACKAGE + ".data.received.ReceivedContactDaoTest",
                     PACKAGE + ".data.received.TxokaMigrationTest"]
OUTPUT = Path("screenshot-review")
MAX_ARCHIVE = 60 * 1024 * 1024
MAIN_SCENES = {
    "01-hub-hologram.png",
    "02-editor-material-options.png",
    "03-editor-stained-glass.png",
    "04-editor-nacre.png",
    "05-editor-brushed-metal.png",
    "06-hub-material-front.png",
    "07-hub-qr-back.png",
    "08-viewer-portrait-front.png",
    "09-viewer-portrait-qr.png",
    "10-viewer-landscape-qr.png",
    "11-viewer-landscape-front.png",
}


def run(*arguments, data=None, timeout=30, check=True):
    result = subprocess.run(arguments, input=data, capture_output=True, timeout=timeout, check=False)
    if check and result.returncode:
        raise RuntimeError(f"Command failed: {arguments[:3]}: {result.stderr.decode(errors='replace')}")
    return result


def adb(*arguments, **kwargs):
    return run("adb", *arguments, **kwargs)


def varint(value):
    result = bytearray()
    while value > 127:
        result.append((value & 127) | 128)
        value >>= 7
    result.append(value)
    return bytes(result)


def field(number, value):
    value = value.encode() if isinstance(value, str) else value
    return varint(number << 3 | 2) + varint(len(value)) + value


def seed_before_instrumentation():
    values = {
        "profile.name": "Marta Ruiz", "profile.phone": "+34600111222",
        "profile.email": "marta@example.com", "profile.linkedin": "", "profile.instagram": "",
        "profile.card.accent": "#6554C0", "profile.card.accent2": "#36C6A8",
        "profile.card.appearance": "1:cWgBoA",
    }
    # The Application starts its DataStore observer before test methods run.
    # Seed while the process is stopped, using the verified PreferencesProto wire fields.
    encoded = b"".join(field(1, field(1, key) + field(2, field(5, value)))
                       for key, value in values.items())
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "run-as", PACKAGE, "mkdir", "-p", "files/datastore")
    adb("shell", "-T", "run-as", PACKAGE, "sh", "-c",
        "'cat > files/datastore/profile.preferences_pb'", data=encoded)


def host_snapshot(label):
    sections = []
    for command in [
        ["free", "-m"], ["ps", "-eo", "pid,rss,comm", "--sort=-rss"],
        ["adb", "devices", "-l"],
    ]:
        try:
            result = run(*command, timeout=10, check=False)
            text = (result.stdout + result.stderr).decode(errors="replace")
            sections.append("$ " + " ".join(command) + "\n" + "\n".join(text.splitlines()[:35]))
        except (OSError, subprocess.TimeoutExpired) as error:
            sections.append(type(error).__name__)
    (OUTPUT / (label + ".txt")).write_text("\n\n".join(sections) + "\n")


def watch_memory(stop):
    started = time.monotonic()
    with (OUTPUT / "host-memory-samples.log").open("w") as output:
        while not stop.is_set():
            try:
                memory = Path("/proc/meminfo").read_text()
                selected = "\n".join(line for line in memory.splitlines()
                                     if line.startswith(("MemTotal:", "MemAvailable:", "SwapFree:")))
                processes = run("ps", "-eo", "pid,rss,comm", "--sort=-rss", timeout=5, check=False)
                output.write(f"elapsed={time.monotonic()-started:.1f}s\n{selected}\n")
                output.write("\n".join(processes.stdout.decode(errors="replace").splitlines()[:12]) + "\n\n")
                output.flush()
            except (OSError, subprocess.TimeoutExpired):
                pass
            stop.wait(10)


def pull_results(verbose=True, timeout=45):
    result = adb("exec-out", "run-as", PACKAGE, "tar", "-cf", "-", "-C",
                 "files/review-screenshots", ".", timeout=timeout, check=False)
    if result.returncode:
        raise RuntimeError("Could not retrieve native test files: " + result.stderr.decode(errors="replace"))
    if len(result.stdout) > MAX_ARCHIVE:
        raise RuntimeError("Native capture archive exceeds the expected size")
    seen = set()
    expanded = 0
    with tarfile.open(fileobj=io.BytesIO(result.stdout), mode="r:") as archive:
        for entry in archive:
            if entry.isdir() and entry.name in {".", "./"}:
                continue
            name = PurePosixPath(entry.name)
            if (not entry.isfile() or name.is_absolute() or ".." in name.parts
                    or len(name.parts) != 1 or name.name in seen
                    or not re.fullmatch(r"[A-Za-z0-9._-]+\.(?:png|json|xml|txt)", name.name)
                    or entry.size > 10 * 1024 * 1024):
                raise RuntimeError("Unexpected native capture archive entry")
            seen.add(name.name)
            expanded += entry.size
            if expanded > MAX_ARCHIVE or len(seen) > 100:
                raise RuntimeError("Native capture archive exceeds extraction limits")
            data = archive.extractfile(entry).read()
            if len(data) != entry.size:
                raise RuntimeError("Native capture entry is truncated")
            (OUTPUT / name.name).write_bytes(data)
    if verbose:
        print("Retrieved native test files: " + ", ".join(sorted(seen)), flush=True)


def retain_in_progress(stop):
    while not stop.wait(4):
        try:
            pull_results(verbose=False, timeout=10)
        except (OSError, RuntimeError, subprocess.TimeoutExpired, tarfile.TarError):
            pass


def retain_emulator_crash_metadata():
    # Whitelist report fields: never export core dumps, process environment or credentials.
    script = r'''
from pathlib import Path
allowed = {"ProblemType", "Date", "ExecutablePath", "Signal", "SignalName",
           "StacktraceTop", "Stacktrace", "ThreadStacktrace", "AssertionMessage"}
budget = 24000
for report in sorted(Path("/var/crash").glob("*qemu*.crash"))[:4]:
    print("Report:", report.name)
    selected = False
    with report.open(errors="replace") as source:
        for line in source:
            if not line.startswith(" "):
                selected = line.split(":", 1)[0] in allowed
            if selected and budget > 0:
                text = line[:min(budget, 2000)]
                print(text, end="")
                budget -= len(text)
print("Emulator crash database files (metadata only):")
for directory in Path("/tmp").glob("android-*"):
    for entry in sorted(directory.glob("emu-crash*/**/*"))[:40]:
        if entry.is_file():
            print(str(entry), entry.stat().st_size)
'''
    try:
        result = run("sudo", "-n", "python3", "-c", script, timeout=15, check=False)
        (OUTPUT / "emulator-crash-metadata.txt").write_bytes(result.stdout + result.stderr)
    except (OSError, subprocess.TimeoutExpired) as error:
        (OUTPUT / "emulator-crash-metadata.txt").write_text(type(error).__name__ + "\n")


def validate_results():
    manifest = json.loads((OUTPUT / "capture-manifest.json").read_text())
    if manifest.get("failure"):
        raise RuntimeError("Native capture test failed: " + manifest["failure"])
    scenes = {scene["file"]: scene for scene in manifest.get("scenes", [])}
    if not MAIN_SCENES.issubset(scenes):
        raise RuntimeError("The native test did not capture all required scenes")
    for name in MAIN_SCENES:
        data = (OUTPUT / name).read_bytes()
        if not data.startswith(b"\x89PNG\r\n\x1a\n") or hashlib.sha256(data).hexdigest() != scenes[name]["sha256"]:
            raise RuntimeError("Invalid native screenshot: " + name)
    print(f"Validated {len(MAIN_SCENES)} native UI captures.", flush=True)
    def bounded(value):
        if isinstance(value, str):
            return value[:1200] + (" [truncated; complete value in artifact]" if len(value) > 1200 else "")
        if isinstance(value, dict):
            return {key: bounded(item) for key, item in value.items()}
        if isinstance(value, list):
            return [bounded(item) for item in value]
        return value
    print("Native diagnostics: " + json.dumps(bounded(manifest.get("probes", {}))), flush=True)


def main():
    OUTPUT.mkdir(exist_ok=True)
    host_snapshot("host-before-capture")
    stop = threading.Event()
    monitor = threading.Thread(target=watch_memory, args=(stop,), daemon=True)
    monitor.start()
    logcat = None
    retention_stop = threading.Event()
    retention = None
    log = (OUTPUT / "device-logcat.txt").open("wb")
    failure = None
    try:
        adb("wait-for-device", timeout=60)
        adb("shell", "settings", "put", "global", "device_provisioned", "1")
        adb("shell", "settings", "put", "secure", "user_setup_complete", "1")
        adb("shell", "pm", "disable-user", "--user", "0", "com.google.android.googlesdksetup", check=False)
        for path in ["app/build/outputs/apk/debug/app-debug.apk",
                     "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
            if not Path(path).is_file():
                raise RuntimeError("Missing native test APK: " + path)
            adb("install", "-r", path, timeout=90)
        seed_before_instrumentation()
        adb("logcat", "-c", check=False)
        logcat = subprocess.Popen(["adb", "logcat", "-v", "threadtime"], stdout=log, stderr=subprocess.STDOUT)
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        adb("shell", "input", "keyevent", "KEYCODE_MENU")
        print("Starting bounded native UiAutomation capture (no waitForIdle).", flush=True)
        retention = threading.Thread(target=retain_in_progress, args=(retention_stop,), daemon=True)
        retention.start()
        command = ["adb", "shell", "am", "instrument", "-w", "-r", "-e", "class",
                   ",".join([TEST_CLASS, RENDER_TEST_CLASS, MATERIAL_TEST_CLASS, *ROOM_TEST_CLASSES]),
                   "-e", "qaSha", os.environ.get("GITHUB_SHA", "unspecified"),
                   "-e", "workflowRun", os.environ.get("GITHUB_RUN_ID", "unspecified"),
                   PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner"]
        with (OUTPUT / "instrumentation.log").open("wb") as report:
            process = subprocess.Popen(command, stdout=report, stderr=subprocess.STDOUT)
            try:
                code = process.wait(timeout=600)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
                raise RuntimeError("Native capture exceeded its 10-minute limit")
        text = (OUTPUT / "instrumentation.log").read_text(errors="replace")
        print(text[-12000:], flush=True)
        if code or "FAILURES!!!" in text or "INSTRUMENTATION_FAILED" in text or f"OK ({EXPECTED_TESTS} tests)" not in text:
            raise RuntimeError("The native screenshot instrumentation did not pass")
    except Exception as error:
        failure = error
    finally:
        retention_stop.set()
        if retention is not None:
            retention.join(timeout=12)
        try:
            pull_results()
        except Exception as error:
            (OUTPUT / "retrieval-error.txt").write_text(str(error) + "\n")
            if failure is None:
                failure = error
        if logcat is not None:
            logcat.terminate()
            try:
                logcat.wait(timeout=10)
            except subprocess.TimeoutExpired:
                logcat.kill()
                logcat.wait(timeout=10)
        log.close()
        stop.set()
        monitor.join(timeout=12)
        host_snapshot("host-after-capture")
        retain_emulator_crash_metadata()
        try:
            result = run("sudo", "-n", "dmesg", "-T", timeout=15, check=False)
            (OUTPUT / "host-dmesg.txt").write_bytes(result.stdout + result.stderr)
        except (OSError, subprocess.TimeoutExpired) as error:
            (OUTPUT / "host-dmesg.txt").write_text(type(error).__name__ + "\n")
    if failure is not None:
        raise failure
    validate_results()


if __name__ == "__main__":
    main()
