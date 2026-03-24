#!/usr/bin/env python3
"""S09 load-performance tests: split JSON boot, lazy detail fetch, error state, SW registration."""

from __future__ import annotations

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


def _force_split_mode(page):
    """Block versions.json so the app falls into split-index mode."""
    page.route("**/versions/versions.json", lambda route: route.fulfill(status=404, body=""))


def _open(page, path: str = "/"):
    """Navigate and wait for loading to complete."""
    page.goto(f"{SERVER_URL}{path}")
    page.locator("#loading").wait_for(state="hidden", timeout=15000)


# ── Test 1: Index loads, monolithic lua_api.json does NOT ─────────────────

def test_index_loads_not_full_json(page):
    """On boot (no versions manifest), only lua_api_index.json is fetched, not lua_api.json."""
    _force_split_mode(page)

    fetched_urls: list[str] = []

    def _on_request(request):
        fetched_urls.append(request.url)

    page.on("request", _on_request)
    _open(page, "/")

    paths = [u.split(SERVER_URL)[-1] for u in fetched_urls]

    assert any("lua_api_index.json" in p for p in paths), (
        f"Expected lua_api_index.json to be fetched. Requests: {paths}"
    )
    assert not any(p.endswith("/lua_api.json") for p in paths), (
        f"lua_api.json should NOT be fetched in split mode. Requests: {paths}"
    )

    # Confirm split mode is active in the runtime
    is_split = page.evaluate("() => window._apiSplit")
    assert is_split is True, "Expected window._apiSplit to be true"


# ── Test 2: Clicking a class triggers a lazy detail fetch ─────────────────

def test_detail_lazy_fetch(page):
    """Clicking a class in the sidebar triggers a lua_api_detail/*.json fetch
    and the detail panel reaches data-detail-state='ready'."""
    _force_split_mode(page)

    detail_fetches: list[str] = []

    def _on_request(request):
        if "lua_api_detail/" in request.url:
            detail_fetches.append(request.url)

    page.on("request", _on_request)
    _open(page, "/")

    # Pick the first class in the sidebar
    first_item = page.locator("#class-list .class-item[data-fqn]").first
    expect(first_item).to_be_visible(timeout=5000)
    fqn = first_item.get_attribute("data-fqn")
    assert fqn, "Expected a class item with a data-fqn attribute"

    first_item.click()

    # Wait for the detail panel to reach 'ready'
    expect(page.locator("#detail-panel")).to_have_attribute(
        "data-detail-state", "ready", timeout=10000
    )

    # At least one detail fetch should have occurred
    assert len(detail_fetches) > 0, (
        f"Expected at least one lua_api_detail/ fetch after clicking class '{fqn}'. "
        f"Got: {detail_fetches}"
    )
    assert any(fqn in u for u in detail_fetches), (
        f"Expected a detail fetch for '{fqn}'. Fetches: {detail_fetches}"
    )


# ── Test 3: Failed detail fetch sets error state ─────────────────────────

def test_detail_error_state(page):
    """When a detail JSON fetch fails (404), the detail panel shows an error
    with data-detail-state='error' and an error message."""
    _force_split_mode(page)
    _open(page, "/")

    # In split mode, index entries lack methods — find one that hasn't been fetched
    target_fqn = page.evaluate(
        "() => Object.keys(API.classes).find(k => API.classes[k].methods === undefined)"
    )
    assert target_fqn, "Need at least one class that hasn't been detail-fetched yet"

    # Intercept that class's detail fetch and return 404
    page.route(
        f"**/lua_api_detail/{target_fqn}.json",
        lambda route: route.fulfill(status=404, body="Not Found"),
    )

    # Trigger detail render via JS
    page.evaluate(f"renderClassDetail('{target_fqn}')")

    # Verify error state
    expect(page.locator("#detail-panel")).to_have_attribute(
        "data-detail-state", "error", timeout=10000
    )

    error_text = page.locator("#detail-panel").inner_text()
    assert "failed to load" in error_text.lower() or "404" in error_text, (
        f"Expected error message in detail panel, got: {error_text[:200]}"
    )


# ── Test 4: Service worker registration ──────────────────────────────────

def test_sw_registration(page):
    """The service worker is registered on page load."""
    sw_messages: list[str] = []

    def _on_console(msg):
        if "[SW]" in msg.text:
            sw_messages.append(msg.text)

    page.on("console", _on_console)
    _open(page, "/")

    # Give SW registration a moment to complete
    page.wait_for_timeout(2000)

    has_sw = page.evaluate(
        """() => navigator.serviceWorker
            ? navigator.serviceWorker.getRegistration().then(r => !!r)
            : false"""
    )
    assert has_sw, (
        f"Expected service worker to be registered. Console SW messages: {sw_messages}"
    )
