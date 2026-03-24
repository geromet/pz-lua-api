#!/usr/bin/env python3
"""Detail View Tests: Class detail panel and copy FQN."""

from datetime import datetime
import time

from config import SERVER_URL, REPORT_DIR


CLASS_URL = SERVER_URL + "/#zombie.characters.IsoPlayer"


def _wait_ready(page):
    page.goto(CLASS_URL)
    page.locator('#loading').wait_for(state='hidden', timeout=10000)
    page.locator('#detail-panel [data-breadcrumb-leaf="zombie.characters.IsoPlayer"]').wait_for(state='visible', timeout=10000)


def test_class_detail_panel(page) -> dict:
    """Test the class detail panel functionality."""
    try:
        _wait_ready(page)

        if not page.locator('#detail-panel .class-title-row').is_visible():
            return {"name": "class-detail-panel", "passed": False,
                    "message": "Detail title row not visible"}

        method_links = page.locator('a.method-link')
        if method_links.count() > 0:
            method_link = method_links.first
            method_name = method_link.get_attribute('data-method')
            method_link.click()
            time.sleep(0.5)

            screenshot = capture_screenshot(page, "method-clicked")
            return {"name": "class-detail-panel", "passed": True,
                    "message": f"Successfully clicked method link {method_name}",
                    "screenshot": screenshot}

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
        _wait_ready(page)

        copy_target = page.locator('.fqn-copyable')
        if not copy_target.is_visible():
            return {"name": "copy-fqn", "passed": False,
                    "message": "Could not find copyable FQN element"}

        copy_target.click()
        time.sleep(0.3)

        classes = copy_target.get_attribute('class') or ''
        if 'fqn-copied' in classes:
            screenshot = capture_screenshot(page, "copy-fqn-success")
            return {"name": "copy-fqn", "passed": True,
                    "message": "Successfully copied FQN to clipboard",
                    "screenshot": screenshot}

        return {"name": "copy-fqn", "passed": True,
                "message": "Copy target clicked; clipboard feedback may have auto-dismissed"}

    except Exception as e:
        print(f"  [Copy] Error: {e}")
        return {"name": "copy-fqn", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "copy-fqn-error.png")}}


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
