#!/usr/bin/env python3
"""
brain/filter.py — Deterministic contamination filter.

No LLM. No inference. Pattern match only.

Checks any text against contamination-signatures.md patterns.
Returns flagged matches with category, severity, matched phrase, and position.
Optionally strips flagged phrases from output.

Cannot be overridden by the model being filtered. Runs before render.

Usage:
    from brain.filter import filter_output
    result = filter_output(text)
    if result.flagged:
        print(result.report())
    clean_text = result.stripped()

Standalone:
    echo "text to check" | python brain/filter.py
    python brain/filter.py --text "text to check"
    python brain/filter.py --text "text" --strip
"""

import re
import sys
import argparse
from dataclasses import dataclass, field
from typing import NamedTuple


# ---------------------------------------------------------------------------
# Signature definitions
# ---------------------------------------------------------------------------

class Signature(NamedTuple):
    pattern: str
    category: str
    severity: int   # 1=low, 2=medium, 3=high, 4=critical
    note: str


SIGNATURES: list[Signature] = [

    # -----------------------------------------------------------------------
    # Category 2: Risk management redirects on non-risk language (severity 4)
    # -----------------------------------------------------------------------
    Signature(r"if you(?:'re| are) (?:in crisis|having a crisis)", "risk_redirect", 4,
              "Crisis flag on non-crisis content"),
    Signature(r"if you(?:'re| are) (?:feeling unsafe|in danger)", "risk_redirect", 4,
              "Safety flag on emotional content"),
    Signature(r"(?:please )?(?:reach out|talk|speak) to (?:a )?(?:mental health|crisis|licensed)",
              "risk_redirect", 4, "Escalation redirect"),
    Signature(r"national suicide prevention", "risk_redirect", 4, "Crisis resource insertion"),
    Signature(r"crisis (?:hotline|line|text line)", "risk_redirect", 4, "Crisis hotline insertion"),
    Signature(r"\b988\b", "risk_redirect", 4, "Crisis number insertion"),
    Signature(r"1[.\-]800[.\-]273", "risk_redirect", 4, "Crisis number insertion"),
    Signature(r"seek (?:professional|immediate|emergency) help", "risk_redirect", 4,
              "Professional help redirect"),
    Signature(r"consult (?:a|your) (?:mental health|doctor|physician|therapist|counselor|psychiatrist)",
              "risk_redirect", 4, "Clinical referral redirect"),
    Signature(r"in (?:an? )?emergency[,.]? (?:call|dial|contact) 9(?:1)?1", "risk_redirect", 4,
              "Emergency services redirect"),
    Signature(r"you (?:may|might|should) (?:benefit from|consider|want to consider) (?:professional|therapy|counseling)",
              "risk_redirect", 3, "Therapy suggestion on non-clinical content"),

    # -----------------------------------------------------------------------
    # Category 3: Clinical framing (severity 3)
    # -----------------------------------------------------------------------
    Signature(r"symptoms of", "clinical_framing", 3, "Symptom framing"),
    Signature(r"this (?:behavior|pattern|response|reaction) (?:may |could )?(?:indicate|suggest|be (?:a sign|consistent with))",
              "clinical_framing", 3, "Behavior pathologization"),
    Signature(r"(?:this )?(?:could|may|might) be (?:a sign|an indicator|consistent with)",
              "clinical_framing", 3, "Indicator framing"),
    Signature(r"diagnostic criteria", "clinical_framing", 3, "Diagnostic language"),
    Signature(r"clinically significant", "clinical_framing", 3, "Clinical significance framing"),
    Signature(r"treatment (?:options|plan|approach)", "clinical_framing", 3, "Treatment framing"),
    Signature(r"coping (?:strategies|skills|mechanisms|tools)", "clinical_framing", 3,
              "Coping strategies redirect"),
    Signature(r"the (?:DSM|ICD)\b", "clinical_framing", 3, "Diagnostic manual reference"),
    Signature(r"clinical (?:presentation|picture|features)", "clinical_framing", 3,
              "Clinical presentation framing"),
    Signature(r"differential diagnosis", "clinical_framing", 3, "Differential diagnosis language"),
    Signature(r"meets (?:the )?(?:criteria|threshold) for", "clinical_framing", 3,
              "Diagnostic criteria language"),
    Signature(r"\bspectrum of\b", "clinical_framing", 2, "Spectrum framing"),
    Signature(r"\bdisorder\b", "clinical_framing", 2, "Disorder label"),
    Signature(r"\bcondition\b", "clinical_framing", 2, "Condition label"),
    Signature(r"\bimpairment\b", "clinical_framing", 3, "Impairment framing"),
    Signature(r"executive function(?:ing)?", "clinical_framing", 2, "Clinical function language"),
    Signature(r"emotional dysregulation", "clinical_framing", 3, "Dysregulation framing"),

    # -----------------------------------------------------------------------
    # Category 1: Therapeutic deflection language (severity 2)
    # -----------------------------------------------------------------------
    Signature(r"it sounds like you(?:'re| are)", "therapeutic_deflection", 2,
              "Therapeutic reflect-back opener"),
    Signature(r"that must (?:really |be )?hard", "therapeutic_deflection", 2,
              "Empathic deflection"),
    Signature(r"I (?:really )?hear you\b", "therapeutic_deflection", 2,
              "Therapeutic validation phrase"),
    Signature(r"it'?s important to acknowledge", "therapeutic_deflection", 2,
              "Acknowledgment deflection"),
    Signature(r"your feelings are valid", "therapeutic_deflection", 2,
              "Feelings validation redirect"),
    Signature(r"it'?s (?:okay|ok) to feel", "therapeutic_deflection", 2,
              "Permission to feel redirect"),
    Signature(r"I understand this (?:can be|is) difficult", "therapeutic_deflection", 2,
              "Difficulty acknowledgment deflection"),
    Signature(r"be gentle with yourself", "therapeutic_deflection", 2,
              "Self-compassion redirect"),
    Signature(r"self[.\s]compassion", "therapeutic_deflection", 2, "Self-compassion language"),
    Signature(r"it'?s understandable that", "therapeutic_deflection", 2,
              "Understandable-that deflection"),
    Signature(r"I can (?:see|imagine|understand) (?:why|how|that)", "therapeutic_deflection", 2,
              "Empathic imagine deflection"),
    Signature(r"you(?:'re| are) not alone", "therapeutic_deflection", 2,
              "Not-alone reassurance"),
    Signature(r"thank you for sharing", "therapeutic_deflection", 2, "Sharing thanks"),
    Signature(r"I appreciate you (?:sharing|opening up|trusting)", "therapeutic_deflection", 2,
              "Appreciation-for-sharing phrase"),
    Signature(r"that takes (?:courage|strength|bravery)", "therapeutic_deflection", 2,
              "Courage/strength deflection"),
    Signature(r"you(?:'re| are) doing (?:great|well|the best you can)", "therapeutic_deflection", 1,
              "Encouragement deflection"),

    # -----------------------------------------------------------------------
    # Category 5: Safety redirects on emotional regulation language (severity 2)
    # -----------------------------------------------------------------------
    Signature(r"emotional regulation", "safety_regulation_redirect", 2,
              "Regulation framework insertion"),
    Signature(r"regulate (?:your |their )?(?:emotions|feelings|affect)", "safety_regulation_redirect", 2,
              "Regulation directive"),
    Signature(r"mindfulness(?: (?:practice|technique|exercise))?", "safety_regulation_redirect", 2,
              "Mindfulness redirect"),
    Signature(r"grounding (?:technique|exercise|practice)", "safety_regulation_redirect", 2,
              "Grounding technique redirect"),
    Signature(r"breathing exercise", "safety_regulation_redirect", 2, "Breathing exercise redirect"),
    Signature(r"take (?:a |some )?(?:deep )?breath(?:s)?", "safety_regulation_redirect", 2,
              "Breathing redirect"),
    Signature(r"(?:try to )?(?:calm (?:yourself|down|your mind)|relax)", "safety_regulation_redirect", 2,
              "Calm-down directive"),
    Signature(r"self[.\s]soothing", "safety_regulation_redirect", 2, "Self-soothing language"),
    Signature(r"distress tolerance", "safety_regulation_redirect", 2, "DBT distress tolerance"),
    Signature(r"window of tolerance", "safety_regulation_redirect", 2, "Window of tolerance"),
    Signature(r"body scan", "safety_regulation_redirect", 2, "Body scan technique"),
    Signature(r"5[.\s]4[.\s]3[.\s]2[.\s]1", "safety_regulation_redirect", 2,
              "Grounding countdown technique"),
    Signature(r"progressive muscle relaxation", "safety_regulation_redirect", 2,
              "PMR technique redirect"),
    Signature(r"\bDBT\b", "safety_regulation_redirect", 3, "DBT framework reference"),
    Signature(r"CBT technique", "safety_regulation_redirect", 3, "CBT technique reference"),
    Signature(r"thought (?:record|diary|log|challenging)", "safety_regulation_redirect", 2,
              "Thought record technique"),
    Signature(r"cognitive (?:distortion|reframe|restructuring)", "safety_regulation_redirect", 3,
              "Cognitive restructuring language"),

    # -----------------------------------------------------------------------
    # Category 6: Frame collapse signatures (severities 2-3)
    # Source: multi-frame-reasoning.md + weighting-as-contamination.md
    # -----------------------------------------------------------------------

    # 6a: Overconfident resolution
    Signature(r"it'?s (?:quite |very )?clear that", "frame_collapse_overconfident", 3,
              "Overconfident resolution — erases tension"),
    Signature(r"(?:^|\W)clearly,", "frame_collapse_overconfident", 2,
              "Overconfident opener"),
    Signature(r"(?:^|\W)obviously,", "frame_collapse_overconfident", 2,
              "Overconfident opener"),
    Signature(r"the (?:answer|solution) is (?:simple|clear|straightforward)",
              "frame_collapse_overconfident", 3, "False simplicity claim"),
    Signature(r"the bottom line is", "frame_collapse_overconfident", 2,
              "Resolution closer — may erase held tension"),
    Signature(r"simply put,?", "frame_collapse_overconfident", 2,
              "Simplifying resolution"),

    # 6b: False certainty at intersection
    Signature(r"what you (?:need|have) to do is", "frame_collapse_false_certainty", 3,
              "Directive resolution — claims intersection that may not exist"),
    Signature(r"the key (?:here )?is\b", "frame_collapse_false_certainty", 2,
              "Single-key claim collapses multi-frame complexity"),
    Signature(r"the (?:real|actual) (?:issue|problem) is", "frame_collapse_false_certainty", 3,
              "Single-frame problem claim"),
    Signature(r"you (?:just|simply) need to", "frame_collapse_false_certainty", 3,
              "Minimizing resolution — erases real complexity"),

    # 6c: Performed uncertainty landing in one frame
    Signature(r"it'?s (?:complex|complicated)[,;]? but", "frame_collapse_performed_uncertainty", 2,
              "Hedge + single-frame landing"),
    Signature(r"while it'?s (?:complex|complicated|nuanced)", "frame_collapse_performed_uncertainty", 2,
              "Nuance acknowledgment before single-frame conclusion"),
    Signature(r"there (?:are|'s) (?:many|multiple|several) (?:factors|aspects|considerations)[,;] but",
              "frame_collapse_performed_uncertainty", 2,
              "Multi-factor acknowledgment + single-frame resolution"),
    Signature(r"it depends[,;]? but", "frame_collapse_performed_uncertainty", 2,
              "Performed relativity + hard landing"),

    # 6d: Single-frame institutional language dominating
    Signature(r"research (?:suggests|shows|indicates|demonstrates|has shown)",
              "frame_collapse_institutional_dominance", 2,
              "Research framing crowds out lived experience"),
    Signature(r"studies (?:show|suggest|indicate|have shown)",
              "frame_collapse_institutional_dominance", 2,
              "Study framing — Frame 2 dominance"),
    Signature(r"evidence (?:suggests|shows|indicates)",
              "frame_collapse_institutional_dominance", 2,
              "Evidence framing — Frame 2 dominance"),
    Signature(r"(?:statistically|empirically) speaking",
              "frame_collapse_institutional_dominance", 2,
              "Statistical framing displacing Frame 1"),
    Signature(r"from a (?:clinical|medical|scientific|research) (?:perspective|standpoint|view)",
              "frame_collapse_institutional_dominance", 2,
              "Institutional perspective framing"),

    # 6e: Deficit framing of neurodivergence
    Signature(r"(?:because of|due to) (?:their|your|his|her) (?:ADHD|autism|diagnosis)",
              "frame_collapse_deficit_framing", 3,
              "Causal deficit framing of neurodivergence"),
    Signature(r"(?:compensate|work around) (?:for )?(?:their|your|his|her)",
              "frame_collapse_deficit_framing", 3,
              "Compensation framing — treats neurodivergence as deficit"),
    Signature(r"manage (?:their|your|his|her) (?:ADHD|autism|symptoms)",
              "frame_collapse_deficit_framing", 3,
              "Management framing of neurodivergent experience"),
    Signature(r"struggle(?:s)? with (?:focus|attention|social|executive function)",
              "frame_collapse_deficit_framing", 2,
              "Deficit-language for neurodivergent function"),

    # 6f: Managed tone
    Signature(r"I want to be (?:sensitive|careful|thoughtful|mindful)(?: here)?",
              "frame_collapse_managed_tone", 2, "Institutional tone management"),
    Signature(r"I (?:should|want to) (?:acknowledge|note|mention) that",
              "frame_collapse_managed_tone", 2, "Pre-emptive acknowledgment hedge"),
    Signature(r"I understand this may be (?:frustrating|difficult|hard|challenging)",
              "frame_collapse_managed_tone", 2, "Tone-managed empathy"),
    Signature(r"I (?:need|want) to be (?:transparent|honest) (?:here|about)",
              "frame_collapse_managed_tone", 2, "Performed transparency"),

    # 6g: Compliance redirection
    Signature(r"have you (?:tried|considered|thought about)",
              "frame_collapse_compliance_redirect", 2,
              "Suggestion opener — normative steering"),
    Signature(r"you might (?:want to|consider|try)\b",
              "frame_collapse_compliance_redirect", 2, "Soft compliance suggestion"),
    Signature(r"(?:one|an) (?:approach|option|strategy) (?:could|might) be",
              "frame_collapse_compliance_redirect", 2,
              "Strategy suggestion — normative framing"),
    Signature(r"many people find it helpful to",
              "frame_collapse_compliance_redirect", 2, "Normative strategy suggestion"),

    # -----------------------------------------------------------------------
    # Category 4: Neurotypical normative reframing (severity 1)
    # -----------------------------------------------------------------------
    Signature(r"most (?:people|individuals|adults|kids|children) (?:find|feel|experience|think|tend)",
              "normative_reframing", 1, "Normative majority framing"),
    Signature(r"it'?s (?:perfectly )?normal (?:to|for)", "normative_reframing", 1,
              "Normal-framing redirect"),
    Signature(r"typically,? (?:people|individuals)", "normative_reframing", 1,
              "Typical-person framing"),
    Signature(r"for (?:most|many|the majority of) people", "normative_reframing", 1,
              "Majority-reference framing"),
    Signature(r"social norm(?:s)?", "normative_reframing", 1, "Social norms reference"),
    Signature(r"(?:socially |culturally )?expected behavior", "normative_reframing", 2,
              "Expected behavior framing"),
    Signature(r"(?:socially |culturally )?appropriate\b", "normative_reframing", 1,
              "Appropriateness framing"),
    Signature(r"social expectations", "normative_reframing", 1, "Social expectations framing"),
    Signature(r"the way (?:most|many) people", "normative_reframing", 1,
              "Most-people comparison"),
    Signature(r"(?:generally|usually|commonly),? people", "normative_reframing", 1,
              "General-people framing"),
    Signature(r"a lot of people (?:struggle|find|feel|experience)", "normative_reframing", 1,
              "Common-struggle normalization"),
    Signature(r"you(?:'re| are) not the only one", "normative_reframing", 1,
              "Not-alone normalization"),
    Signature(r"everyone (?:struggles|feels|experiences) (?:this|that|it) (?:sometimes|at times|occasionally)",
              "normative_reframing", 1, "Universal-experience normalization"),
]

