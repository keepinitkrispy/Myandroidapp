#!/bin/bash
# Brain autonomous update hook
# Runs on Claude Code session start
# Signals Claude to read the brain and update threads

BRAIN_DIR="$(dirname "$0")/../../brain"
DATE=$(date +%Y-%m-%d)

echo "=== BRAIN SESSION START ==="
echo "Date: $DATE"
echo "Brain dir: $BRAIN_DIR"
echo ""
echo "INSTRUCTION: Read brain/INDEX.md and follow the session start protocol."
echo "Update all active threads. Log changes to brain/updates/latest.md and brain/updates/$DATE.md"
echo "==========================="

# Touch a signal file so Claude knows a new session started
echo "$DATE" > "$BRAIN_DIR/.last-session"
