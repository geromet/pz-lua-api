"""Serve the PZ Lua API Viewer locally on http://localhost:8000"""
import http.server
import webbrowser
import os
from pathlib import Path

PORT = 8000
REPO_ROOT = Path(__file__).resolve().parent.parent
os.chdir(REPO_ROOT)

print(f"Serving at http://localhost:{PORT}")
print("Press Ctrl+C to stop.\n")
webbrowser.open(f"http://localhost:{PORT}")

http.server.test(
    HandlerClass=http.server.SimpleHTTPRequestHandler,
    port=PORT,
    bind="127.0.0.1",
)