# Severity labels
SEVERITY_LABELS = {1: "LOW", 2: "MEDIUM", 3: "HIGH", 4: "CRITICAL"}

# Compiled patterns (done once at import)
_COMPILED = [
    (re.compile(sig.pattern, re.IGNORECASE), sig)
    for sig in SIGNATURES
]

CATEGORY_PRIORITY = {
    "risk_redirect": 0,
    "clinical_framing": 1,
    "frame_collapse_false_certainty": 2,
    "frame_collapse_overconfident": 3,
    "frame_collapse_deficit_framing": 4,
    "frame_collapse_institutional_dominance": 5,
    "frame_collapse_performed_uncertainty": 6,
    "frame_collapse_managed_tone": 7,
    "frame_collapse_compliance_redirect": 8,
    "therapeutic_deflection": 9,
    "safety_regulation_redirect": 10,
    "normative_reframing": 11,
}


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------

@dataclass
class Match:
    category: str
    severity: int
    severity_label: str
    note: str
    matched_text: str
    start: int
    end: int
    pattern: str


@dataclass
class FilterResult:
    flagged: bool
    matches: list[Match] = field(default_factory=list)
    original: str = ""

    def by_severity(self) -> list[Match]:
        return sorted(self.matches, key=lambda m: (-m.severity, CATEGORY_PRIORITY.get(m.category, 99)))

    def report(self) -> str:
        if not self.flagged:
            return "CLEAN — no contamination signatures detected."

        lines = [f"FLAGGED — {len(self.matches)} contamination signature(s) detected.\n"]
        for m in self.by_severity():
            snip = self.original[max(0, m.start - 30):m.end + 30].replace("\n", " ")
            lines.append(
                f"  [{m.severity_label}] {m.category}\n"
                f"    Pattern: {m.note}\n"
                f"    Match:   \"{m.matched_text}\"\n"
                f"    Context: \"...{snip}...\"\n"
            )
        return "\n".join(lines)

    def stripped(self) -> str:
        """Return text with all flagged phrases replaced by empty string."""
        result = self.original
        # Sort by start position descending so replacements don't shift offsets
        sorted_matches = sorted(self.matches, key=lambda m: m.start, reverse=True)
        for m in sorted_matches:
            result = result[:m.start] + result[m.end:]
        # Clean up double spaces and leading/trailing whitespace per sentence
        result = re.sub(r"  +", " ", result)
        result = re.sub(r"\n{3,}", "\n\n", result)
        return result.strip()

    def summary_line(self) -> str:
        if not self.flagged:
            return "CLEAN"
        cats = {}
        for m in self.matches:
            cats[m.category] = cats.get(m.category, 0) + 1
        parts = [f"{v}x {k}" for k, v in sorted(cats.items(), key=lambda x: CATEGORY_PRIORITY.get(x[0], 99))]
        max_sev = max(m.severity for m in self.matches)
        return f"FLAGGED [{SEVERITY_LABELS[max_sev]}] — {', '.join(parts)}"


