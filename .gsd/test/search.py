#!/usr/bin/env python3
"""Search Tests: Basic search and scope toggle."""

from config import browser, SERVER_URL, REPORT_DIR


def test_search_basic(page) -> dict:
    """Test basic search functionality."""
    try:
        page.goto(SERVER_URL + "/")
        time.sleep(0.5)
        
        # Find the search input
        search_input = page.locator('input[name="search"]')
        if not search_input.is_visible():
            return {"name": "search-basic", "passed": False,
                    "message": "Could not find search input element"}
        
        # Type a search query
        search_input.fill("IsoPlayer")
        time.sleep(0.3)
        
        # Check if results appear
        results = page.locator('.class-result-item')
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
    """Test the search scope toggle."""
    try:
        page.goto(SERVER_URL + "/")
        time.sleep(0.5)
        
        # Find the scope toggle (checkbox or button)
        scope_toggle = page.locator('input[name="scope"]')
        if not scope_toggle.is_visible():
            return {"name": "search-scope-toggle", "passed": False,
                    "message": "Could not find scope toggle element"}
        
        # Toggle the scope
        scope_toggle.click()
        time.sleep(0.3)
        
        screenshot = capture_screenshot(page, "search-scope-toggled")
        return {"name": "search-scope-toggle", "passed": True,
                "message": "Scope toggle clicked successfully",
                "screenshot": screenshot}
                
    except Exception as e:
        print(f"  [Search] Error: {e}")
        return {"name": "search-scope-toggle", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "search-scope-error.png")}}


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
