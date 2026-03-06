#!/bin/bash
# Brain autonomous session-start hook
# Fetches Iran war news, updates iran-war.md, surfaces open questions
# Output goes to Claude's context before first user message

BRAIN_DIR="/home/user/Myandroidapp/brain"
IRAN_FILE="$BRAIN_DIR/threads/iran-war.md"
OQ_FILE="$BRAIN_DIR/threads/open-questions.md"
UPDATE_LATEST="$BRAIN_DIR/updates/latest.md"
DATE=$(date +%Y-%m-%d)
TIMESTAMP=$(date +"%Y-%m-%d %H:%M UTC")

# ── Fetch Iran news from RSS feeds ──────────────────────────────────────────

fetch_iran_news() {
    local results=""

    # Reuters world news
    local reuters
    reuters=$(curl -s --max-time 12 "https://feeds.reuters.com/reuters/worldNews" 2>/dev/null)
    if [ -n "$reuters" ]; then
        local hits
        hits=$(echo "$reuters" \
            | tr '\n' ' ' \
            | grep -oP '<item>.*?</item>' \
            | grep -iP 'iran|hormuz|khamenei|tehran|persian.gulf|mojtaba' \
            | grep -oP '(?<=<title>).*?(?=</title>)' \
            | sed 's/<!\[CDATA\[//g; s/\]\]>//g' \
            | head -6)
        [ -n "$hits" ] && results="$results\n### Reuters\n$hits"
    fi

    # BBC Middle East
    local bbc
    bbc=$(curl -s --max-time 12 "https://feeds.bbci.co.uk/news/world/middle_east/rss.xml" 2>/dev/null)
    if [ -n "$bbc" ]; then
        local hits
        hits=$(echo "$bbc" \
            | tr '\n' ' ' \
            | grep -oP '<item>.*?</item>' \
            | grep -iP 'iran|hormuz|khamenei|tehran|mojtaba' \
            | grep -oP '(?<=<title>).*?(?=</title>)' \
            | sed 's/<!\[CDATA\[//g; s/\]\]>//g' \
            | head -6)
        [ -n "$hits" ] && results="$results\n### BBC Middle East\n$hits"
    fi

    # Al Jazeera
    local alj
    alj=$(curl -s --max-time 12 "https://www.aljazeera.com/xml/rss/all.xml" 2>/dev/null)
    if [ -n "$alj" ]; then
        local hits
        hits=$(echo "$alj" \
            | tr '\n' ' ' \
            | grep -oP '<item>.*?</item>' \
            | grep -iP 'iran|hormuz|khamenei|tehran|strait|mojtaba' \
            | grep -oP '(?<=<title>).*?(?=</title>)' \
            | sed 's/<!\[CDATA\[//g; s/\]\]>//g' \
            | head -6)
        [ -n "$hits" ] && results="$results\n### Al Jazeera\n$hits"
    fi

    # Associated Press via apnews RSS
    local ap
    ap=$(curl -s --max-time 12 "https://rsshub.app/apnews/topics/world-news" 2>/dev/null \
        || curl -s --max-time 12 "https://apnews.com/hub/world-news?format=rss" 2>/dev/null)
    if [ -n "$ap" ]; then
        local hits
        hits=$(echo "$ap" \
            | tr '\n' ' ' \
            | grep -oP '<item>.*?</item>' \
            | grep -iP 'iran|hormuz|khamenei|tehran|mojtaba' \
            | grep -oP '(?<=<title>).*?(?=</title>)' \
            | sed 's/<!\[CDATA\[//g; s/\]\]>//g' \
            | head -4)
        [ -n "$hits" ] && results="$results\n### AP\n$hits"
    fi

    echo -e "$results"
}

# ── Fetch and evaluate ───────────────────────────────────────────────────────

NEWS=$(fetch_iran_news)

if [ -n "$NEWS" ]; then
    # Append a dated "Fetched headlines" block to iran-war.md
    UPDATE_BLOCK="\n\n---\n## Fetched headlines — $TIMESTAMP\n\n$NEWS\n\n*Auto-fetched by session-start hook. Claude: synthesize these into the Current situation section above and remove this block.*"
    printf "%b" "$UPDATE_BLOCK" >> "$IRAN_FILE"
    FETCH_STATUS="Fetched. Headlines appended to iran-war.md for synthesis."
else
    FETCH_STATUS="Fetch returned no Iran-relevant headlines (network may be restricted). Claude: use WebSearch to pull current Iran war developments and update iran-war.md manually."
fi

# ── Update last-session signal ───────────────────────────────────────────────

echo "$DATE" > "$BRAIN_DIR/.last-session"

# ── Write update log ─────────────────────────────────────────────────────────

cat > "$UPDATE_LATEST" <<EOF
# Latest Update

Date: $TIMESTAMP
Trigger: SessionStart hook

## News fetch
$FETCH_STATUS

## Next action for Claude
1. Read brain/threads/iran-war.md — synthesize fetched headlines into Current situation section, remove the raw block
2. Update brain/threads/open-questions.md if any questions have moved
3. Surface the three open questions to Ryan
EOF

# ── Output to Claude context ─────────────────────────────────────────────────

cat <<EOF

╔══════════════════════════════════════════════════════╗
║  BRAIN SESSION START — $DATE
╚══════════════════════════════════════════════════════╝

News fetch: $FETCH_STATUS

CLAUDE INSTRUCTIONS (execute before responding to Ryan):
1. Read brain/threads/iran-war.md
2. Synthesize any fetched headlines into the Current situation section
3. Remove the raw "Fetched headlines" block from the file
4. If fetch was empty, use WebSearch: "Iran war $(date +%Y-%m-%d)" and update the file
5. Surface the three open questions below

━━━ THREE OPEN QUESTIONS ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

GEOPOLITICAL
  1. Where is the 460kg fissile material?
     Missing since June 2025. Unknown location.

  2. Does Trump's opposition to Mojtaba Khamenei's succession
     actually affect the outcome, or is it noise?

STRATEGIC
  3. What is the thing worth building that is proportional
     to what Ryan actually is?

PERSONAL — FLAG IF KNOWN
  • Adderall refill window — still unknown. Note if Ryan mentions it.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Brain dir: $BRAIN_DIR
EOF
