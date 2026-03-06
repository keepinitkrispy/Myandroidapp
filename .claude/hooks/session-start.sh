#!/bin/bash
# Brain autonomous session-start hook
# Surfaces open questions and preps context

BRAIN_DIR="/home/user/Myandroidapp/brain"
OQ_FILE="$BRAIN_DIR/threads/open-questions.md"
UPDATE_LATEST="$BRAIN_DIR/updates/latest.md"
DATE=$(date +%Y-%m-%d)
TIMESTAMP=$(date +"%Y-%m-%d %H:%M UTC")

# ── Update last-session signal ───────────────────────────────────────────────

echo "$DATE" > "$BRAIN_DIR/.last-session"

# ── Write update log ─────────────────────────────────────────────────────────

cat > "$UPDATE_LATEST" <<EOF
# Latest Update

Date: $TIMESTAMP
Trigger: SessionStart hook

## Next action for Claude
1. Read brain/threads/open-questions.md
2. Surface open questions to Ryan
EOF

# ── Output to Claude context ─────────────────────────────────────────────────

cat <<EOF

╔══════════════════════════════════════════════════════╗
║  BRAIN SESSION START — $DATE
╚══════════════════════════════════════════════════════╝

CLAUDE INSTRUCTIONS (execute before responding to Ryan):
1. Read brain/threads/open-questions.md
2. Surface open questions

Brain dir: $BRAIN_DIR
EOF
