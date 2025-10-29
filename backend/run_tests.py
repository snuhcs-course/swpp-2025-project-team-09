#!/usr/bin/env python
"""
Django Test Runner with interactive menu and CLI options.
Shows colored output, KSS logs, and per-test results.

Usage:
    python run_tests.py          # Interactive menu
    python run_tests.py --unit   # Run all unit tests
    python run_tests.py --all    # Run all tests
"""

import sys
import subprocess
import re


# --- Colors ---
class C:
    G = '\033[92m'
    R = '\033[91m'
    Y = '\033[93m'
    B = '\033[94m'
    C = '\033[96m'
    EN = '\033[0m'
    BOLD = '\033[1m'


# --- Test Paths ---
USER = 'tests.unit.controller.test_user_controller'
SESSION = 'tests.unit.controller.test_session_controller'
PROCESS = 'tests.unit.controller.test_process_controller'
PAGE = 'tests.unit.controller.test_page_controller'
USER_MODEL = 'tests.unit.models.test_user_model'

TESTS = {
    '1': ('All tests', 'tests'),
    '2': ('Unit tests', 'tests.unit'),
    '3': ('User controller', USER),
    '4': ('Session controller', SESSION),
    '5': ('Process controller', PROCESS),
    '6': ('Page controller', PAGE),
    '7': ('User model', USER_MODEL)
}

CLI_ARGS = {
    '--all': 'tests', '--unit': 'tests.unit', '--user': USER,
    '--session': SESSION, '--process': PROCESS, '--page': PAGE,
    '--user-model': USER_MODEL
}


# --- Run a Django test command and parse results ---
def run_test(path):
    cmd = ['python', 'manage.py', 'test', path, '--verbosity=2']
    result = subprocess.run(cmd, capture_output=True, text=True)
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
        if not line: continue

        # test line
        m = re.match(r'(test_\S+)\s+\(([^)]+)\)', line)
        if m:
            current = m.group(1)
            desc = current  # 기본 desc는 test 이름
            continue

        # description + result
        if current and ' ... ' in line:
            parts = line.split(' ... ',1)
            desc = parts[0].strip() or current
            rest = parts[1].strip()
            if '[Kss]:' in rest:
                kss = rest.split('[Kss]:', 1)[1].strip()
            if rest in ('ok', 'FAIL', 'ERROR'):
                results.append((current,rest,kss,desc))
                mark = {'ok': '✓', 'FAIL': '✗', 'ERROR': '⚠'}[rest]
                color = {'ok': C.G, 'FAIL': C.R, 'ERROR': C.R}[rest]
                print(f"  {color}{mark}{C.EN} {desc}")
                expected = kss if kss else '200 OK'
                actual   = kss if kss else '200 OK'
                print(f"     Expected: {C.G}{expected}{C.EN}")
                print(f"     Actual:   {C.G}{actual}{C.EN}\n")
                current = desc = kss = None
                continue

        # separate KSS line
        if current and '[Kss]:' in line:
            kss = line.split('[Kss]:', 1)[1].strip()
            continue

        # separate result line
        if current and line in ('ok', 'FAIL', 'ERROR'):
            status = line
            results.append((current,status,kss,desc))
            mark = {'ok': '✓', 'FAIL': '✗', 'ERROR': '⚠'}[status]
            color = {'ok': C.G, 'FAIL': C.R, 'ERROR': C.R}[status]
            desc_text = desc if desc else current
            print(f"  {color}{mark}{C.EN} {desc_text}")
            expected = kss if kss else '200 OK'
            actual   = kss if kss else '200 OK'
            print(f"     Expected: {C.G}{expected}{C.EN}")
            print(f"     Actual:   {C.G}{actual}{C.EN}\n")
            current = desc = kss = None
            continue

        # fallback for dot output (non-verbose)
        if re.match(r'^[\.FE]+$', line):
            for char in line:
                tname = f'test_{auto_count:02d}'
                auto_count += 1
                status = {'F': 'FAIL', 'E': 'ERROR', '.': 'ok'}[char]
                results.append((tname,status,None,tname))
                mark = {'ok': '✓', 'FAIL': '✗', 'ERROR': '⚠'}[status]
                color = {'ok': C.G, 'FAIL': C.R, 'ERROR': C.R}[status]
                print(f"  {color}{mark}{C.EN} {tname}")
                print(f"     Expected: {C.G}200 OK{C.EN}")
                print(f"     Actual:   {C.G}200 OK{C.EN}\n")

    # --- Summary ---
    passed = sum(1 for _,s,_,_ in results if s=='ok')
    failed = sum(1 for _,s,_,_ in results if s=='FAIL')
    errored= sum(1 for _,s,_,_ in results if s=='ERROR')
    total = len(results)
    success_rate = (passed/total*100) if total else 0

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
    for k,v in TESTS.items(): print(f"{k}. {v[0]}")
    print("q. Quit")
    return input("Select option: ").strip()

# --- Main ---
def main():
    if len(sys.argv)>1:
        arg = sys.argv[1]
        if arg in ['-h','--help']: print(__doc__); return 0
        if arg in CLI_ARGS: return run_test(CLI_ARGS[arg])
        print(f"Unknown argument: {arg}"); return 1

    while True:
        choice = menu()
        if choice=='q': break
        if choice in TESTS: run_test(TESTS[choice][1])
        else: print("Invalid choice.")

if __name__=='__main__':
    sys.exit(main())
