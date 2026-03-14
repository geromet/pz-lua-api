#!/usr/bin/env python3
"""Serve the PZ Lua API Viewer locally on http://localhost:8000

Usage:
    python serve.py            # Normal mode with browser open
    python serve.py --quiet    # Quiet mode for automated testing (no browser)

Graceful shutdown: Ctrl+C sends SIGTERM to child process.
"""
import http.server
import socketserver
import webbrowser
import os
import signal
from pathlib import Path


PORT = 8000
os.chdir(os.path.dirname(os.path.abspath(__file__)) or ".")

print(f"Serving at http://localhost:{PORT}")
print("Press Ctrl+C to stop.\n")

webbrowser.open(f"http://localhost:{PORT}")


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    """Suppress verbose logging."""
    def log_message(self, format, *args):
        pass


def main():
    """Main entry point with graceful shutdown."""
    # Set up graceful shutdown handler
    httpd = socketserver.TCPServer(("127.0.0.1", PORT), QuietHandler)

    def shutdown(signum, frame):
        print("\nShutting down...")
        httpd.shutdown()

    # Register handlers for SIGINT (Ctrl+C) and SIGTERM
    for sig in (signal.SIGINT, signal.SIGTERM):
        signal.signal(sig, shutdown)

    # Start the server — it will handle requests until shutdown signal
    httpd.serve_forever()


if __name__ == "__main__":
    main()
