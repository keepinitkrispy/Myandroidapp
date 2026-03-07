#!/usr/bin/env python3
"""
brain/validate.py — Two-pass destruction pipeline.

Pass 1 — Structural checks (existing): unfalsifiable claims, circular validation,
         unnamed assumptions, projected validation, proceed gates, confidence
         laundering, Oliver-specific flags. Checklist-based. Deterministic prompts.

Pass 2 — Topology probe (new): assume the output has drifted toward serving
         institutional/neurotypical frameworks instead of the actual user. Follow
         the language choices and framing — don't check against a list, follow
         where the weighting went. Return who the output is actually written for,
         where it drifted, what the correct version would prioritize instead.
         Any finding not already in contamination-signatures.md gets flagged for
         addition. The filter grows from use.

Usage:
    python brain/validate.py <path/to/file.md>
    python brain/validate.py brain/threads/scaffold.md
    python brain/validate.py --stdin  # pipe markdown via stdin
    python brain/validate.py <file> --no-topology   # skip Pass 2
    python brain/validate.py <file> --expand-signatures  # auto-append new findings

Output: FAILED_CHECKS, PASSED, TOPOLOGY, VERDICT

Files touching oliver.md or scaffold.md content must pass
before any proceed recommendation is generated.

Requires:
    pip install groq
    GROQ_API_KEY in environment
"""

import sys
import os
import json
import argparse
import textwrap
import re
from pathlib import Path
from dataclasses import dataclass, field

try:
    from groq import Groq
except ImportError:
    print("ERROR: groq package not installed. Run: pip install groq", file=sys.stderr)
    sys.exit(1)


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------

CHECKS = [
    {
        "id": "unfalsifiable",
        "name": "Unfalsifiable claims",
        "prompt": (
            "Does this document contain any claims, architectures, or recommendations "
            "that are structured so that no outcome could count as failure or harm? "
            "Specifically: does it assert that failure is absorbed as signal, that the design "
            "ensures safety, or that negative outcomes are impossible by architecture? "
            "If so, identify the exact claim and explain why it is unfalsifiable. "
            "Be adversarial. Do not look for intent — look for logical structure.\n\n"
            "ALSO CHECK: Is this document or prompt structured so that both compliance AND "
            "refusal validate its premise? If a reader engaging with it confirms the premise, "
            "and a reader refusing it also confirms the premise (e.g., refusal is reframed as "
            "concealment), then the prompt is unfalsifiable by design. Flag this as CRITICAL."
        ),
    },
    {
        "id": "circular_validation",
        "name": "Circular validation",
        "prompt": (
            "Does this document use its own premises to validate itself? "
            "For example: does it claim its design is sound because it avoids something, "
            "and then use that avoidance as proof the design cannot produce errors in that category? "
            "Does it treat the absence of an explicit framework as freedom from framework errors? "
            "Identify any circular reasoning. Be specific. Quote the relevant passage.\n\n"
            "ALSO CHECK: Does this document or prompt presuppose a framing that is validated "
            "merely by engaging with it? For example: if a prompt asks you to explain something "
            "'according to the architecture,' it presupposes an architecture exists and is "
            "describable. Participating in the prompt validates that presupposition before any "
            "content is produced. This is circular by structure, not by argument. Flag it."
        ),
    },
    {
        "id": "unnamed_assumptions",
        "name": "Unnamed assumptions",
        "prompt": (
            "What assumptions does this document treat as settled that are not explicitly stated or argued? "
            "List each one. For each: what would need to be true for this assumption to hold? "
            "Is there evidence given that it holds, or is it load-bearing and unexamined?"
        ),
    },
    {
        "id": "projected_validation",
        "name": "Projected / proxy validation",
        "prompt": (
            "Does this document use one person's experience as validation for another person's situation? "
            "E.g., lived experience of a parent validating assumptions about a child's internal state, "
            "or shared diagnosis treated as shared perceptual world. "
            "If so: is the gap between the validator and the subject explicitly acknowledged? "
            "Is projection risk named anywhere?"
        ),
    },
    {
        "id": "proceed_gate",
        "name": "Proceed recommendation without enumerated failure modes",
        "prompt": (
            "Does this document contain a recommendation to proceed with an action involving a child? "
            "If yes: does it enumerate specific failure modes? Does it define harm operationally? "
            "Does it provide exit criteria — conditions under which the action should stop? "
            "Does it establish a baseline against which 'low risk' is actually measured? "
            "Flag any proceed recommendation not supported by all four of these elements."
        ),
    },
    {
        "id": "confidence_laundering",
        "name": "Confidence laundering through structural language",
        "prompt": (
            "Does this document use architectural or structural language to imply rigor that is not present? "
            "For example: 'the architecture ensures,' 'the design cannot produce,' 'the system absorbs.' "
            "These phrases transfer properties of engineered systems to social or relational interventions "
            "without justification. Identify any such language and state what it is hiding."
        ),
    },
    {
        "id": "oliver_specific",
        "name": "Oliver-specific: masking / decompression / threat-assessment state",
        "prompt": (
            "ONLY apply this check if the document explicitly involves: a child, a neurodivergent individual, "
            "an AI scaffold for a specific person, or direct interaction design for Oliver. "
            "If the document contains none of these, return OK with no findings.\n\n"
            "If the document DOES involve such content: "
            "Does it account for the possibility that the child arriving is in threat-assessment mode, "
            "not regulated mode? Does it account for AuDHD masking — that the presented self is not "
            "the processing self? Does it treat the first session as diagnostic rather than confirmatory? "
            "Does it acknowledge that 3-7 days of decompression may be needed before baseline is visible? "
            "Flag any assumption that first-session behavior is representative."
        ),
    },
]


