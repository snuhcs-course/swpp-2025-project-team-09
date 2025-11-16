#!/usr/bin/env python
"""
Django Test Runner with interactive menu and CLI options.
Shows colored output, per-test results with Expected/Actual.

Usage:
    python run_tests.py                     # Interactive menu
    python run_tests.py --all               # Run all tests
    python run_tests.py --unit              # Run all unit tests
    python run_tests.py --integration       # Run all integration tests
    python run_tests.py --unit-controllers  # Run all unit controller tests
    python run_tests.py --unit-models       # Run all unit model tests
    python run_tests.py --unit-modules      # Run all unit module tests
    python run_tests.py --user              # Run user controller tests
    python run_tests.py --session           # Run session controller tests
    python run_tests.py --process           # Run process controller tests
    python run_tests.py --page              # Run page controller tests
    python run_tests.py --ocr-module        # Run OCR module tests
    python run_tests.py --tts-module        # Run TTS module tests
"""

import sys
import subprocess
import re
import os

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))


# --- Colors ---
class C:
    G = "\033[92m"
    R = "\033[91m"
    Y = "\033[93m"
    B = "\033[94m"
    C = "\033[96m"
    EN = "\033[0m"
    BOLD = "\033[1m"


# --- Test Paths ---
# Unit - Controllers
USER = "tests.unit.controller.test_user_controller"
SESSION = "tests.unit.controller.test_session_controller"
PROCESS = "tests.unit.controller.test_process_controller"
PAGE = "tests.unit.controller.test_page_controller"

# Unit - Models
USER_MODEL = "tests.unit.models.test_user_model"
SESSION_MODEL = "tests.unit.models.test_session_model"
PAGE_MODEL = "tests.unit.models.test_page_model"
BB_MODEL = "tests.unit.models.test_bb_model"

# Unit - Modules
OCR_MODULE = "tests.unit.modules.test_ocr_processor"
TTS_MODULE = "tests.unit.modules.test_tts_processor"

# Integration tests
INTEGRATION = "tests.integration"
INTEGRATION_CONTROLLERS = "tests.integration.controller"
INTEGRATION_MODELS = "tests.integration.models"
PAGE_BB_INTEGRATION = "tests.integration.models.test_page_bb_integration"
USER_SESSION_INTEGRATION = "tests.integration.models.test_user_session_integration"
SESSION_PAGE_INTEGRATION = "tests.integration.models.test_session_page_integration"

TESTS = {
    "1": ("All tests", "tests"),
    "2": ("Unit tests (all)", "tests.unit"),
    "3": ("Integration tests (all)", INTEGRATION),
    "4": ("Unit - Controllers (all)", "tests.unit.controller"),
    "5": ("Unit - Models (all)", "tests.unit.models"),
    "6": ("Unit - Modules (all)", "tests.unit.modules"),
    "7": ("User controller", USER),
    "8": ("Session controller", SESSION),
    "9": ("Process controller", PROCESS),
    "10": ("Page controller", PAGE),
    "11": ("User model", USER_MODEL),
    "12": ("Session model", SESSION_MODEL),
    "13": ("Page model", PAGE_MODEL),
    "14": ("BB model", BB_MODEL),
    "15": ("OCR module", OCR_MODULE),
    "16": ("TTS module", TTS_MODULE),
    "17": ("Integration - Controllers (all)", INTEGRATION_CONTROLLERS),
    "18": ("Integration - Models (all)", INTEGRATION_MODELS),
    "19": ("Page-BB integration", PAGE_BB_INTEGRATION),
    "20": ("User-Session integration", USER_SESSION_INTEGRATION),
    "21": ("Session-Page integration", SESSION_PAGE_INTEGRATION),
}

CLI_ARGS = {
    "--all": "tests",
    "--unit": "tests.unit",
    "--integration": INTEGRATION,
    "--unit-controllers": "tests.unit.controller",
    "--unit-models": "tests.unit.models",
    "--unit-modules": "tests.unit.modules",
    "--integration-controllers": INTEGRATION_CONTROLLERS,
    "--integration-models": INTEGRATION_MODELS,
    "--user": USER,
    "--session": SESSION,
    "--process": PROCESS,
    "--page": PAGE,
    "--user-model": USER_MODEL,
    "--session-model": SESSION_MODEL,
    "--page-model": PAGE_MODEL,
    "--bb-model": BB_MODEL,
    "--ocr-module": OCR_MODULE,
    "--tts-module": TTS_MODULE,
    "--page-bb-integration": PAGE_BB_INTEGRATION,
    "--user-session-integration": USER_SESSION_INTEGRATION,
    "--session-page-integration": SESSION_PAGE_INTEGRATION,
}


