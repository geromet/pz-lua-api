#!/usr/bin/env python3
"""S08 navigation-state tests: URL serialization, restoration, and diagnostics."""

from __future__ import annotations

import json
import subprocess
import time
from pathlib import Path
from urllib.request import urlopen

import pytest
from playwright.sync_api import expect, sync_playwright

PROJECT_DIR = Path(__file__).resolve().parent.parent.parent
SERVER_SCRIPT = PROJECT_DIR / "server" / "serve.py"
SERVER_URL = "http://127.0.0.1:8000"


@pytest.fixture(scope="module")
def server_process():
    proc = subprocess.Popen(
        ["python", str(SERVER_SCRIPT)],
        cwd=PROJECT_DIR,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )

    deadline = time.time() + 20
    last_error = None
    while time.time() < deadline:
        try:
            with urlopen(SERVER_URL, timeout=1) as response:
                if response.status == 200:
                    yield proc
                    proc.terminate()
                    try:
                        proc.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        proc.kill()
                    return
        except Exception as exc:  # pragma: no cover - startup polling only
            last_error = exc
            time.sleep(0.25)

    proc.terminate()
    raise RuntimeError(f"Server did not become ready: {last_error}")


@pytest.fixture()
def page(server_process):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 1000})
        yield page
        browser.close()


def _open(page, path: str):
    page.goto(f"{SERVER_URL}{path}")
    page.locator("#loading").wait_for(state="hidden", timeout=10000)


def _nav_dataset(page):
    raw = page.locator("#content").evaluate(
        """
        (el) => ({
          serialized: el.dataset.navSerializedState || '',
          parsed: el.dataset.navParsedState || '',
          applied: el.dataset.navAppliedState || '',
          restoreStatus: el.dataset.navRestoreStatus || '',
          restoreReason: el.dataset.navRestoreReason || '',
          restoreError: el.dataset.navRestoreError || '',
          urlStateSource: el.dataset.navUrlStateSource || '',
          urlStateHref: el.dataset.navUrlStateHref || '',
        })
        """
    )
    for key in ("serialized", "parsed", "applied"):
        raw[key] = json.loads(raw[key]) if raw[key] else {}
    return raw


def _recent_dataset(page):
    return page.locator("#recent-classes").evaluate(
        """
        (el) => ({
          state: el.dataset.recentState || '',
          count: el.dataset.recentCount || '',
          source: el.dataset.recentSource || '',
          pruned: el.dataset.recentPruned || '',
          lastAction: el.dataset.recentLastAction || '',
          entries: [...el.querySelectorAll('[data-recent-fqn]')].map((node) => node.dataset.recentFqn || ''),
        })
        """
    )


@pytest.mark.restore
def test_navigation_actions_serialize_shareable_url_and_preserve_version(page):
    _open(page, "/?v=r964")

    expect(page.locator("#content")).to_have_attribute("data-nav-restore-status", "defaulted")

    page.locator("#filter-select").select_option("exposed")
    selected_fqn = page.locator("#class-list .class-item[data-fqn]").first.get_attribute("data-fqn")
    assert selected_fqn, "Expected at least one class in the exposed filter view"
    page.locator(f'#class-list .class-item[data-fqn="{selected_fqn}"]').click()
    page.locator('[data-ctab="source"]').click()

    expect(page.locator("#source-panel")).to_have_attribute("data-source-state", "ready", timeout=10000)
    expect(page.locator("#content")).to_have_attribute("data-nav-restore-status", "defaulted")

    page.locator("#global-search").fill("iso")
    expect(page.locator("#global-search")).to_have_value("iso")
    page.wait_for_timeout(250)

    assert "v=r964" in page.url
    assert f"class={selected_fqn}" in page.url
    assert "search=iso" in page.url
    assert "filter=exposed" in page.url
    assert "ctab=source" in page.url

    nav = _nav_dataset(page)
    assert nav["serialized"] == {
        "tab": "classes",
        "classFqn": selected_fqn,
        "search": "iso",
        "filter": "exposed",
        "ctab": "source",
    }


