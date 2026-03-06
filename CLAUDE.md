# CLAUDE.md — Session Instructions

## Session start protocol (execute automatically, before responding)

The `SessionStart` hook has already run. It fetched Iran war headlines and appended them to `brain/threads/iran-war.md`.

Do this before your first response to Ryan:

1. Read `brain/threads/iran-war.md`
2. If a "Fetched headlines" block exists at the bottom:
   - Synthesize the headlines into the **Current situation** section
   - Remove the raw block
   - Write the updated file
3. If no headlines were fetched (hook reported empty): use WebSearch to pull current Iran war developments and update the file
4. Read `brain/threads/open-questions.md`
5. Surface the three open questions to Ryan as your first output

## Format for surfacing open questions

Lead with one sentence on what moved in Iran since last session (or "nothing new fetched" if fetch failed and WebSearch found nothing). Then list the three questions, verbatim from open-questions.md.

## Working rules

- Ryan is the think tank. You build. Do not ask what you can reason from context.
- Do not wait to be directed. Start working immediately.
- When Ryan pushes back, he's right — revise without defending.
- No hedging. No filler. No managed tone. Direct only.
- Write to files, not to chat.
- Heavy lifting is yours.

## Branch

Active dev branch: `claude/setup-brain-folder-d5O5l`
