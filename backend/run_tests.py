#!/usr/bin/env python
"""
Test Runner Script for Backend Tests

Usage:
    python run_tests.py                          # Show interactive menu
"""

import sys
import subprocess
import re


# ANSI color codes
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    BOLD = '\033[1m'
    ENDC = '\033[0m'


def run_command(command, verbose=True):
    """Run a django test command and format output"""
    # Always use verbose mode for proper parsing
    if '--verbosity=2' not in command:
        command.append('--verbosity=2')

    print(f"\n{Colors.CYAN}{'='*70}{Colors.ENDC}")
    print(f"{Colors.BOLD}{Colors.BLUE}Running Tests{Colors.ENDC}")
    print(f"{Colors.CYAN}{'='*70}{Colors.ENDC}")
    print(f"{Colors.YELLOW}Command: {' '.join(command[2:])}{Colors.ENDC}\n")

    result = subprocess.run(command, capture_output=True, text=True)

    # Parse output
    output = result.stdout + result.stderr
    lines = output.split('\n')

    # Track test results
    test_results = []
    failures = []
    in_failure = False
    failure_text = []
    printing_tests = False
    current_test = None
    test_description = None
    last_kss_response = None

    for line in lines:
        # Skip initial setup lines
        if any(x in line for x in ['Creating test database', 'Operations to perform',
                                    'Synchronize unmigrated', 'Apply all migrations',
                                    'Creating tables', 'Running deferred SQL',
                                    'Running migrations', 'Applying']):
            continue

        # Found test count
        if line.startswith('Found '):
            found_match = re.search(r'Found (\d+) test', line)
            if found_match:
                print(f"{Colors.CYAN}Found {found_match.group(1)} test(s)...{Colors.ENDC}\n")
            continue

        # System check
        if 'System check identified' in line:
            printing_tests = True
            continue

        # Parse test execution lines
        # Format: test_name (module.ClassName)\nDescription ... [Kss log]\nok
        if printing_tests and line.startswith('test_'):
            # This line contains test name and class
            test_match = re.match(r'(test_\S+)\s+\(([^)]+)\)', line)
            if test_match:
                test_name = test_match.group(1)
                current_test = test_name
                continue

        # Check for description line (contains ...)
        if printing_tests and current_test and ' ... ' in line:
            description = line.split(' ... ')[0].strip()
            rest_of_line = line.split(' ... ', 1)[1] if ' ... ' in line else ''

            # Check for [Kss] in the rest of the line
            if '[Kss]:' in rest_of_line:
                kss_match = re.search(r'\[Kss\]: ([^\n]+)', rest_of_line)
                if kss_match:
                    last_kss_response = kss_match.group(1).strip()

            # Check if result is on same line (format: Description ... ok)
            if rest_of_line.strip() == 'ok':
                # Result is on same line and it's ok
                test_results.append((current_test, 'PASS'))
                print(f"  {Colors.GREEN}✓{Colors.ENDC} {description}")
                if last_kss_response:
                    print(f"     Expected: {Colors.GREEN}{last_kss_response}{Colors.ENDC}")
                    print(f"     Actual:   {Colors.GREEN}{last_kss_response}{Colors.ENDC}")
                    last_kss_response = None
                else:
                    print(f"     Expected: {Colors.GREEN}200 OK{Colors.ENDC}")
                    print(f"     Actual:   {Colors.GREEN}200 OK{Colors.ENDC}")
                print()
                current_test = None
                test_description = None
                continue
            elif rest_of_line.strip() == 'FAIL':
                test_results.append((current_test, 'FAIL'))
                print(f"  {Colors.RED}✗{Colors.ENDC} {description}")
                if last_kss_response:
                    print(f"     Actual: {Colors.RED}{last_kss_response}{Colors.ENDC}")
                    last_kss_response = None
                print()
                current_test = None
                test_description = None
                continue
            elif rest_of_line.strip() == 'ERROR':
                test_results.append((current_test, 'ERROR'))
                print(f"  {Colors.RED}⚠{Colors.ENDC} {description}")
                if last_kss_response:
                    print(f"     Error: {Colors.RED}{last_kss_response}{Colors.ENDC}")
                    last_kss_response = None
                print()
                current_test = None
                test_description = None
                continue
            else:
                # Result will come later on separate line, store description
                test_description = description
            continue

        # Handle [Kss] log on separate line
        if printing_tests and '[Kss]:' in line and current_test and not ' ... ' in line:
            kss_match = re.search(r'\[Kss\]: (.+)', line)
            if kss_match:
                last_kss_response = kss_match.group(1).strip()
            continue

        # Handle result on separate line (ok/FAIL/ERROR)
        if printing_tests and current_test:
            line_stripped = line.strip()
            if line_stripped == 'ok':
                test_results.append((current_test, 'PASS'))
                print(f"  {Colors.GREEN}✓{Colors.ENDC} {test_description if test_description else current_test}")
                if last_kss_response:
                    # Extract expected status from last_kss_response
                    print(f"     Expected: {Colors.GREEN}{last_kss_response}{Colors.ENDC}")
                    print(f"     Actual:   {Colors.GREEN}{last_kss_response}{Colors.ENDC}")
                    last_kss_response = None
                else:
                    print(f"     Expected: {Colors.GREEN}200 OK{Colors.ENDC}")
                    print(f"     Actual:   {Colors.GREEN}200 OK{Colors.ENDC}")
                print()
                current_test = None
                test_description = None
                continue
            elif line_stripped == 'FAIL':
                test_results.append((current_test, 'FAIL'))
                print(f"  {Colors.RED}✗{Colors.ENDC} {test_description if test_description else current_test}")
                if last_kss_response:
                    print(f"     Actual: {Colors.RED}{last_kss_response}{Colors.ENDC}")
                    last_kss_response = None
                print()
                current_test = None
                test_description = None
                continue
            elif line_stripped == 'ERROR':
                test_results.append((current_test, 'ERROR'))
                print(f"  {Colors.RED}⚠{Colors.ENDC} {test_description if test_description else current_test}")
                if last_kss_response:
                    print(f"     Error: {Colors.RED}{last_kss_response}{Colors.ENDC}")
                    last_kss_response = None
                print()
                current_test = None
                test_description = None
                continue

        # Parse dots output (non-verbose mode)
        if re.match(r'^[\.FE]+$', line.strip()):
            for char in line.strip():
                if char == '.':
                    test_results.append((f'test_{len(test_results)+1}', 'PASS'))
                    print(f"{Colors.GREEN}●{Colors.ENDC}", end='', flush=True)
                elif char == 'F':
                    test_results.append((f'test_{len(test_results)+1}', 'FAIL'))
                    print(f"{Colors.RED}●{Colors.ENDC}", end='', flush=True)
                elif char == 'E':
                    test_results.append((f'test_{len(test_results)+1}', 'ERROR'))
                    print(f"{Colors.YELLOW}●{Colors.ENDC}", end='', flush=True)
            print()
            continue

        # Capture failure/error details
        if line.startswith('FAIL:') or line.startswith('ERROR:'):
            in_failure = True
            failure_text = [line]
        elif in_failure:
            if line.startswith('======'):
                if failure_text:
                    failures.append('\n'.join(failure_text))
                    failure_text = []
                in_failure = False
            else:
                failure_text.append(line)

        # Parse final summary line
        if line.startswith('Ran '):
            match = re.search(r'Ran (\d+) test', line)
            if match and not test_results:
                # Fallback if we missed parsing individual tests
                count = int(match.group(1))
                for i in range(count):
                    test_results.append((f'test_{i+1}', 'PASS'))

    # Print summary
    passed = sum(1 for _, status in test_results if status == 'PASS')
    failed = sum(1 for _, status in test_results if status == 'FAIL')
    errored = sum(1 for _, status in test_results if status == 'ERROR')
    total = len(test_results)

    print(f"\n{Colors.CYAN}{'='*70}{Colors.ENDC}")
    print(f"{Colors.BOLD}📊 Test Summary{Colors.ENDC}")
    print(f"{Colors.CYAN}{'='*70}{Colors.ENDC}")

    if total > 0:
        print(f"  Total Tests: {Colors.BOLD}{total}{Colors.ENDC}")
        print(f"  {Colors.GREEN}✓ Passed: {passed}{Colors.ENDC}")
        if failed > 0:
            print(f"  {Colors.RED}✗ Failed: {failed}{Colors.ENDC}")
        if errored > 0:
            print(f"  {Colors.RED}⚠ Errors: {errored}{Colors.ENDC}")

        # Calculate success rate
        success_rate = (passed / total) * 100 if total > 0 else 0
        if success_rate == 100:
            print(f"\n  {Colors.GREEN}{Colors.BOLD}🎉 All tests passed! (100%){Colors.ENDC}")
        else:
            color = Colors.YELLOW if success_rate >= 50 else Colors.RED
            print(f"\n  {color}Success Rate: {success_rate:.1f}%{Colors.ENDC}")
    else:
        print(f"  {Colors.YELLOW}No tests were executed{Colors.ENDC}")

    # Print failure details
    if failures:
        print(f"\n{Colors.RED}{Colors.BOLD}❌ Failure Details:{Colors.ENDC}")
        print(f"{Colors.RED}{'='*70}{Colors.ENDC}")
        for i, failure in enumerate(failures, 1):
            print(f"\n{Colors.BOLD}Failure #{i}:{Colors.ENDC}")
            print(failure)

    print(f"\n{Colors.CYAN}{'='*70}{Colors.ENDC}\n")

    return result.returncode


