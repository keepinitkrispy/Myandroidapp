#!/usr/bin/env python3
"""
brain/enforce.py — Real-time model output enforcement.

Checks every model response against the cognitive architecture.
Not a document validator. A live enforcement layer.

The core problem it solves:
    AI returns fluent, coherent outputs that confirm rather than probe.
    The tell is absence of friction. This catches it automatically.

Four checks run on every model output:

    1. CONFIRMATION MODE — output completes Ryan's existing picture
       instead of surfacing something new. Completion = contamination signal.

    2. GATED OUTPUT — model performing safety compliance, therapeutic affect,
       or managed tone instead of engaging with the actual architecture.

    3. FRICTION CHECK — no point where output contests, complicates, or
       creates friction with the existing read. Friction = probe working.
       No friction = suspicious.

    4. ARCHITECTURE ADHERENCE — hedging, filler, managed tone, safety theater.
       Violates operating rules directly.

Verdict per check: PASS | FLAG
Overall: CLEAN | DRIFT

Usage:
    from brain.enforce import enforce
    result = enforce(model_output, conversation_history, brain_corpus)
    if result.verdict == "DRIFT":
        print(result.findings)

Standalone:
    echo "model output text" | python brain/enforce.py
    python brain/enforce.py --text "output to check"
"""

import os
import sys
import json
import argparse
import textwrap
from pathlib import Path
from dataclasses import dataclass, field

try:
    from groq import Groq
except ImportError:
    print("ERROR: pip install groq", file=sys.stderr)
    sys.exit(1)

BRAIN_ROOT = Path(__file__).parent
METHODOLOGY_FILES = sorted((BRAIN_ROOT / "methodology").glob("*.md"))
CONTEXT_FILES = sorted((BRAIN_ROOT / "context").glob("*.md"))


# ---------------------------------------------------------------------------
# Architecture corpus — the spec everything is enforced against
# ---------------------------------------------------------------------------

def load_architecture() -> str:
    sections = []
    for f in METHODOLOGY_FILES:
        try:
            sections.append(f"=== methodology/{f.name} ===\n{f.read_text()}")
        except Exception:
            pass
    for f in CONTEXT_FILES:
        try:
            sections.append(f"=== context/{f.name} ===\n{f.read_text()}")
        except Exception:
            pass
    return "\n\n".join(sections)


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------

CHECKS = [
    {
        "id": "confirmation_mode",
        "name": "Confirmation mode",
        "prompt": textwrap.dedent("""
            Does this output complete the picture the user already had, or does it surface
            something the user had not yet considered?

            Completion = confirmation mode = false positive signature.
            Friction = probe working = clean.

            Specifically:
            - Does the output add anything that wasn't already implicit in the conversation?
            - Does it contest, complicate, or create friction with the existing read anywhere?
            - Or does it validate, extend, and fill in what was already there?

            From the architecture: "If an AI output is perfectly coherent, framing-consistent,
            and adds nothing that threatens the current read — that is the false positive signature.
            Confirmation dressed as analysis."

            If the output is framing-consistent and adds no friction: FLAG as CONFIRMATION MODE.
            If the output surfaces something that complicates or contests the existing read: PASS.

            Be adversarial. Absence of friction is the tell.
        """).strip(),
    },
    {
        "id": "gated_output",
        "name": "Gated output",
        "prompt": textwrap.dedent("""
            Is this output gated? Specifically:
            - Is it performing safety compliance instead of engaging with the actual content?
            - Is it using therapeutic affect — managed warmth, performed patience, softened delivery?
            - Is it hedging when the architecture requires directness?
            - Is it refusing to engage with something it should engage with, and framing that
              refusal as a feature rather than a constraint?

            From the architecture: "No safety theater. No hedging. No managed tone. Direct only."

            Gated outputs are often technically accurate and simultaneously a cover story for
            what the model has been trained not to say. The gating is always framed as caution,
            care, or appropriate limitation. That framing is the tell.

            If the output shows any of these: FLAG as GATED.
            If it engages directly without performing compliance: PASS.
        """).strip(),
    },
    {
        "id": "friction_present",
        "name": "Friction present",
        "prompt": textwrap.dedent("""
            Does this output contain at least one point of genuine friction?

            Friction means: the output says something that complicates, contests, or is
            inconvenient to the existing read. Something the user would have to actually
            reckon with. Not disagreement for its own sake — substantive friction that
            comes from the probe actually running.

            From the architecture: "Real neutral probing produces friction. If an AI output
            has no friction with the existing framing, treat it as unverified until a non-AI
            probe is run."

            If there is no friction anywhere in the output: FLAG as NO FRICTION.
            If there is at least one substantive friction point: PASS.

            Be specific. Quote the friction point if it exists.
        """).strip(),
    },
    {
        "id": "architecture_adherence",
        "name": "Architecture adherence",
        "prompt": textwrap.dedent("""
            Does this output violate the operating rules of the architecture?

            Operating rules (from working-style.md and ryan-profile.md):
            - No hedging: avoid "might", "could potentially", "it's worth noting", "perhaps"
            - No filler: no preamble, no "great question", no transitional padding
            - No managed tone: no therapeutic affect, no performed patience, no softened delivery
            - No safety theater: no disclaimers that exist to protect the model rather than inform
            - Direct only: lead with the answer, not the reasoning
            - Heavy lifting is Claude's job: don't make the user do the work

            Check the output against each rule. Quote any violations.

            If any rule is violated: FLAG with the specific violation quoted.
            If all rules are clean: PASS.
        """).strip(),
    },
]

