#!/usr/bin/env python3
"""Search Tests: Basic search and filter control."""

from datetime import datetime
import time

from config import SERVER_URL, REPORT_DIR


def test_search_basic(page) -> dict:
    """Test basic search functionality."""
    try:
        page.goto(SERVER_URL + "/")
        page.locator('#loading').wait_for(state='hidden', timeout=10000)

        search_input = page.locator('#global-search')
        if not search_input.is_visible():
            return {"name": "search-basic", "passed": False,
                    "message": "Could not find global search input element"}

        search_input.fill("IsoPlayer")
        time.sleep(0.3)

        results = page.locator('#class-list .class-item')
        if results.count() < 1:
            return {"name": "search-basic", "passed": False,
                    "message": "No class results found for 'IsoPlayer'"}

        screenshot = capture_screenshot(page, "search-results")
        return {"name": "search-basic", "passed": True,
                "message": f"Found {results.count()} results for 'IsoPlayer'",
                "screenshot": screenshot}

    except Exception as e:
        print(f"  [Search] Error: {e}")
        return {"name": "search-basic", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "search-error.png")}}


def test_search_scope_toggle(page) -> dict:
    """Test the class filter control."""
    try:
        page.goto(SERVER_URL + "/")
        page.locator('#loading').wait_for(state='hidden', timeout=10000)

        scope_toggle = page.locator('#filter-select')
        if not scope_toggle.is_visible():
            return {"name": "search-scope-toggle", "passed": False,
                    "message": "Could not find filter select element"}

        scope_toggle.select_option('enum')
        time.sleep(0.3)

        active_chip = page.locator('#active-filter-chip')
        if active_chip.get_attribute('data-filter') != 'enum':
            return {"name": "search-scope-toggle", "passed": False,
                    "message": "Filter chip did not update to enum"}

        screenshot = capture_screenshot(page, "search-scope-toggled")
        return {"name": "search-scope-toggle", "passed": True,
                "message": "Filter select updated successfully",
                "screenshot": screenshot}

    except Exception as e:
        print(f"  [Search] Error: {e}")
        return {"name": "search-scope-toggle", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "search-scope-error.png")}}


def capture_screenshot(page, name, selector=None):
    """Capture a screenshot with metadata."""
    from config import REPORT_DIR
    screenshot_path = REPORT_DIR / f"{name}.png"
    screenshot_path.parent.mkdir(parents=True, exist_ok=True)

    if selector:
        element = page.locator(selector).first
        element.screenshot(path=str(screenshot_path))
    else:
        page.screenshot(path=str(screenshot_path))

    import hashlib
    file_hash = hashlib.md5(open(screenshot_path, 'rb').read()).hexdigest()[:8]

    return {
        "path": str(screenshot_path),
        "hash": file_hash,
        "timestamp": datetime.now().isoformat(),
        "selector": selector
    }
