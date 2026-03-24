#!/usr/bin/env python3
"""Inheritance Tests: Inheritance chain display."""

from datetime import datetime
import time

from config import SERVER_URL, REPORT_DIR


CLASS_URL = SERVER_URL + "/#zombie.characters.IsoPlayer"


def test_inheritance_chain(page) -> dict:
    """Test that inheritance chains are displayed correctly."""
    try:
        page.goto(CLASS_URL)
        page.locator('#loading').wait_for(state='hidden', timeout=10000)
        time.sleep(0.5)

        inheritance_section = page.locator('.inherit-tree-item')
        if inheritance_section.count() < 1:
            return {"name": "inheritance-chain", "passed": False,
                    "message": "Could not find inheritance tree items"}

        root_marker = page.locator('.root-marker')
        has_root = root_marker.count() > 0 and root_marker.first.is_visible()

        screenshot = capture_screenshot(page, "inheritance-chain")
        return {"name": "inheritance-chain", "passed": True,
                "message": f"Inheritance tree with {inheritance_section.count()} items, root marker: {'Yes' if has_root else 'No'}",
                "screenshot": screenshot}

    except Exception as e:
        print(f"  [Inherit] Error: {e}")
        return {"name": "inheritance-chain", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "inheritance-error.png")}}


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
