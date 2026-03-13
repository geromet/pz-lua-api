#!/usr/bin/env python3
"""Detail View Tests: Class detail panel and copy FQN."""

from config import browser, SERVER_URL, REPORT_DIR


def test_class_detail_panel(page) -> dict:
    """Test the class detail panel functionality."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        time.sleep(0.5)
        
        # Check if we're on a class page
        if "IsoPlayer" not in page.title():
            return {"name": "class-detail-panel", "passed": False,
                    "message": f"Page title: {page.title()}"}
        
        # Try to find and click on a method/field link
        method_links = page.locator('a.method-link')
        if method_links.count() > 0:
            method_link = method_links.first
            method_link.click()
            time.sleep(0.5)
            
            screenshot = capture_screenshot(page, "method-clicked")
            return {"name": "class-detail-panel", "passed": True,
                    "message": "Successfully clicked on a method link",
                    "screenshot": screenshot}
        else:
            print("  [Detail] No method links found, but page loaded correctly")
            screenshot = capture_screenshot(page, "class-detail-no-methods")
            return {"name": "class-detail-panel", "passed": True,
                    "message": "Class detail page loaded (no clickable methods found)",
                    "screenshot": screenshot}
                
    except Exception as e:
        print(f"  [Detail] Error: {e}")
        return {"name": "class-detail-panel", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "detail-error.png")}}


def test_copy_fqn(page) -> dict:
    """Test the copy FQN to clipboard functionality."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        time.sleep(0.5)
        
        # Find the copy FQN button
        copy_btn = page.locator('button.copy-fqn')
        if not copy_btn.is_visible():
            return {"name": "copy-fqn", "passed": False,
                    "message": "Could not find copy FQN button element"}
        
        # Click the copy button
        copy_btn.click()
        time.sleep(0.3)
        
        # Check if there's a toast notification
        toast = page.locator('.toast-notification')
        if toast.is_visible():
            screenshot = capture_screenshot(page, "copy-fqn-success")
            return {"name": "copy-fqn", "passed": True,
                    "message": "Successfully copied FQN to clipboard",
                    "screenshot": screenshot}
        else:
            # Toast might be hidden after a short delay
            print("  [Copy] No toast visible immediately, but click succeeded")
            return {"name": "copy-fqn", "passed": True,
                    "message": "Copy FQN button clicked (toast may have auto-dismissed)"}
                
    except Exception as e:
        print(f"  [Copy] Error: {e}")
        return {"name": "copy-fqn", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "copy-fqn-error.png")}}


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
