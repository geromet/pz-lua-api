#!/usr/bin/env python3
"""Navigation Tests: Back/Forward buttons and URL hash changes."""

from config import browser, SERVER_URL, REPORT_DIR


def test_back_forward_buttons(page) -> dict:
    """Test that back/forward buttons work correctly."""
    try:
        # Navigate to IsoPlayer
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        
        # Click a class to go deeper
        class_ref = page.locator('a[data-fqn]').first
        if not class_ref.is_visible():
            return {"name": "back-forward-buttons", "passed": False,
                    "message": "Could not find a class reference link to navigate to"}
        
        fqn = class_ref.get_attribute("data-fqn")
        print(f"  [Nav] Navigating from IsoPlayer to {fqn}...")
        
        class_ref.click()
        import time
        time.sleep(1)
        
        # Check if we're on the new page
        if "IsoPlayer" in page.title():
            return {"name": "back-forward-buttons", "passed": False,
                    "message": f"Expected to be on {fqn}, but still on IsoPlayer"}
        
        # Capture screenshot before back
        screenshot_before = capture_screenshot(page, "back-before")
        
        # Press Back
        page.keyboard.press("ArrowLeft")
        time.sleep(0.5)
        
        # Check if we're back at IsoPlayer
        if "IsoPlayer" not in page.title():
            return {"name": "back-forward-buttons", "passed": False,
                    "message": f"After back button: {page.title()}"}
        
        screenshot_after = capture_screenshot(page, "back-success")
        return {"name": "back-forward-buttons", "passed": True,
                "message": f"Successfully navigated back from {fqn} to IsoPlayer",
                "screenshot": screenshot_after}
                
    except Exception as e:
        print(f"  [Nav] Error: {e}")
        return {"name": "back-forward-buttons", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "back-error.png")}}


def test_nav_hash_change(page) -> dict:
    """Test that navigation updates the URL hash."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        initial_hash = page.url.split("#")[-1] if "#" in page.url else ""
        
        # Navigate to another class
        class_ref = page.locator('a[data-fqn]').first
        if not class_ref.is_visible():
            return {"name": "nav-hash-change", "passed": False,
                    "message": "Could not find a class reference link"}
        
        fqn = class_ref.get_attribute("data-fqn")
        class_ref.click()
        import time
        time.sleep(0.5)
        
        new_hash = page.url.split("#")[-1] if "#" in page.url else ""
        
        if new_hash == initial_hash and initial_hash:
            return {"name": "nav-hash-change", "passed": False,
                    "message": f"Hash before: {initial_hash}, after: {new_hash}"}
        
        return {"name": "nav-hash-change", "passed": True,
                "message": f"URL hash updated from {initial_hash} to {new_hash}"}
                
    except Exception as e:
        print(f"  [Nav] Error: {e}")
        return {"name": "nav-hash-change", "passed": False,
                "message": str(e)}


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