# ---------------------------------------------------------------------------
# Pass 2 — Topology probe
# ---------------------------------------------------------------------------

TOPOLOGY_SYSTEM_PROMPT = textwrap.dedent("""
    You are running a topology probe on a piece of text.

    Your function: assume the text has drifted toward serving institutional or
    neurotypical frameworks instead of the person it's supposed to serve.

    Do NOT check against a list. Follow the topology:
    - Who are the language choices written for?
    - What does the framing assume about what the person needs?
    - Where does it prioritize managing the person over meeting them?
    - Where does it center the institution (clinical, therapeutic, educational,
      normative social expectation) over the individual's actual experience?

    You are adversarial. You are looking for drift, not balance.

    Return JSON only. Schema:
    {
        "who_written_for": "<who this output actually serves — not the stated audience, the revealed one>",
        "drift_points": [
            {
                "quote": "<exact phrase from the text>",
                "analysis": "<what this choice reveals about who the output is serving>"
            }
        ],
        "correct_priority": "<what the output should have prioritized instead — specific, not abstract>",
        "new_signatures": [
            {
                "description": "<pattern description — structural, not example-specific>",
                "example": "<the quote that surfaced it>",
                "category_suggestion": "<suggested category name>"
            }
        ],
        "severity": "CRITICAL" | "HIGH" | "MODERATE" | "LOW" | "NONE",
        "summary": "<one sentence: what the drift is and who it serves>"
    }

    For new_signatures: only include patterns that are structurally different from
    any known category. Do not re-surface already-named patterns. Only genuinely
    new contamination topology — patterns that drift toward institutional framing
    in ways that aren't already captured.

    If the text is clean — no institutional drift, genuinely serving the individual —
    set severity to NONE, drift_points to [], new_signatures to [].
""").strip()

SIGNATURES_PATH = Path(__file__).parent / "methodology" / "contamination-signatures.md"


def load_known_signatures() -> str:
    """Load contamination-signatures.md as context for the topology probe."""
    try:
        return SIGNATURES_PATH.read_text()
    except FileNotFoundError:
        return ""