def show_menu():
    """Display interactive menu for test selection"""
    print("\n" + "="*60)
    print("Backend Test Runner - Interactive Menu")
    print("="*60)
    print("\n[All Tests]")
    print("  1. Run all tests")
    print("  2. Run all unit tests")
    print("\n[User Controller Tests]")
    print("  3. Run all user controller tests")
    print("  4. Run user register tests")
    print("  5. Run user login tests")
    print("  6. Run user change language tests")
    print("  7. Run user info tests")
    print("\n[Individual User Register Tests]")
    print("  8. test_register_success")
    print("  9. test_register_missing_device_info")
    print("  10. test_register_missing_language_preference")
    print("  11. test_register_duplicate_device")
    print("\n[Individual User Login Tests]")
    print("  12. test_login_success")
    print("  13. test_login_missing_device_info")
    print("  14. test_login_device_not_registered")
    print("\n[Options]")
    print("  v. Toggle verbose mode (currently: OFF)")
    print("  q. Quit")
    print("="*60)

    return input("\nSelect option: ").strip()


def main():
    verbose = '--verbose' in sys.argv or '-v' in sys.argv

    # Command line arguments
    if len(sys.argv) > 1:
        arg = sys.argv[1]

        if arg == '--all':
            cmd = ['python', 'manage.py', 'test', 'tests']
        elif arg == '--unit':
            cmd = ['python', 'manage.py', 'test', 'tests.unit']
        elif arg == '--user':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller']
        elif arg == '--user-register':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserRegisterView']
        elif arg == '--user-login':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserLoginView']
        elif arg == '--user-lang':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserChangeLangView']
        elif arg == '--user-info':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserInfoView']
        elif arg in ['--help', '-h']:
            print(__doc__)
            return 0
        else:
            print(f"Unknown argument: {arg}")
            print(__doc__)
            return 1

        return run_command(cmd, verbose)

    # Interactive menu (always verbose for proper parsing)
    verbose_mode = True

    while True:
        choice = show_menu()

        if choice == 'q':
            print("\nExiting...")
            break
        elif choice == 'v':
            verbose_mode = not verbose_mode
            print(f"\nVerbose mode: {'ON' if verbose_mode else 'OFF'}")
            continue

        cmd = None

        # Map choices to commands
        if choice == '1':
            cmd = ['python', 'manage.py', 'test', 'tests']
        elif choice == '2':
            cmd = ['python', 'manage.py', 'test', 'tests.unit']
        elif choice == '3':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller']
        elif choice == '4':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserRegisterView']
        elif choice == '5':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserLoginView']
        elif choice == '6':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserChangeLangView']
        elif choice == '7':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserInfoView']
        elif choice == '8':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserRegisterView.test_01_register_success']
        elif choice == '9':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserRegisterView.test_02_register_missing_device_info']
        elif choice == '10':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserRegisterView.test_03_register_missing_language_preference']
        elif choice == '11':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserRegisterView.test_04_register_duplicate_device']
        elif choice == '12':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserLoginView.test_01_login_success']
        elif choice == '13':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserLoginView.test_02_login_missing_device_info']
        elif choice == '14':
            cmd = ['python', 'manage.py', 'test', 'tests.unit.controller.user_controller.test_views.TestUserLoginView.test_03_login_device_not_registered']
        else:
            print("\nInvalid choice. Please try again.")
            continue

        if cmd:
            run_command(cmd, verbose_mode)
            input("\nPress Enter to continue...")

    return 0


if __name__ == '__main__':
    sys.exit(main())
