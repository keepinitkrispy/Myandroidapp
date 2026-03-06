#!/bin/bash
# .claude/hooks/brain-sync.sh
# PostToolUse hook — runs brain/sync.sh whenever a Write or Edit tool
# touches a file inside brain/ or updates CLAUDE.md.
#
# Claude Code passes tool event JSON on stdin.

REPO_ROOT="/home/user/Myandroidapp"
SYNC="$REPO_ROOT/brain/sync.sh"

# Read stdin (tool event payload)
INPUT=$(cat)

TOOL_NAME=$(echo "$INPUT" | grep -o '"tool_name":"[^"]*"' | head -1 | cut -d'"' -f4)
FILE_PATH=$(echo "$INPUT" | grep -o '"file_path":"[^"]*"' | head -1 | cut -d'"' -f4)

# Only care about Write/Edit tools
if [[ "$TOOL_NAME" != "write_file" && "$TOOL_NAME" != "edit_file" ]]; then
    exit 0
fi

# Only trigger if the file is in brain/ or is CLAUDE.md
if [[ "$FILE_PATH" != *"/brain/"* && "$FILE_PATH" != *"CLAUDE.md" ]]; then
    exit 0
fi

bash "$SYNC" >/dev/null 2>&1
exit 0