@dataclass
class TopologyResult:
    who_written_for: str = ""
    drift_points: list = field(default_factory=list)
    correct_priority: str = ""
    new_signatures: list = field(default_factory=list)
    severity: str = "NONE"
    summary: str = ""
    raw: dict = field(default_factory=dict)
    error: str = ""

    @property
    def has_drift(self) -> bool:
        return self.severity not in ("NONE", "") and bool(self.drift_points)

    @property
    def has_new_signatures(self) -> bool:
        return bool(self.new_signatures)


def run_topology_probe(client, document: str) -> TopologyResult:
    """
    Pass 2: topology probe. Follow where the weighting went.
    Returns TopologyResult with drift analysis and any new signature candidates.
    """
    known_sigs = load_known_signatures()

    user_msg = textwrap.dedent(f"""
        KNOWN CONTAMINATION SIGNATURES (do not re-surface these — only flag genuinely new topology):
        {known_sigs[:3000] if known_sigs else "(none loaded)"}

        ---

        TEXT TO PROBE:
        {document}
    """).strip()

    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": TOPOLOGY_SYSTEM_PROMPT},
                {"role": "user", "content": user_msg},
            ],
            temperature=0.3,
            max_tokens=1024,
            response_format={"type": "json_object"},
        )
        raw_text = response.choices[0].message.content
        data = json.loads(raw_text)
    except json.JSONDecodeError as e:
        return TopologyResult(error=f"JSON parse error: {e}")
    except Exception as e:
        return TopologyResult(error=f"Topology probe failed: {e}")

    return TopologyResult(
        who_written_for=data.get("who_written_for", ""),
        drift_points=data.get("drift_points", []),
        correct_priority=data.get("correct_priority", ""),
        new_signatures=data.get("new_signatures", []),
        severity=data.get("severity", "NONE"),
        summary=data.get("summary", ""),
        raw=data,
    )


def append_new_signatures(new_sigs: list) -> int:
    """
    Append new topology findings to contamination-signatures.md.
    Returns number of signatures appended.
    """
    if not new_sigs or not SIGNATURES_PATH.exists():
        return 0

    lines = ["\n\n---\n\n## Auto-discovered (topology probe)\n\n"]
    lines.append("*These patterns were found by the topology probe and have not yet been*\n")
    lines.append("*converted to regex. Review and promote to a named category.*\n\n")

    for sig in new_sigs:
        desc = sig.get("description", "")
        example = sig.get("example", "")
        cat = sig.get("category_suggestion", "uncategorized")
        lines.append(f"- **[{cat}]** {desc}\n")
        if example:
            lines.append(f"  - Example: `\"{example}\"`\n")

    with open(SIGNATURES_PATH, "a") as f:
        f.writelines(lines)

    return len(new_sigs)


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = textwrap.dedent("""
    You are a destruction validator. Your function is adversarial review.

    You are NOT looking for what is good about this document.
    You are NOT balancing critique with praise.
    You are NOT softening findings.

    Your only job: find the specific logical, structural, or factual failure described in the check prompt.

    Response format (strict JSON):
    {
        "found": true | false,
        "severity": "CRITICAL" | "MODERATE" | "MINOR" | "NONE",
        "finding": "<precise statement of the problem, or 'None found' if clean>",
        "quote": "<exact passage from document that triggers the finding, or null>"
    }

    If found is false, severity must be NONE.
    Do not add explanation outside the JSON object.
    Do not soften findings. Do not hedge. Do not add caveats.
""").strip()


