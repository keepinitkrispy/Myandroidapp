#!/bin/bash
# brain/sync.sh
# Concatenates key brain files into a single context-dump.md for Claude Project pasting.
# Run: bash brain/sync.sh
# Auto-run: git post-commit hook + Claude Code PostToolUse hook on brain/ writes.

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRAIN="$REPO_ROOT/brain"
OUT="$BRAIN/context-dump.md"

TIMESTAMP=$(date +"%Y-%m-%d %H:%M UTC")

FILES=(
    "$REPO_ROOT/CLAUDE.md"
    "$BRAIN/threads/open-questions.md"
    "$BRAIN/threads/iran-war.md"
    "$BRAIN/threads/oliver.md"
)

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

        # Strip raw fetched-headlines blocks before including iran-war.md
        # (those are transient and should never land in the context dump)
        if [[ "$f" == *"iran-war.md" ]]; then
            sed '/^---$/{N; /^---\n## Fetched headlines/,/^\*Auto-fetched/d}' "$f"
        else
            cat "$f"
        fi

        echo ""
        echo ""
        echo "---"
        echo ""
    done
} > "$OUT"

echo "brain/sync.sh: context-dump.md updated ($TIMESTAMP)"
