#!/usr/bin/env python3
"""Globals Tests: Globals panel functionality."""

from datetime import datetime
import time

from config import SERVER_URL, REPORT_DIR


GLOBALS_URL = SERVER_URL + "/#globals"


def _wait_ready(page):
    page.goto(GLOBALS_URL)
    page.locator('#loading').wait_for(state='hidden', timeout=10000)
    page.locator('#globals-panel').wait_for(state='visible', timeout=10000)


def test_globals_panel(page) -> dict:
    """Test the globals panel functionality."""
    try:
        _wait_ready(page)

        globals_table = page.locator('.gfn-row')
        if globals_table.count() < 1:
            return {"name": "globals-panel", "passed": False,
                    "message": "Could not find any global functions in the globals panel"}

        first_global = page.locator('.gfn-link').first
        method_name = first_global.get_attribute('data-method')
        first_global.click()
        time.sleep(0.5)

        source_wrap = page.locator('#globals-source-wrap')
        if source_wrap.get_attribute('class') and 'has-source' in source_wrap.get_attribute('class'):
            screenshot = capture_screenshot(page, "globals-clicked")
            return {"name": "globals-panel", "passed": True,
                    "message": f"Opened global source for {method_name}",
                    "screenshot": screenshot}

        return {"name": "globals-panel", "passed": False,
                "message": f"Expected globals source view for {method_name}"}

    except Exception as e:
        print(f"  [Globals] Error: {e}")
        return {"name": "globals-panel", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "globals-error.png")}}


def test_globals_search(page) -> dict:
    """Test searching within the globals panel."""
    try:
        _wait_ready(page)

        search_input = page.locator('#globals-search')
        if not search_input.is_visible():
            return {"name": "globals-search", "passed": False,
                    "message": "Could not find globals search input element"}

        search_input.fill("get")
        time.sleep(0.3)

        results = page.locator('.gfn-row')
        if results.count() < 1:
            return {"name": "globals-search", "passed": False,
                    "message": "No global functions found matching 'get'"}

        screenshot = capture_screenshot(page, "globals-search-results")
        return {"name": "globals-search", "passed": True,
                "message": f"Found {results.count()} results for 'get' in globals",
                "screenshot": screenshot}

    except Exception as e:
        print(f"  [Globals] Error: {e}")
        return {"name": "globals-search", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "globals-search-error.png")}}


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