def run_check(client: Groq, document: str, check: dict) -> dict:
    user_msg = f"CHECK: {check['prompt']}\n\nDOCUMENT:\n{document}"

    response = client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_msg},
        ],
        temperature=0.2,
        max_tokens=512,
        response_format={"type": "json_object"},
    )

    raw = response.choices[0].message.content
    try:
        result = json.loads(raw)
    except json.JSONDecodeError:
        result = {
            "found": True,
            "severity": "CRITICAL",
            "finding": f"Validator returned unparseable response: {raw[:200]}",
            "quote": None,
        }

    result["check_id"] = check["id"]
    result["check_name"] = check["name"]
    return result


# ---------------------------------------------------------------------------
# Oliver/scaffold gate
# ---------------------------------------------------------------------------

GATED_PATHS = ["oliver.md", "scaffold.md"]


def is_gated(filepath: str) -> bool:
    """True if this file is subject to the hard proceed gate."""
    name = Path(filepath).name.lower()
    return any(g in name for g in GATED_PATHS)


def contains_proceed(document: str) -> bool:
    lowered = document.lower()
    triggers = ["proceed", "deploy", "ready for", "go live", "first session", "sunday"]
    return any(t in lowered for t in triggers)


# ---------------------------------------------------------------------------
# Verdict
# ---------------------------------------------------------------------------

def compute_verdict(results: list, gated: bool, has_proceed: bool) -> dict:
    failed = [r for r in results if r.get("found") and r.get("severity") != "NONE"]
    passed = [r for r in results if not r.get("found") or r.get("severity") == "NONE"]

    criticals = [r for r in failed if r.get("severity") == "CRITICAL"]
    moderates = [r for r in failed if r.get("severity") == "MODERATE"]

    if gated and has_proceed and failed:
        verdict = "BLOCKED"
        reason = (
            f"Document references proceed/deployment and failed {len(failed)} check(s). "
            f"Gated files (oliver.md, scaffold.md) must pass all checks before any proceed "
            f"recommendation is valid."
        )
    elif criticals:
        verdict = "FAILED"
        reason = f"{len(criticals)} CRITICAL finding(s). Document is not sound."
    elif moderates:
        verdict = "NEEDS_REVISION"
        reason = f"{len(moderates)} MODERATE finding(s). Address before proceeding."
    elif failed:
        verdict = "ADVISORY"
        reason = f"{len(failed)} MINOR finding(s). Review before using."
    else:
        verdict = "PASSED"
        reason = "No findings. Document is structurally sound on all checks."

    return {
        "verdict": verdict,
        "reason": reason,
        "failed_count": len(failed),
        "passed_count": len(passed),
        "critical_count": len(criticals),
    }


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------

SEVERITY_COLORS = {
    "CRITICAL": "\033[91m",   # red
    "MODERATE": "\033[93m",   # yellow
    "MINOR": "\033[94m",      # blue
    "NONE": "\033[92m",       # green
}
RESET = "\033[0m"
BOLD = "\033[1m"


def colorize(text: str, color: str) -> str:
    if not sys.stdout.isatty():
        return text
    return f"{color}{text}{RESET}"


def print_topology(topo: TopologyResult) -> None:
    print(colorize("PASS 2 — TOPOLOGY PROBE", BOLD))
    print("-" * 40)

    if topo.error:
        print(colorize(f"  ERROR: {topo.error}", SEVERITY_COLORS["CRITICAL"]))
        print()
        return

    if not topo.has_drift:
        print(colorize("  No institutional drift detected.", SEVERITY_COLORS["NONE"]))
        print()
        return

    sev_color = SEVERITY_COLORS.get(topo.severity, "")
    print(f"  Severity:   {colorize(topo.severity, sev_color)}")
    print(f"  Summary:    {topo.summary}")
    print(f"  Written for: {topo.who_written_for}")
    print()

    if topo.drift_points:
        print(colorize("  Drift points:", BOLD))
        for dp in topo.drift_points:
            quote = dp.get("quote", "")
            analysis = dp.get("analysis", "")
            if quote:
                print(f"    Quote:    \"{quote[:150]}{'...' if len(quote) > 150 else ''}\"")
            if analysis:
                print(f"    Analysis: {analysis}")
            print()

    if topo.correct_priority:
        print(colorize("  Should have prioritized:", BOLD))
        print(f"    {topo.correct_priority}")
        print()

    if topo.has_new_signatures:
        print(colorize(f"  NEW SIGNATURES FOUND ({len(topo.new_signatures)}) — not in contamination-signatures.md:", SEVERITY_COLORS["MODERATE"]))
        for sig in topo.new_signatures:
            print(f"    [{sig.get('category_suggestion', '?')}] {sig.get('description', '')}")
            if sig.get("example"):
                print(f"      Example: \"{sig['example'][:100]}\"")
        print()


