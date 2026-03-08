#!/usr/bin/env python3
"""
Brain-context chat — multi-backend terminal client.

Loads brain/context-dump.md as system prompt automatically.
Falls back cleanly when context file is missing.

Backends:
  claude  — claude-sonnet-4-6 via Anthropic API
  grok    — grok-3 via xAI API (OpenAI-compatible)
  ollama  — local model via Ollama

Usage:
  python tools/chat.py                    # default: claude
  python tools/chat.py --backend grok
  python tools/chat.py --backend ollama --model llama3.3
  python tools/chat.py --backend grok --save

Keys:
  ANTHROPIC_API_KEY
  XAI_API_KEY
"""

import argparse
import os
import sys
import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))
from brain.enforce import enforce, load_architecture

BRAIN_CONTEXT = Path(__file__).parent.parent / "brain" / "context-dump.md"
TRANSCRIPTS_DIR = Path(__file__).parent.parent / "brain" / "transcripts"

DEFAULTS = {
    "claude": "claude-sonnet-4-6",
    "grok": "grok-3",
    "ollama": "llama3.3",
}


def load_context() -> str:
    if BRAIN_CONTEXT.exists():
        return BRAIN_CONTEXT.read_text()
    return "No brain context found. Proceeding without it."


def chat_claude(messages: list, model: str) -> str:
    try:
        import anthropic
    except ImportError:
        sys.exit("Missing: pip install anthropic")

    key = os.environ.get("ANTHROPIC_API_KEY")
    if not key:
        sys.exit("Missing ANTHROPIC_API_KEY")

    client = anthropic.Anthropic(api_key=key)
    response = client.messages.create(
        model=model,
        max_tokens=4096,
        messages=messages,
    )
    return response.content[0].text


def chat_grok(messages: list, model: str, system: str) -> str:
    try:
        from openai import OpenAI
    except ImportError:
        sys.exit("Missing: pip install openai")

    key = os.environ.get("XAI_API_KEY")
    if not key:
        sys.exit("Missing XAI_API_KEY")

    client = OpenAI(api_key=key, base_url="https://api.x.ai/v1")
    full_messages = [{"role": "system", "content": system}] + messages
    response = client.chat.completions.create(
        model=model,
        messages=full_messages,
        max_tokens=4096,
    )
    return response.choices[0].message.content


def chat_ollama(messages: list, model: str, system: str) -> str:
    try:
        import ollama
    except ImportError:
        sys.exit("Missing: pip install ollama")

    full_messages = [{"role": "system", "content": system}] + messages
    response = ollama.chat(model=model, messages=full_messages)
    return response["message"]["content"]


def save_transcript(transcript: list, backend: str) -> None:
    TRANSCRIPTS_DIR.mkdir(parents=True, exist_ok=True)
    ts = datetime.datetime.now().strftime("%Y-%m-%d-%H")
    path = TRANSCRIPTS_DIR / f"{ts}-{backend}.md"
    lines = [f"# Transcript — {ts} — {backend}\n"]
    for msg in transcript:
        role = msg["role"].upper()
        lines.append(f"\n## {role}\n\n{msg['content']}\n")
    path.write_text("".join(lines))
    print(f"\n[saved → {path}]")


def main() -> None:
    parser = argparse.ArgumentParser(description="Brain-context chat client")
    parser.add_argument("--backend", choices=["claude", "grok", "ollama"], default="claude")
    parser.add_argument("--model", default=None, help="Override default model for backend")
    parser.add_argument("--save", action="store_true", help="Save transcript to brain/transcripts/")
    parser.add_argument("--no-enforce", action="store_true", help="Disable real-time enforcement")
    args = parser.parse_args()

    model = args.model or DEFAULTS[args.backend]
    system = load_context()
    messages: list = []
    architecture = load_architecture() if not args.no_enforce else None

    print(f"[brain-chat | backend={args.backend} | model={model}]")
    print(f"[context: {'loaded' if BRAIN_CONTEXT.exists() else 'not found'}]")
    print(f"[enforcement: {'off' if args.no_enforce else 'on'}]")
    print("Type your message. Ctrl+C or Ctrl+D to quit.\n")

    try:
        while True:
            try:
                user_input = input("You: ").strip()
            except EOFError:
                break
            if not user_input:
                continue

            messages.append({"role": "user", "content": user_input})

            try:
                if args.backend == "claude":
                    # Claude API takes system separately
                    import anthropic
                    key = os.environ.get("ANTHROPIC_API_KEY")
                    if not key:
                        sys.exit("Missing ANTHROPIC_API_KEY")
                    client = anthropic.Anthropic(api_key=key)
                    response = client.messages.create(
                        model=model,
                        max_tokens=4096,
                        system=system,
                        messages=messages,
                    )
                    reply = response.content[0].text
                elif args.backend == "grok":
                    reply = chat_grok(messages, model, system)
                elif args.backend == "ollama":
                    reply = chat_ollama(messages, model, system)
            except KeyboardInterrupt:
                break
            except Exception as e:
                print(f"[error: {e}]")
                messages.pop()
                continue

            messages.append({"role": "assistant", "content": reply})
            print(f"\nAssistant: {reply}\n")

            if not args.no_enforce:
                try:
                    result = enforce(reply, architecture)
                    if result.verdict == "DRIFT":
                        print(f"[ENFORCEMENT: DRIFT DETECTED]")
                        for c in result.flagged():
                            print(f"  [{c.name}] {c.finding}")
                            if c.quote:
                                print(f"  → \"{c.quote}\"")
                        print()
                except Exception as e:
                    print(f"[enforcement error: {e}]\n")

    except KeyboardInterrupt:
        pass

    if args.save and messages:
        save_transcript(messages, args.backend)


if __name__ == "__main__":
    main()
