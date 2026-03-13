#!/usr/bin/env python3
"""
Comprehensive automated testing framework for PZ Lua API Viewer.
Run: python .gsd/test/run.py
"""

import sys
import json
from pathlib import Path
from datetime import datetime

# Add parent to path for imports
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from config import browser, SERVER_URL, REPORT_DIR, init_browser
from s02_features import test_middle_click_new_tab, test_hover_preview_card
from navigation import test_back_forward_buttons, test_nav_hash_change
from search import test_search_basic, test_search_scope_toggle
from globals import test_globals_panel, test_globals_search
from detail import test_class_detail_panel, test_copy_fqn
from inheritance import test_inheritance_chain
from regression import test_tab_navigation, test_class_link_clicks


def run_all_tests():
    """Run all tests and generate report."""
    print("\n" + "="*60)
    print("PZ Lua API Viewer — Automated Test Suite")
    print("="*60)
    print(f"\nTest run started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Initialize browser
    page = init_browser()
    
    # Run S02 feature tests
    print("\n[1/7] Running S02 Feature Tests...")
    result = test_middle_click_new_tab(page)
    print_result(result)
    result = test_hover_preview_card(page)
    print_result(result)
    
    # Run navigation tests
    print("\n[2/7] Running Navigation Tests...")
    result = test_back_forward_buttons(page)
    print_result(result)
    result = test_nav_hash_change(page)
    print_result(result)
    
    # Run search tests
    print("\n[3/7] Running Search Tests...")
    result = test_search_basic(page)
    print_result(result)
    result = test_search_scope_toggle(page)
    print_result(result)
    
    # Run globals tests
    print("\n[4/7] Running Globals Tests...")
    result = test_globals_panel(page)
    print_result(result)
    result = test_globals_search(page)
    print_result(result)
    
    # Run detail view tests
    print("\n[5/7] Running Detail View Tests...")
    result = test_class_detail_panel(page)
    print_result(result)
    result = test_copy_fqn(page)
    print_result(result)
    
    # Run inheritance tests
    print("\n[6/7] Running Inheritance Tests...")
    result = test_inheritance_chain(page)
    print_result(result)
    
    # Run regression tests
    print("\n[7/7] Running Regression Tests...")
    result = test_tab_navigation(page)
    print_result(result)
    result = test_class_link_clicks(page)
    print_result(result)
    
    # Save report
    _save_report()
    
    # Cleanup
    browser.close()
    
    # Print summary
    passed = sum(1 for r in all_results if r["passed"])
    total = len(all_results)
    print("\n" + "="*60)
    print(f"Test Summary: {passed}/{total} tests passed")
    print("="*60)
    
    return all(r["passed"] for r in all_results)


def print_result(result):
    """Print a test result."""
    status = "PASS" if result["passed"] else "FAIL"
    print(f"  [{status}] {result['name']}: {result.get('message', 'N/A')}")
    all_results.append(result)


all_results = []


def _save_report():
    """Save a JSON report of all test results."""
    report_dir = REPORT_DIR
    report_dir.mkdir(parents=True, exist_ok=True)
    
    # Collect all screenshots
    screenshots = []
    for r in all_results:
        if r.get("screenshot") and isinstance(r["screenshot"], dict) and "path" in r["screenshot"]:
            screenshots.append({
                "test": r["name"],
                **r["screenshot"]
            })
    
    # Create report
    report = {
        "run_date": datetime.now().isoformat(),
        "total_tests": len(all_results),
        "passed": sum(1 for r in all_results if r["passed"]),
        "failed": sum(1 for r in all_results if not r["passed"]),
        "results": all_results,
        "screenshots": screenshots
    }
    
    # Save as JSON
    json_path = report_dir / "report.json"
    with open(json_path, 'w') as f:
        json.dump(report, f, indent=2)
    
    print(f"\n[INFO] Report saved to {json_path}")


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