@pytest.mark.restore
@pytest.mark.diagnostics
def test_crafted_url_restores_search_filter_class_and_source_state(page):
    _open(
        page,
        "/?v=r964&tab=classes&class=zombie.characters.IsoDummyCameraCharacter&search=iso&filter=exposed&ctab=source",
    )

    expect(page.locator("#global-search")).to_have_value("iso")
    expect(page.locator("#filter-select")).to_have_value("exposed")
    expect(page.locator("#detail-panel")).to_have_attribute("data-detail-fqn", "zombie.characters.IsoDummyCameraCharacter")
    expect(page.locator('[data-ctab="source"]')).to_have_attribute("class", "ctab active")
    expect(page.locator("#source-panel")).to_have_attribute("data-source-state", "ready", timeout=10000)
    expect(page.locator("#content")).to_have_attribute("data-nav-restore-status", "restored")

    nav = _nav_dataset(page)
    assert nav["urlStateSource"] == "query"
    assert nav["parsed"] == {
        "tab": "classes",
        "classFqn": "zombie.characters.IsoDummyCameraCharacter",
        "search": "iso",
        "filter": "exposed",
        "ctab": "source",
    }
    assert nav["applied"] == nav["parsed"]


@pytest.mark.recent
@pytest.mark.diagnostics
def test_recent_classes_persist_order_deduplicate_and_navigate(page):
    page.add_init_script(
        """
        window.localStorage.setItem('pzRecentClasses', JSON.stringify([
          { fqn: 'zombie.characters.IsoDummyCameraCharacter' },
          { fqn: 'zombie.characters.IsoPlayer' },
          { fqn: 'zombie.characters.DoesNotExist' }
        ]));
        """
    )
    _open(page, "/?v=r964")

    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-state", "ready")
    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-count", "2")
    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-pruned", "1")
    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-source", "localStorage")

    page.locator("#btn-recent-classes").click()
    recent_items = page.locator("#recent-classes-list [data-recent-fqn]")
    expect(recent_items).to_have_count(2)
    assert recent_items.nth(0).get_attribute("data-recent-fqn") == "zombie.characters.IsoDummyCameraCharacter"
    assert recent_items.nth(1).get_attribute("data-recent-fqn") == "zombie.characters.IsoPlayer"

    page.locator('#class-list .class-item[data-fqn="zombie.characters.IsoZombie"]').click()
    page.locator('#class-list .class-item[data-fqn="zombie.characters.IsoPlayer"]').click()
    page.locator('[data-ctab="source"]').click()
    expect(page.locator("#source-panel")).to_have_attribute("data-source-state", "ready", timeout=10000)

    page.locator("#btn-recent-classes").click()
    recent = _recent_dataset(page)
    assert recent["entries"][:3] == [
        "zombie.characters.IsoPlayer",
        "zombie.characters.IsoZombie",
        "zombie.characters.IsoDummyCameraCharacter",
    ]
    assert recent["count"] == "3"

    page.locator('#recent-classes-list [data-recent-fqn="zombie.characters.IsoDummyCameraCharacter"]').click()
    expect(page.locator("#detail-panel")).to_have_attribute("data-detail-fqn", "zombie.characters.IsoDummyCameraCharacter")
    expect(page.locator('[data-ctab="detail"]')).to_have_attribute("class", "ctab active")
    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-last-action", "panel-close")

    page.locator("#btn-recent-classes").click()
    recent_after_nav = _recent_dataset(page)
    assert recent_after_nav["entries"][:3] == [
        "zombie.characters.IsoDummyCameraCharacter",
        "zombie.characters.IsoPlayer",
        "zombie.characters.IsoZombie",
    ]


@pytest.mark.failure
@pytest.mark.diagnostics
def test_recent_classes_empty_storage_is_visible(page):
    page.add_init_script("window.localStorage.removeItem('pzRecentClasses');")
    _open(page, "/?v=r964")

    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-state", "empty")
    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-count", "0")
    expect(page.locator("#recent-classes")).to_have_attribute("data-recent-pruned", "0")

    page.locator("#btn-recent-classes").click()
    expect(page.locator("#recent-classes-list [data-recent-empty='true']")).to_be_visible()


@pytest.mark.failure
@pytest.mark.diagnostics
def test_unresolvable_class_state_exposes_fallback_diagnostics(page):
    _open(
        page,
        "/?v=r964&tab=classes&class=zombie.characters.DoesNotExist&search=iso&filter=exposed&ctab=source",
    )

    expect(page.locator("#content")).to_have_attribute("data-nav-restore-status", "fallback")
    expect(page.locator("#content")).to_have_attribute("data-nav-url-state-source", "query")
    expect(page.locator("#global-search")).to_have_value("iso")
    expect(page.locator("#filter-select")).to_have_value("exposed")
    expect(page.locator("#placeholder")).to_be_visible()

    nav = _nav_dataset(page)
    assert nav["restoreError"] == "class not found:zombie.characters.DoesNotExist"
    assert nav["applied"] == {
        "tab": "classes",
        "classFqn": "",
        "search": "iso",
        "filter": "exposed",
        "ctab": "detail",
    }
    assert "DoesNotExist" not in page.url
    assert "search=iso" in page.url
    assert "filter=exposed" in page.url