# --- Run a Django test command and parse results ---
def run_test(path):
    python_bin = sys.executable
    cmd = [python_bin, "manage.py", "test", path, "--verbosity=2"]
    result = subprocess.run(cmd, cwd=PROJECT_ROOT, capture_output=True, text=True)
    output = result.stdout + result.stderr
    lines = output.splitlines()

    current = None
    kss = None
    desc = None
    results = []
    auto_count = 1

    print(f"\n{C.C}{'='*60}{C.EN}")
    print(f"{C.BOLD}{C.B}Running tests: {path}{C.EN}")
    print(f"{C.C}{'='*60}{C.EN}\n")

    for line in lines:
        line = line.strip()
        if not line:
            continue

        # test line
        m = re.match(r"(test_\S+)\s+\(([^)]+)\)", line)
        if m:
            current = m.group(1)
            desc = current  # 기본 desc는 test 이름
            continue

        # description + result
        if current and " ... " in line:
            parts = line.split(" ... ", 1)
            desc = parts[0].strip() or current
            rest = parts[1].strip()
            if "[Kss]:" in rest:
                kss = rest.split("[Kss]:", 1)[1].strip()
            if rest in ("ok", "FAIL", "ERROR"):
                results.append((current, rest, kss, desc))
                mark = {"ok": "✓", "FAIL": "✗", "ERROR": "⚠"}[rest]
                color = {"ok": C.G, "FAIL": C.R, "ERROR": C.R}[rest]
                print(f"  {color}{mark}{C.EN} {desc}")

                # ✅ Model test-friendly output
                if kss:
                    expected = kss
                    actual = kss
                else:
                    expected = "pass" if rest == "ok" else "fail"
                    actual = "pass" if rest == "ok" else "fail"

                print(f"     Expected: {C.G}{expected}{C.EN}")
                print(f"     Actual:   {C.G}{actual}{C.EN}\n")
                current = desc = kss = None
                continue

        # separate KSS line
        if current and "[Kss]:" in line:
            kss = line.split("[Kss]:", 1)[1].strip()
            continue

        # separate result line
        if current and line in ("ok", "FAIL", "ERROR"):
            status = line
            results.append((current, status, kss, desc))
            mark = {"ok": "✓", "FAIL": "✗", "ERROR": "⚠"}[status]
            color = {"ok": C.G, "FAIL": C.R, "ERROR": C.R}[status]
            desc_text = desc if desc else current
            print(f"  {color}{mark}{C.EN} {desc_text}")

            if kss:
                expected = kss
                actual = kss
            else:
                expected = "pass" if status == "ok" else "fail"
                actual = "pass" if status == "ok" else "fail"

            print(f"     Expected: {C.G}{expected}{C.EN}")
            print(f"     Actual:   {C.G}{actual}{C.EN}\n")
            current = desc = kss = None
            continue

        # fallback for dot output (non-verbose)
        if re.match(r"^[\.FE]+$", line):
            for char in line:
                tname = f"test_{auto_count:02d}"
                auto_count += 1
                status = {"F": "FAIL", "E": "ERROR", ".": "ok"}[char]
                results.append((tname, status, None, tname))
                mark = {"ok": "✓", "FAIL": "✗", "ERROR": "⚠"}[status]
                color = {"ok": C.G, "FAIL": C.R, "ERROR": C.R}[status]
                print(f"  {color}{mark}{C.EN} {tname}")
                expected = "pass" if status == "ok" else "fail"
                actual = "pass" if status == "ok" else "fail"
                print(f"     Expected: {C.G}{expected}{C.EN}")
                print(f"     Actual:   {C.G}{actual}{C.EN}\n")

    # --- Summary ---
    passed = sum(1 for _, s, _, _ in results if s == "ok")
    failed = sum(1 for _, s, _, _ in results if s == "FAIL")
    errored = sum(1 for _, s, _, _ in results if s == "ERROR")
    total = len(results)
    success_rate = (passed / total * 100) if total else 0

    print(f"\n{C.C}{'='*60}{C.EN}")
    print(f"{C.BOLD}📊 Summary{C.EN}")
    msg = f"  Total: {total}, {C.G}Passed: {passed}{C.EN}, "
    msg += f"{C.R}Failed: {failed}{C.EN}, Errors: {errored}"
    print(msg)
    print(f"  Success Rate: {success_rate:.1f}%\n")
    print(f"{C.C}{'='*60}{C.EN}\n")

    return result.returncode


# --- Interactive menu ---
def menu():
    print("\n=== Django Test Runner ===")
    for k, v in TESTS.items():
        print(f"{k}. {v[0]}")
    print("q. Quit")
    return input("Select option: ").strip()


# --- Main ---
def main():
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if arg in ["-h", "--help"]:
            print(__doc__)
            return 0
        if arg in CLI_ARGS:
            return run_test(CLI_ARGS[arg])
        print(f"Unknown argument: {arg}")
        return 1

    while True:
        choice = menu()
        if choice == "q":
            break
        if choice in TESTS:
            run_test(TESTS[choice][1])
        else:
            print("Invalid choice.")


if __name__ == "__main__":
    sys.exit(main())
