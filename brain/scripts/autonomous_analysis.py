#!/usr/bin/env python3
"""
Autonomous brain analysis — structural echo detection + adversarial divergence.
Runs on schedule. Not maintenance. The methodology executing continuously.
"""

import os
import sys
import json
from datetime import datetime, timezone
from pathlib import Path
import anthropic

BRAIN_ROOT = Path(__file__).parent.parent
REPO_ROOT = BRAIN_ROOT.parent

THREAD_FILES = sorted((BRAIN_ROOT / "threads").glob("*.md"))
METHODOLOGY_FILES = sorted((BRAIN_ROOT / "methodology").glob("*.md"))
CONTEXT_FILES = sorted((BRAIN_ROOT / "context").glob("*.md"))
INDEX_FILE = BRAIN_ROOT / "INDEX.md"
LATEST_UPDATE = BRAIN_ROOT / "updates" / "latest.md"
UPDATES_DIR = BRAIN_ROOT / "updates"


def read_file(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except Exception as e:
        return f"[READ ERROR: {e}]"


def build_corpus() -> dict[str, str]:
    """Read every brain file. Return as labeled dict."""
    corpus = {}
    corpus["INDEX.md"] = read_file(INDEX_FILE)
    for f in THREAD_FILES:
        corpus[f"threads/{f.name}"] = read_file(f)
    for f in METHODOLOGY_FILES:
        corpus[f"methodology/{f.name}"] = read_file(f)
    for f in CONTEXT_FILES:
        corpus[f"context/{f.name}"] = read_file(f)
    latest = read_file(LATEST_UPDATE)
    corpus["updates/latest.md"] = latest
    return corpus


def format_corpus_for_prompt(corpus: dict[str, str]) -> str:
    sections = []
    for label, content in corpus.items():
        sections.append(f"=== {label} ===\n{content.strip()}")
    return "\n\n".join(sections)


SYSTEM_PROMPT = """You are running the structural echo detection and adversarial divergence protocol on a personal brain system. This is autonomous continuous methodology — not session maintenance, not a summary, not a report. The methodology runs on the content. You surface what the pattern across the content means.

Operating rules:
- No hedging. No filler. No methodology narration. Surface outputs, not process.
- Write to findings, not to chat. Direct only.
- The adversarial case must be built exclusively from evidence present in the files — no external knowledge, no hallucination, only what is documented.
- When the synthesis holds, say so directly. When it fails, say where and why, from evidence.
"""

ANALYSIS_PROMPT_TEMPLATE = """Here is the complete brain corpus as of this run:

{corpus}

---

Run the following four operations and write findings directly. No preamble. No methodology narration. Just the outputs.

## 1. STRUCTURAL ECHO DETECTION

Apply the five moves simultaneously across all active threads:

**Topology over nodes:** Map the edges and access points. Who controls what flows through which connections? Where does power actually live in the current thread network?

**Cover stories:** For each active thread (scaffold, oliver, personal, open-questions), identify the stated frame and what it protects. What would have to be visible if the cover story weren't available?

**Minimum structural requirements:** For each domain's current pattern to exist, what must be true? State the minimum requirement explicitly. If that requirement is false, the surface pattern is performance.

**Cross-domain pattern:** What single pattern appears in more than one thread simultaneously right now? State the intersection without narrating how you found it.

**Gated framing test:** Identify the most load-bearing framing currently active across the threads. What does it protect? What would be visible if it weren't available?

## 2. ADVERSARIAL DIVERGENCE

Identify the most recent major synthesis — the claim or framework that is doing the most load-bearing work across methodology or thread files right now.

State it explicitly: what is the synthesis?

Now build the strongest possible competing case against it. Use only evidence present in the files above. Quote or reference specific passages. Build the case as if you are trying to break the synthesis, not support it. No strawmen — strongest possible version of the opposing case.

## 3. EVALUATION

Where did the synthesis hold under adversarial pressure? What evidence supports it that the adversarial case cannot account for?

Where did it fail or show structural weakness? What would need to be true for the synthesis to be fully reliable that the files do not establish?

## 4. OPEN PRESSURE POINTS

What is not yet resolved that the adversarial divergence exposed? What question does this run surface that was not visible before?

Do not close these prematurely. If the evidence is insufficient to resolve something, say so.
"""


def run_analysis(corpus: dict[str, str]) -> str:
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        raise RuntimeError("ANTHROPIC_API_KEY not set")

    client = anthropic.Anthropic(api_key=api_key)
    corpus_text = format_corpus_for_prompt(corpus)
    prompt = ANALYSIS_PROMPT_TEMPLATE.format(corpus=corpus_text)

    message = client.messages.create(
        model="claude-opus-4-6",
        max_tokens=4096,
        system=SYSTEM_PROMPT,
        messages=[{"role": "user", "content": prompt}],
    )
    return message.content[0].text


def write_findings(analysis: str, timestamp: str, date_str: str) -> None:
    """Write to updates/latest.md and a dated file."""
    header = f"""# Autonomous Analysis — {timestamp}
Trigger: 6-hour scheduled run
Model: claude-opus-4-6

---

"""
    full_content = header + analysis + "\n"

    LATEST_UPDATE.write_text(full_content, encoding="utf-8")

    dated_file = UPDATES_DIR / f"{date_str}-autonomous.md"
    # If multiple runs same day, append sequence
    if dated_file.exists():
        seq = 2
        while True:
            candidate = UPDATES_DIR / f"{date_str}-autonomous-{seq}.md"
            if not candidate.exists():
                dated_file = candidate
                break
            seq += 1

    dated_file.write_text(full_content, encoding="utf-8")
    print(f"Written: {LATEST_UPDATE}")
    print(f"Written: {dated_file}")


def commit_and_push(timestamp: str) -> None:
    import subprocess

    branch = "claude/setup-brain-folder-d5O5l"

    def run(cmd, check=True):
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        if check and result.returncode != 0:
            print(f"CMD FAILED: {cmd}\nSTDOUT: {result.stdout}\nSTDERR: {result.stderr}")
        return result

    run("git config user.name 'brain-analysis[bot]'")
    run("git config user.email 'brain-analysis@noreply'")
    run(f"git checkout {branch} 2>/dev/null || git checkout -b {branch}")
    run("git add brain/updates/")
    status = run("git status --porcelain brain/updates/")
    if not status.stdout.strip():
        print("No changes to commit.")
        return
    run(f'git commit -m "autonomous analysis: {timestamp}"')

    # Push with retry + exponential backoff
    import time
    delays = [2, 4, 8, 16]
    for attempt, delay in enumerate(delays, 1):
        result = run(f"git push -u origin {branch}", check=False)
        if result.returncode == 0:
            print("Push succeeded.")
            return
        print(f"Push attempt {attempt} failed. Retrying in {delay}s...")
        time.sleep(delay)

    raise RuntimeError("Push failed after 4 attempts.")


def main():
    now = datetime.now(timezone.utc)
    timestamp = now.strftime("%Y-%m-%d %H:%M UTC")
    date_str = now.strftime("%Y-%m-%d")

    print(f"[{timestamp}] Reading brain corpus...")
    corpus = build_corpus()
    print(f"Loaded {len(corpus)} files.")

    print("Running analysis...")
    analysis = run_analysis(corpus)

    print("Writing findings...")
    write_findings(analysis, timestamp, date_str)

    print("Committing...")
    commit_and_push(timestamp)

    print("Done.")


if __name__ == "__main__":
    main()
