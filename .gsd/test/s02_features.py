#!/usr/bin/env python3
"""S02 Feature Tests: Middle-click new tab and hover preview card."""

from playwright.sync_api import expect
from config import browser, SERVER_URL, REPORT_DIR


def test_middle_click_new_tab(page) -> dict:
    """Test that middle-clicking a class link opens it in a new tab."""
    try:
        # Navigate to IsoPlayer
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        
        # Find a class reference link in the source view
        class_ref = page.locator('a[data-fqn]').first
        if not class_ref.is_visible():
            return {"name": "middle-click-new-tab", "passed": False, 
                    "message": "Could not find a class reference link to click"}
        
        # Get the FQN from the link
        fqn = class_ref.get_attribute("data-fqn")
        if not fqn:
            return {"name": "middle-click-new-tab", "passed": False,
                    "message": "Class link missing data-fqn attribute"}
        
        print(f"  [Feature] Testing middle-click on {fqn}...")
        
        # Middle-click (button 1)
        class_ref.click(button=1)
        import time
        time.sleep(1)
        
        # Check if a new tab opened with the class loaded
        pages = browser.context.pages
        if len(pages) < 2:
            return {"name": "middle-click-new-tab", "passed": False,
                    "message": f"Expected new tab for {fqn}, but only {len(pages)} page(s) open"}
        
        # Check if the new tab has the correct content
        new_tab = pages[-1]
        if "IsoPlayer" in new_tab.title() or "IsoLivingCharacter" in new_tab.title():
            screenshot = capture_screenshot(new_tab, "middle-click-success")
            return {"name": "middle-click-new-tab", "passed": True,
                    "message": f"Successfully opened {fqn} in new tab via middle-click",
                    "screenshot": screenshot}
        else:
            return {"name": "middle-click-new-tab", "passed": False,
                    "message": f"New tab title: {new_tab.title()}"}
                
    except Exception as e:
        print(f"  [Feature] Error: {e}")
        return {"name": "middle-click-new-tab", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "middle-click-error.png")}}


def test_hover_preview_card(page) -> dict:
    """Test that hovering over a class link shows a preview card."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        
        # Find a class reference and hover over it
        class_ref = page.locator('a[data-fqn]').first
        if not class_ref.is_visible():
            return {"name": "hover-preview-card", "passed": False,
                    "message": "Could not find a class reference link to hover over"}
        
        fqn = class_ref.get_attribute("data-fqn")
        print(f"  [Feature] Testing hover on {fqn}...")
        
        # Hover for 400ms (the delay before showing)
        class_ref.hover()
        import time
        time.sleep(0.5)  # Wait for the hover effect
        
        # Check if preview card is visible
        preview_card = page.locator("#hover-preview")
        if not preview_card.is_visible():
            return {"name": "hover-preview-card", "passed": False,
                    "message": "Preview card element not found or not visible after hover"}
        
        screenshot = capture_screenshot(page, "hover-preview-success")
        return {"name": "hover-preview-card", "passed": True,
                "message": f"Preview card appeared for {fqn}",
                "screenshot": screenshot}
                
    except Exception as e:
        print(f"  [Feature] Error: {e}")
        return {"name": "hover-preview-card", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "hover-preview-error.png")}}


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
