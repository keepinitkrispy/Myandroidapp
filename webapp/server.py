#!/usr/bin/env python3
"""
webapp/server.py — Brain interface server.

Loads brain/context/*.md as system prompt.
Routes:
    GET  /       → index.html
    POST /chat   → Mistral inference + filter.py (Pass 1) + enforce.py (Pass 2)
    GET  /context → debug: loaded context files + model

Destruction pipeline per response:
    Mistral → filter.py (deterministic signatures) → enforce.py (Groq meta-eval) → clean output

Requires:
    pip install flask mistralai groq python-dotenv
    MISTRAL_API_KEY and GROQ_API_KEY in .env or environment
"""

import os
import sys
import json
from pathlib import Path

from dotenv import load_dotenv
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

from flask import Flask, request, jsonify, send_from_directory

# Import brain modules from project root
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from brain.filter import filter_output
from brain.enforce import enforce

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

    # Pass 2 — enforce.py meta-evaluation (Groq/llama-3.3-70b)
    enforcement = None
    enforcement_error = None
    if os.environ.get("GROQ_API_KEY"):
        try:
            enforcement_result = enforce(clean_text)
            enforcement = {
                "verdict": enforcement_result.verdict,  # CLEAN | DRIFT
                "checks": [
                    {
                        "id": c.id,
                        "name": c.name,
                        "verdict": c.verdict,  # PASS | FLAG
                        "finding": c.finding,
                        "quote": c.quote,
                    }
                    for c in enforcement_result.checks
                ],
                "flagged": [
                    {
                        "id": c.id,
                        "name": c.name,
                        "finding": c.finding,
                        "quote": c.quote,
                    }
                    for c in enforcement_result.flagged()
                ],
            }
        except Exception as e:
            enforcement_error = str(e)
    else:
        enforcement_error = "GROQ_API_KEY not set — enforcement skipped"

    return jsonify({
        "response": clean_text,
        "raw": raw_output,
        # Pass 1
        "filter": {
            "flagged": filter_result.flagged,
            "summary": filter_result.summary_line(),
            "matches": [
                {
                    "category": m.category,
                    "severity_label": m.severity_label,
                    "note": m.note,
                    "matched_text": m.matched_text,
                }
                for m in filter_result.by_severity()
            ],
        },
        # Pass 2
        "enforcement": enforcement,
        "enforcement_error": enforcement_error,
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
    groq_ready = "yes" if os.environ.get("GROQ_API_KEY") else "NO — enforcement disabled"

    print(f"Brain interface starting on http://localhost:{port}")
    print(f"Model: {MISTRAL_MODEL}")
    print(f"Context files: {files_loaded} ({len(SYSTEM_CONTEXT)} chars)")
    print(f"Filter: {len(__import__('brain.filter', fromlist=['SIGNATURES']).SIGNATURES)} signatures loaded")
    print(f"Enforcement (Groq): {groq_ready}")

    app.run(host="0.0.0.0", port=port, debug=debug)
