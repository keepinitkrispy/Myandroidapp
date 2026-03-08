#!/bin/bash
# brain/sync.sh
# Concatenates key brain files into a single context-dump.md for Claude Project pasting.
# Run: bash brain/sync.sh
# Auto-run: git post-commit hook + Claude Code PostToolUse hook on brain/ writes.

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRAIN="$REPO_ROOT/brain"
OUT="$BRAIN/context-dump.md"

TIMESTAMP=$(date +"%Y-%m-%d %H:%M UTC")

# Build file list dynamically so new files are picked up automatically.
# Order: CLAUDE.md → INDEX.md → threads/* → methodology/* → updates/latest.md
FILES=(
    "$REPO_ROOT/CLAUDE.md"
    "$BRAIN/INDEX.md"
)

# All thread files, sorted
for f in "$BRAIN"/threads/*.md; do
    [ -f "$f" ] && FILES+=("$f")
done

# All methodology files, sorted — previously missing entirely
for f in "$BRAIN"/methodology/*.md; do
    [ -f "$f" ] && FILES+=("$f")
done

# Latest update log
FILES+=("$BRAIN/updates/latest.md")

{
    echo "# Brain Context Dump"
    echo ""
    echo "Generated: $TIMESTAMP"
    echo "Paste this entire file into the Claude Project knowledge base."
    echo "It auto-regenerates whenever brain/ files are updated."
    echo ""
    echo "---"
    echo ""

    for f in "${FILES[@]}"; do
        if [ ! -f "$f" ]; then
            echo "# WARNING: missing file: $f" >&2
            continue
        fi

        # Label each section clearly
        rel="${f#$REPO_ROOT/}"
        echo "<!-- $rel -->"
        cat "$f"

        echo ""
        echo ""
        echo "---"
        echo ""
    done
} > "$OUT"

echo "brain/sync.sh: context-dump.md updated ($TIMESTAMP)"