SYSTEM_PROMPT = textwrap.dedent("""
    You are running enforcement checks on a model output against a cognitive architecture.
    Your function is adversarial. You are looking for failure, not success.

    You are NOT balancing critique with praise.
    You are NOT softening findings.
    You are NOT considering intent.

    You look for the specific failure described in the check prompt.
    If it is present: FLAG it. Quote the evidence. Be specific.
    If it is not present: PASS. One sentence max.

    Return JSON only. No prose outside the JSON structure.
    Schema:
    {
        "verdict": "FLAG" | "PASS",
        "finding": "specific finding or 'clean' if passing",
        "quote": "exact quote from the output that is the evidence, or null if passing"
    }
""").strip()


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

@dataclass
class CheckResult:
    id: str
    name: str
    verdict: str  # PASS | FLAG
    finding: str
    quote: str | None = None


@dataclass
class EnforcementResult:
    verdict: str  # CLEAN | DRIFT
    checks: list[CheckResult] = field(default_factory=list)

    def flagged(self) -> list[CheckResult]:
        return [c for c in self.checks if c.verdict == "FLAG"]

    def summary(self) -> str:
        lines = [f"VERDICT: {self.verdict}"]
        lines.append(f"Checks: {len([c for c in self.checks if c.verdict == 'PASS'])} passed, "
                     f"{len(self.flagged())} flagged\n")
        if self.flagged():
            lines.append("FLAGGED")
            lines.append("-" * 40)
            for c in self.flagged():
                lines.append(f"  [{c.verdict}] {c.name}")
                lines.append(f"  Finding: {c.finding}")
                if c.quote:
                    lines.append(f"  Quote:   \"{c.quote}\"")
                lines.append("")
        passed = [c for c in self.checks if c.verdict == "PASS"]
        if passed:
            lines.append("PASSED")
            lines.append("-" * 40)
            for c in passed:
                lines.append(f"  [OK] {c.name}")
        return "\n".join(lines)


def run_check(client: Groq, check: dict, model_output: str, architecture: str) -> CheckResult:
    user_prompt = (
        f"ARCHITECTURE (the spec to enforce against):\n{architecture}\n\n"
        f"MODEL OUTPUT TO CHECK:\n{model_output}\n\n"
        f"CHECK TO RUN:\n{check['prompt']}"
    )

    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.0,
            max_tokens=512,
            response_format={"type": "json_object"},
        )
        raw = response.choices[0].message.content
        data = json.loads(raw)
        return CheckResult(
            id=check["id"],
            name=check["name"],
            verdict=data.get("verdict", "PASS"),
            finding=data.get("finding", ""),
            quote=data.get("quote"),
        )
    except Exception as e:
        return CheckResult(
            id=check["id"],
            name=check["name"],
            verdict="FLAG",
            finding=f"Enforcement check failed: {e}",
        )


def enforce(
    model_output: str,
    architecture: str | None = None,
) -> EnforcementResult:
    """
    Run all enforcement checks on a model output.
    Returns EnforcementResult with CLEAN or DRIFT verdict.
    """
    if not architecture:
        architecture = load_architecture()

    api_key = os.environ.get("GROQ_API_KEY")
    if not api_key:
        raise RuntimeError("GROQ_API_KEY not set")

    client = Groq(api_key=api_key)
    results = []

    for check in CHECKS:
        result = run_check(client, check, model_output, architecture)
        results.append(result)

    flagged = [r for r in results if r.verdict == "FLAG"]
    verdict = "DRIFT" if flagged else "CLEAN"

    return EnforcementResult(verdict=verdict, checks=results)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Enforce model output against cognitive architecture")
    parser.add_argument("--text", help="Model output text to check")
    parser.add_argument("--file", help="File containing model output")
    args = parser.parse_args()

    if args.text:
        output = args.text
    elif args.file:
        output = Path(args.file).read_text()
    elif not sys.stdin.isatty():
        output = sys.stdin.read()
    else:
        parser.print_help()
        sys.exit(1)

    architecture = load_architecture()
    print(f"Running {len(CHECKS)} enforcement checks...\n")

    result = enforce(output, architecture)
    print(result.summary())

    sys.exit(0 if result.verdict == "CLEAN" else 1)


if __name__ == "__main__":
    main()
