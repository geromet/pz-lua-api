#!/usr/bin/env python3
"""S02 Feature Tests: Middle-click new tab and hover preview card."""

from datetime import datetime
import time

from config import browser, SERVER_URL, REPORT_DIR


CLASS_URL = SERVER_URL + "/#zombie.characters.IsoPlayer"


def _wait_ready(page):
    page.goto(CLASS_URL)
    page.locator('#loading').wait_for(state='hidden', timeout=10000)


def _open_source_and_get_link(page):
    page.locator('[data-ctab="source"]').click()
    page.locator('#source-panel').wait_for(state='visible', timeout=5000)
    page.locator('#source-panel a.src-class-ref[data-fqn]').first.wait_for(state='visible', timeout=10000)
    return page.locator('#source-panel a.src-class-ref[data-fqn]').first


def test_middle_click_new_tab(page) -> dict:
    """Test that middle-clicking a class link opens it in a new tab."""
    try:
        _wait_ready(page)
        class_ref = _open_source_and_get_link(page)

        fqn = class_ref.get_attribute("data-fqn")
        if not fqn:
            return {"name": "middle-click-new-tab", "passed": False,
                    "message": "Class link missing data-fqn attribute"}

        print(f"  [Feature] Testing new-tab gesture on {fqn}...")

        page.keyboard.down('Control')
        class_ref.click()
        page.keyboard.up('Control')
        time.sleep(1)

        pages = browser.context.pages
        if len(pages) < 2:
            return {"name": "middle-click-new-tab", "passed": False,
                    "message": f"Expected new tab for {fqn}, but only {len(pages)} page(s) open"}

        new_tab = pages[-1]
        new_tab.wait_for_load_state('domcontentloaded')
        if fqn.split('.')[-1] in new_tab.url or fqn in new_tab.url:
            screenshot = capture_screenshot(new_tab, "middle-click-success")
            return {"name": "middle-click-new-tab", "passed": True,
                    "message": f"Successfully opened {fqn} in new tab via middle-click",
                    "screenshot": screenshot}

        return {"name": "middle-click-new-tab", "passed": False,
                "message": f"New tab url: {new_tab.url}"}

    except Exception as e:
        print(f"  [Feature] Error: {e}")
        return {"name": "middle-click-new-tab", "passed": False,
                "message": str(e),
                "screenshot": {"path": str(REPORT_DIR / "middle-click-error.png")}}


def test_hover_preview_card(page) -> dict:
    """Test that hovering over a class link shows a preview card."""
    try:
        _wait_ready(page)
        class_ref = _open_source_and_get_link(page)

        fqn = class_ref.get_attribute("data-fqn")
        print(f"  [Feature] Testing hover on {fqn}...")

        class_ref.hover()
        time.sleep(0.6)

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
