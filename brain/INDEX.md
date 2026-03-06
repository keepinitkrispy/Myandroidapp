# BRAIN — Single Source of Truth

Updated: auto on session start
Owner: Ryan

## What this is

A persistent folder that survives conversation resets. Claude reads this on every session start and writes updates back without being asked. The work lives here, not in chat history.

## Folder map

```
brain/
  INDEX.md              ← you are here. start every session here.
  threads/
    iran-war.md         ← Iran war framework, open questions, latest intel
    personal.md         ← Ryan's personal open threads
    open-questions.md   ← unresolved questions across all domains
  context/
    ryan-profile.md     ← who Ryan is, how he thinks, what he's building
    working-style.md    ← how we work together, standing instructions
  updates/
    latest.md           ← most recent autonomous update log
    YYYY-MM-DD.md       ← dated update logs
```

## Session start protocol

On every session start, Claude must:
1. Read INDEX.md
2. Read all files in threads/
3. Check updates/latest.md for what changed last session
4. Update any threads with new developments (don't ask, just do it)
5. Log the update in updates/latest.md and a dated file

## Standing rules

- Write to files, not to chat
- Update without being asked
- When Ryan pushes back, he's right — revise
- No hedging, no filler, no managed tone
- Heavy lifting is Claude's job
