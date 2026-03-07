#!/usr/bin/env python3
"""
webapp/server.py — Brain interface server.

Loads brain/context/*.md as system prompt.
Routes:
    GET  /       → index.html
    POST /chat   → Mistral inference + filter.py contamination check

Requires:
    pip install flask mistralai
    MISTRAL_API_KEY in environment
"""

import os
import sys
import json
from pathlib import Path

from flask import Flask, request, jsonify, send_from_directory

# Import brain.filter from project root
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from brain.filter import filter_output

try:
    from mistralai import Mistral
except ImportError:
    print("ERROR: mistralai not installed. Run: pip install mistralai", file=sys.stderr)
    sys.exit(1)

app = Flask(__name__)
WEBAPP_DIR = Path(__file__).parent


# ---------------------------------------------------------------------------
# System context — loaded once at startup
# ---------------------------------------------------------------------------

CONTEXT_DIR = ROOT / "brain" / "context"


def load_system_context() -> str:
    parts = []
    for f in sorted(CONTEXT_DIR.glob("*.md")):
        content = f.read_text().strip()
        parts.append(f"# {f.stem}\n\n{content}")
    if not parts:
        return "You are a direct, no-bullshit assistant. No hedging. No managed tone."
    return "\n\n---\n\n".join(parts)


SYSTEM_CONTEXT = load_system_context()
MISTRAL_MODEL = os.environ.get("MISTRAL_MODEL", "mistral-large-latest")


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@app.route("/")
def index():
    return send_from_directory(WEBAPP_DIR, "index.html")


@app.route("/chat", methods=["POST"])
def chat():
    data = request.get_json(silent=True)
    if not data or not data.get("message", "").strip():
        return jsonify({"error": "No message provided"}), 400

    user_message = data["message"].strip()

    api_key = os.environ.get("MISTRAL_API_KEY")
    if not api_key:
        return jsonify({"error": "MISTRAL_API_KEY not set on server"}), 500

    client = Mistral(api_key=api_key)

    try:
        response = client.chat.complete(
            model=MISTRAL_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_CONTEXT},
                {"role": "user", "content": user_message},
            ],
            temperature=0.7,
            max_tokens=1024,
        )
        raw_output = response.choices[0].message.content
    except Exception as e:
        return jsonify({"error": f"Mistral error: {e}"}), 500

    # Pass 1 — deterministic contamination filter
    filter_result = filter_output(raw_output)
    clean_text = filter_result.stripped() if filter_result.flagged else raw_output

    return jsonify({
        "response": clean_text,
        "raw": raw_output,
        "flagged": filter_result.flagged,
        "filter_summary": filter_result.summary_line(),
        "filter_matches": [
            {
                "category": m.category,
                "severity_label": m.severity_label,
                "note": m.note,
                "matched_text": m.matched_text,
            }
            for m in filter_result.by_severity()
        ],
    })


@app.route("/context", methods=["GET"])
def context_info():
    """Debug: show what system context is loaded."""
    files = [f.name for f in sorted(CONTEXT_DIR.glob("*.md"))]
    return jsonify({
        "files": files,
        "char_count": len(SYSTEM_CONTEXT),
        "model": MISTRAL_MODEL,
    })


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    debug = os.environ.get("DEBUG", "false").lower() == "true"

    files_loaded = [f.name for f in sorted(CONTEXT_DIR.glob("*.md"))]
    print(f"Brain interface starting on http://localhost:{port}")
    print(f"Model: {MISTRAL_MODEL}")
    print(f"Context files: {files_loaded} ({len(SYSTEM_CONTEXT)} chars)")
    print(f"Filter: {len(__import__('brain.filter', fromlist=['SIGNATURES']).SIGNATURES)} signatures loaded")

    app.run(host="0.0.0.0", port=port, debug=debug)