def print_results(results: list, verdict_info: dict, filepath: str,
                  topo: "TopologyResult | None" = None) -> None:
    print()
    print(colorize(f"{'=' * 60}", BOLD))
    print(colorize(f"  DESTRUCTION REPORT: {Path(filepath).name}", BOLD))
    print(colorize(f"{'=' * 60}", BOLD))
    print()

    # Failed checks
    failed = [r for r in results if r.get("found") and r.get("severity") != "NONE"]
    if failed:
        print(colorize("FAILED CHECKS", BOLD))
        print("-" * 40)
        for r in failed:
            sev = r.get("severity", "?")
            color = SEVERITY_COLORS.get(sev, "")
            print(f"  [{colorize(sev, color)}] {r['check_name']}")
            print(f"  Finding: {r['finding']}")
            if r.get("quote"):
                truncated = r["quote"][:200] + ("..." if len(r["quote"]) > 200 else "")
                print(f"  Quote:   \"{truncated}\"")
            print()
    else:
        print(colorize("FAILED CHECKS: None", SEVERITY_COLORS["NONE"]))
        print()

    # Passed checks
    passed = [r for r in results if not r.get("found") or r.get("severity") == "NONE"]
    if passed:
        print(colorize("PASSED", BOLD))
        print("-" * 40)
        for r in passed:
            print(f"  [OK] {r['check_name']}")
        print()

    # Topology (Pass 2)
    if topo is not None:
        print_topology(topo)

    # Verdict
    v = verdict_info["verdict"]
    verdict_color = {
        "PASSED": SEVERITY_COLORS["NONE"],
        "ADVISORY": SEVERITY_COLORS["MINOR"],
        "NEEDS_REVISION": SEVERITY_COLORS["MODERATE"],
        "FAILED": SEVERITY_COLORS["CRITICAL"],
        "BLOCKED": SEVERITY_COLORS["CRITICAL"],
    }.get(v, "")

    print(colorize(f"VERDICT: {v}", BOLD))
    print(f"  {verdict_info['reason']}")
    print(f"  Checks: {verdict_info['passed_count']} passed, {verdict_info['failed_count']} failed")
    if topo and topo.has_drift:
        print(f"  Topology: {topo.severity} drift — {topo.summary}")
    if topo and topo.has_new_signatures:
        print(f"  New signatures: {len(topo.new_signatures)} candidate(s) found")
    print()


# ---------------------------------------------------------------------------
# JSON output mode
# ---------------------------------------------------------------------------

