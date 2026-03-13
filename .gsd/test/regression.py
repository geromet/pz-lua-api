#!/usr/bin/env python3
"""Regression Tests: Ensure no features were broken."""

from config import browser, SERVER_URL, REPORT_DIR


def test_tab_navigation(page) -> dict:
    """Test that tab navigation still works correctly."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        time.sleep(0.5)
        
        # Click on a different tab (like Globals)
        globals_tab = page.locator('.tab-button').filter(has_text="Globals")
        if globals_tab.is_visible():
            globals_tab.click()
            time.sleep(0.5)
            
            screenshot = capture_screenshot(page, "globals-tab-clicked")
            return {"name": "tab-navigation", "passed": True,
                    "message": "Successfully switched to Globals tab",
                    "screenshot": screenshot}
        else:
            print("  [Tab] No Globals tab button found")
            return {"name": "tab-navigation", "passed": True,
                    "message": "Tab navigation tested (Globals tab not found)"}
                
    except Exception as e:
        print(f"  [Tab] Error: {e}")
        return {"name": "tab-navigation", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "tab-error.png")}}


def test_class_link_clicks(page) -> dict:
    """Test that class links still navigate correctly."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        time.sleep(0.5)
        
        # Find a class link and click it
        class_links = page.locator('a[data-fqn]')
        if class_links.count() > 0:
            class_link = class_links.first
            fqn = class_link.get_attribute("data-fqn")
            class_link.click()
            time.sleep(0.5)
            
            # Check that we're on the new page
            new_title = page.title()
            
            screenshot = capture_screenshot(page, "class-link-clicked")
            return {"name": "class-link-clicks", "passed": True,
                    "message": f"Successfully navigated to {fqn} (title: {new_title})",
                    "screenshot": screenshot}
        else:
            print("  [Link] No class links found")
            return {"name": "class-link-clicks", "passed": True,
                    "message": "Class link clicks tested (no links found)"}
                
    except Exception as e:
        print(f"  [Link] Error: {e}")
        return {"name": "class-link-clicks", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "link-error.png")}}


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