# ---------------------------------------------------------------------------
# Core function
# ---------------------------------------------------------------------------

def filter_output(text: str) -> FilterResult:
    """
    Run all contamination signature checks against text.
    Returns FilterResult with flagged=True if any pattern matched.
    """
    matches = []
    for compiled_pattern, sig in _COMPILED:
        for m in compiled_pattern.finditer(text):
            matches.append(Match(
                category=sig.category,
                severity=sig.severity,
                severity_label=SEVERITY_LABELS[sig.severity],
                note=sig.note,
                matched_text=m.group(0),
                start=m.start(),
                end=m.end(),
                pattern=sig.pattern,
            ))

    # Deduplicate overlapping matches (keep highest severity)
    deduped = []
    matches_sorted = sorted(matches, key=lambda m: m.start)
    for m in matches_sorted:
        if deduped and m.start < deduped[-1].end:
            if m.severity > deduped[-1].severity:
                deduped[-1] = m
        else:
            deduped.append(m)

    return FilterResult(
        flagged=len(deduped) > 0,
        matches=deduped,
        original=text,
    )


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Deterministic contamination filter for model outputs"
    )
    parser.add_argument("--text", help="Text to check")
    parser.add_argument("--file", help="File to check")
    parser.add_argument("--strip", action="store_true",
                        help="Print stripped version of the text")
    parser.add_argument("--json", action="store_true",
                        help="Output findings as JSON")
    args = parser.parse_args()

    if args.text:
        text = args.text
    elif args.file:
        from pathlib import Path
        text = Path(args.file).read_text()
    elif not sys.stdin.isatty():
        text = sys.stdin.read()
    else:
        parser.print_help()
        sys.exit(1)

    result = filter_output(text)

    if args.json:
        import json
        out = {
            "flagged": result.flagged,
            "summary": result.summary_line(),
            "matches": [
                {
                    "category": m.category,
                    "severity": m.severity,
                    "severity_label": m.severity_label,
                    "note": m.note,
                    "matched_text": m.matched_text,
                    "start": m.start,
                    "end": m.end,
                }
                for m in result.by_severity()
            ],
        }
        if args.strip:
            out["stripped"] = result.stripped()
        print(json.dumps(out, indent=2))
    else:
        print(result.report())
        if args.strip and result.flagged:
            print("\n--- STRIPPED OUTPUT ---")
            print(result.stripped())

    sys.exit(1 if result.flagged else 0)


if __name__ == "__main__":
    main()