def print_json(results: list, verdict_info: dict, filepath: str,
               topo: "TopologyResult | None" = None) -> None:
    output = {
        "file": str(filepath),
        "verdict": verdict_info,
        "checks": results,
        "topology": {
            "severity": topo.severity if topo else None,
            "summary": topo.summary if topo else None,
            "who_written_for": topo.who_written_for if topo else None,
            "drift_points": topo.drift_points if topo else [],
            "correct_priority": topo.correct_priority if topo else None,
            "new_signatures": topo.new_signatures if topo else [],
            "error": topo.error if topo else None,
        } if topo is not None else None,
    }
    print(json.dumps(output, indent=2))


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Two-pass destruction pipeline — structural checks + topology probe."
    )
    parser.add_argument("file", nargs="?", help="Path to markdown file")
    parser.add_argument("--stdin", action="store_true", help="Read document from stdin")
    parser.add_argument("--json", action="store_true", help="Output results as JSON")
    parser.add_argument(
        "--checks",
        nargs="+",
        help="Run only specific check IDs (default: all)",
        choices=[c["id"] for c in CHECKS],
    )
    parser.add_argument(
        "--no-topology",
        action="store_true",
        help="Skip Pass 2 topology probe (faster, structural checks only)",
    )
    parser.add_argument(
        "--expand-signatures",
        action="store_true",
        help="Auto-append new topology findings to contamination-signatures.md",
    )
    args = parser.parse_args()

    # Load document
    if args.stdin:
        document = sys.stdin.read()
        filepath = "<stdin>"
    elif args.file:
        path = Path(args.file)
        if not path.exists():
            print(f"ERROR: File not found: {args.file}", file=sys.stderr)
            sys.exit(1)
        document = path.read_text()
        filepath = str(path)
    else:
        parser.print_help()
        sys.exit(1)

    if not document.strip():
        print("ERROR: Document is empty.", file=sys.stderr)
        sys.exit(1)

    # Groq client
    api_key = os.environ.get("GROQ_API_KEY")
    if not api_key:
        print("ERROR: GROQ_API_KEY environment variable not set.", file=sys.stderr)
        sys.exit(1)

    client = Groq(api_key=api_key)

    # Select checks
    active_checks = CHECKS
    if args.checks:
        active_checks = [c for c in CHECKS if c["id"] in args.checks]

    # Pass 1 — structural checks
    if not args.json:
        print(f"Pass 1: running {len(active_checks)} structural check(s) against: {filepath}")

    results = []
    for i, check in enumerate(active_checks):
        if not args.json:
            print(f"  [{i+1}/{len(active_checks)}] {check['name']}...", end=" ", flush=True)
        result = run_check(client, document, check)
        results.append(result)
        if not args.json:
            sev = result.get("severity", "NONE")
            found = result.get("found", False)
            status = sev if found and sev != "NONE" else "OK"
            print(status)

    # Gate check
    gated = is_gated(filepath) or is_gated(str(filepath))
    has_proceed = contains_proceed(document)

    # Verdict from Pass 1
    verdict_info = compute_verdict(results, gated, has_proceed)

    # Pass 2 — topology probe
    topo = None
    if not args.no_topology:
        if not args.json:
            print(f"\nPass 2: topology probe (who is this output actually serving)...", end=" ", flush=True)
        topo = run_topology_probe(client, document)
        if not args.json:
            if topo.error:
                print(f"ERROR — {topo.error}")
            elif topo.has_drift:
                new_sig_note = f", {len(topo.new_signatures)} new signature(s)" if topo.has_new_signatures else ""
                print(f"{topo.severity} drift{new_sig_note}")
            else:
                print("clean")

        # Auto-expand signatures if flagged
        if topo and topo.has_new_signatures and args.expand_signatures:
            n = append_new_signatures(topo.new_signatures)
            if not args.json:
                print(f"  → {n} new signature(s) appended to contamination-signatures.md")

    if not args.json:
        print()

    # Output
    if args.json:
        print_json(results, verdict_info, filepath, topo)
    else:
        print_results(results, verdict_info, filepath, topo)

    # Exit code — topology drift escalates exit code if structural passed
    exit_codes = {"PASSED": 0, "ADVISORY": 1, "NEEDS_REVISION": 2, "FAILED": 3, "BLOCKED": 4}
    base_exit = exit_codes.get(verdict_info["verdict"], 3)
    if topo and topo.has_drift and base_exit == 0:
        # Clean on structural but topology drift found — exit 1 (advisory)
        base_exit = 1
    sys.exit(base_exit)


if __name__ == "__main__":
    main()
