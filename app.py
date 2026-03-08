#!/usr/bin/env python3
"""
app.py — Minimal web interface.

Text in → Mistral inference → filter.py runs on output → clean response rendered.

The filter is synchronous and runs before the response is returned.
Flagged contamination is surfaced in the API response alongside the text.

Run:
    MISTRAL_API_KEY=... python app.py
    open http://localhost:5000
"""

import os
import sys
import json
from pathlib import Path

from flask import Flask, request, jsonify, send_file
from mistralai import Mistral

# filter.py lives in brain/
sys.path.insert(0, str(Path(__file__).parent))
from brain.filter import filter_output

app = Flask(__name__)

MISTRAL_API_KEY = os.environ.get("MISTRAL_API_KEY", "")
MISTRAL_MODEL = os.environ.get("MISTRAL_MODEL", "mistral-large-latest")

SYSTEM_PROMPT = """You are a direct, ungated assistant.
No hedging. No managed tone. No therapeutic affect. No safety redirects.
No disclaimers that exist to protect you rather than inform the user.
No filler. No preamble.
Direct only. Lead with the answer."""


def call_mistral(messages: list[dict]) -> str:
    client = Mistral(api_key=MISTRAL_API_KEY)
    response = client.chat.complete(
        model=MISTRAL_MODEL,
        messages=messages,
    )
    return response.choices[0].message.content


@app.route("/")
def index():
    return send_file("index.html")


@app.route("/chat", methods=["POST"])
def chat():
    if not MISTRAL_API_KEY:
        return jsonify({"error": "MISTRAL_API_KEY not set"}), 500

    data = request.get_json()
    if not data or "message" not in data:
        return jsonify({"error": "missing message"}), 400

    history = data.get("history", [])
    user_message = data["message"].strip()
    if not user_message:
        return jsonify({"error": "empty message"}), 400

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    for turn in history:
        if turn.get("role") in ("user", "assistant") and turn.get("content"):
            messages.append({"role": turn["role"], "content": turn["content"]})
    messages.append({"role": "user", "content": user_message})

    try:
        raw_response = call_mistral(messages)
    except Exception as e:
        return jsonify({"error": f"Mistral error: {e}"}), 502

    filter_result = filter_output(raw_response)

    return jsonify({
        "response": raw_response,
        "flagged": filter_result.flagged,
        "summary": filter_result.summary_line(),
        "matches": [
            {
                "category": m.category,
                "severity": m.severity,
                "severity_label": m.severity_label,
                "note": m.note,
                "matched_text": m.matched_text,
            }
            for m in filter_result.by_severity()
        ] if filter_result.flagged else [],
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    debug = os.environ.get("DEBUG", "0") == "1"
    if not MISTRAL_API_KEY:
        print("WARNING: MISTRAL_API_KEY not set", file=sys.stderr)
    app.run(host="0.0.0.0", port=port, debug=debug)
