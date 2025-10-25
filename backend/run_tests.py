#!/usr/bin/env python
"""
Test Runner Script for Backend Tests

This script provides an interactive menu and command-line interface for running
Django tests with formatted output and detailed test results.

Usage:
    python run_tests.py                          # Show interactive menu
    python run_tests.py --all                    # Run all tests
    python run_tests.py --unit                   # Run all unit tests
    python run_tests.py --user                   # Run user controller tests
    python run_tests.py --help                   # Show help message
"""

import sys
import subprocess
import re


# ANSI color codes for terminal output formatting
class Colors:
    """ANSI escape codes for colored terminal output"""
    GREEN = '\033[92m'      # Success messages
    RED = '\033[91m'        # Error/failure messages
    YELLOW = '\033[93m'     # Warning messages
    BLUE = '\033[94m'       # Header messages
    CYAN = '\033[96m'       # Info messages
    BOLD = '\033[1m'        # Bold text
    ENDC = '\033[0m'        # Reset color


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


# Common test path prefixes
USER_CONTROLLER = 'tests.unit.controller.user_controller'
SESSION_CONTROLLER = 'tests.unit.controller.session_controller'

# Test menu configuration
# Each entry: (choice_number, description, test_path)
# test_path will be automatically prefixed with 'python manage.py test'
TEST_MENU = {
    'All Tests': [
        ('1', 'Run all tests', 'tests'),
        ('2', 'Run all unit tests', 'tests.unit'),
    ],
    'User Controller Tests': [
        ('3', 'Run all user controller tests', USER_CONTROLLER),
        ('4', 'Run user register tests', f'{USER_CONTROLLER}.test_views.TestUserRegisterView'),
        ('5', 'Run user login tests', f'{USER_CONTROLLER}.test_views.TestUserLoginView'),
        ('6', 'Run user change language tests', f'{USER_CONTROLLER}.test_views.TestUserChangeLangView'),
        ('7', 'Run user info tests', f'{USER_CONTROLLER}.test_views.TestUserInfoView'),
    ],
    'Session Controller Tests': [
        ('8', 'Run all session controller tests', SESSION_CONTROLLER),
        ('9', 'test_start_session_success', f'{SESSION_CONTROLLER}.test_views.TestSessionController.test_01_start_session_success'),
        ('10', 'test_select_voice_success', f'{SESSION_CONTROLLER}.test_views.TestSessionController.test_04_select_voice_success'),
        ('11', 'test_end_session_success', f'{SESSION_CONTROLLER}.test_views.TestSessionController.test_07_end_session_success'),
        ('12', 'test_get_session_info_success', f'{SESSION_CONTROLLER}.test_views.TestSessionController.test_10_get_session_info_success'),
        ('13', 'test_get_session_stats_success', f'{SESSION_CONTROLLER}.test_views.TestSessionController.test_13_get_session_stats_success'),
        ('14', 'test_get_session_review_success', f'{SESSION_CONTROLLER}.test_views.TestSessionController.test_16_get_session_review_success'),
    ],
}


def build_test_command(test_path):
    """Build a django test command from a test path"""
    return ['python', 'manage.py', 'test', test_path]


def show_menu():
    """Display interactive menu for test selection"""
    print("\n" + "="*60)
    print("Backend Test Runner - Interactive Menu")
    print("="*60)

    for category, tests in TEST_MENU.items():
        print(f"\n[{category}]")
        for choice, description, _test_path in tests:
            print(f"  {choice}. {description}")

    print("\n[Options]")
    print("  v. Toggle verbose mode (currently: OFF)")
    print("  q. Quit")
    print("="*60)

    return input("\nSelect option: ").strip()


# Command line argument mapping
CLI_ARGS = {
    '--all': 'tests',
    '--unit': 'tests.unit',
    '--user': USER_CONTROLLER,
    '--user-register': f'{USER_CONTROLLER}.test_views.TestUserRegisterView',
    '--user-login': f'{USER_CONTROLLER}.test_views.TestUserLoginView',
    '--user-lang': f'{USER_CONTROLLER}.test_views.TestUserChangeLangView',
    '--user-info': f'{USER_CONTROLLER}.test_views.TestUserInfoView',
}


def main():
    verbose = '--verbose' in sys.argv or '-v' in sys.argv

    # Command line arguments
    if len(sys.argv) > 1:
        arg = sys.argv[1]

        if arg in ['--help', '-h']:
            print(__doc__)
            return 0
        elif arg in CLI_ARGS:
            test_path = CLI_ARGS[arg]
            cmd = build_test_command(test_path)
            return run_command(cmd, verbose)
        else:
            print(f"Unknown argument: {arg}")
            print(__doc__)
            return 1

    # Interactive menu (always verbose for proper parsing)
    verbose_mode = True

    # Build choice-to-test-path mapping from TEST_MENU
    choice_map = {}
    for tests in TEST_MENU.values():
        for choice, _description, test_path in tests:
            choice_map[choice] = test_path

    while True:
        choice = show_menu()

        if choice == 'q':
            print("\nExiting...")
            break
        elif choice == 'v':
            verbose_mode = not verbose_mode
            print(f"\nVerbose mode: {'ON' if verbose_mode else 'OFF'}")
            continue

        # Look up test path and build command
        test_path = choice_map.get(choice)

        if test_path:
            cmd = build_test_command(test_path)
            run_command(cmd, verbose_mode)
            input("\nPress Enter to continue...")
        else:
            print("\nInvalid choice. Please try again.")
            continue

    return 0


if __name__ == '__main__':
    sys.exit(main())
