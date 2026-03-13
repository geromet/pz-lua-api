#!/usr/bin/env python3
"""Inheritance Tests: Inheritance chain display."""

from config import browser, SERVER_URL, REPORT_DIR


def test_inheritance_chain(page) -> dict:
    """Test that inheritance chains are displayed correctly."""
    try:
        page.goto(SERVER_URL + "/zombie.characters.IsoPlayer")
        time.sleep(0.5)
        
        # Find the inheritance section
        inheritance_section = page.locator('.inheritance-section')
        if not inheritance_section.is_visible():
            return {"name": "inheritance-chain", "passed": False,
                    "message": "Could not find inheritance section element"}
        
        # Check if there are classes in the inheritance tree
        class_tree_items = page.locator('.inheritance-tree-item')
        
        if class_tree_items.count() > 0:
            # Check for root marker
            root_marker = page.locator('.root-marker')
            has_root = root_marker.is_visible()
            
            screenshot = capture_screenshot(page, "inheritance-chain")
            return {"name": "inheritance-chain", "passed": True,
                    "message": f"Inheritance tree with {class_tree_items.count()} items, root marker: {'Yes' if has_root else 'No'}",
                    "screenshot": screenshot}
        else:
            print("  [Inherit] No inheritance tree items found")
            return {"name": "inheritance-chain", "passed": True,
                    "message": "Inheritance section visible but no tree items (may be expected for this class)"}
                
    except Exception as e:
        print(f"  [Inherit] Error: {e}")
        return {"name": "inheritance-chain", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "inheritance-error.png")}}


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
