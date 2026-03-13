#!/usr/bin/env python3
"""Globals Tests: Globals panel functionality."""

from config import browser, SERVER_URL, REPORT_DIR


def test_globals_panel(page) -> dict:
    """Test the globals panel functionality."""
    try:
        page.goto(SERVER_URL + "/globals")
        time.sleep(0.5)
        
        # Check if globals table is visible
        globals_table = page.locator('.global-function-item')
        if globals_table.count() < 1:
            return {"name": "globals-panel", "passed": False,
                    "message": "Could not find any global functions in the globals panel"}
        
        # Click on a global function
        first_global = globals_table.first
        first_global.click()
        time.sleep(0.5)
        
        # Check if we're viewing that global
        current_title = page.title()
        if "global" in current_title.lower() or "Global Functions" in current_title:
            screenshot = capture_screenshot(page, "globals-clicked")
            return {"name": "globals-panel", "passed": True,
                    "message": f"Clicked on a global function, title: {current_title}",
                    "screenshot": screenshot}
        else:
            return {"name": "globals-panel", "passed": False,
                    "message": f"Expected globals view, got: {current_title}"}
                
    except Exception as e:
        print(f"  [Globals] Error: {e}")
        return {"name": "globals-panel", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "globals-error.png")}}


def test_globals_search(page) -> dict:
    """Test searching within the globals panel."""
    try:
        page.goto(SERVER_URL + "/globals")
        time.sleep(0.5)
        
        # Find the search input in globals
        search_input = page.locator('input[name="global-search"]')
        if not search_input.is_visible():
            return {"name": "globals-search", "passed": False,
                    "message": "Could not find globals search input element"}
        
        # Type a search query
        search_input.fill("print")
        time.sleep(0.3)
        
        # Check if results appear
        results = page.locator('.global-function-item')
        if results.count() < 1:
            return {"name": "globals-search", "passed": False,
                    "message": "No global functions found matching 'print'"}
        
        screenshot = capture_screenshot(page, "globals-search-results")
        return {"name": "globals-search", "passed": True,
                "message": f"Found {results.count()} results for 'print' in globals",
                "screenshot": screenshot}
                
    except Exception as e:
        print(f"  [Globals] Error: {e}")
        return {"name": "globals-search", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "globals-search-error.png")}}


def capture_screenshot(page, name, selector=None):
    """Capture a screenshot with metadata."""
    from config import REPORT_DIR
    from playwright.sync_api import BrowserContext
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

from datetime import datetime
import time
