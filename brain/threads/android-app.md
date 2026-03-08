# Android App — Direction

Status: Pre-build. No code yet.
Started: 2026-03-06

## What it is

Local-first Android app that hosts the brain folder and runs on-device inference. The scaffold's interface layer for Oliver — and eventually other kids like him.

Not a cloud product. Not a subscription. Not a platform that can be acquired or subpoenaed.

## Why Android, why local

- Ryan is on Android
- Local-first is load-bearing for sovereign developmental memory (see scaffold.md)
- On-device models are now viable: Phi-3 Mini (3.8B), Gemma 2B, Gemma 3 run well on mid-range Android hardware
- No cloud dependency = no institutional access risk, no rate limits, no latency, full offline operation
- Oliver's data never leaves the device

## What it solves simultaneously

1. Rate limits — inference runs locally, no API quotas
2. Offline operation — works without internet
3. Oliver's interface — voice dump, game-design mode, persistent presence
4. Data privacy — sovereign developmental record, physically controlled
5. Cost — no per-token billing after initial setup

## MVP definition

**One session, one kid, no cloud.**

Minimum viable:
- Brain folder stored on-device (read/write)
- Chat interface backed by on-device model
- Context-dump.md loaded as system prompt (same as `tools/chat.py`)
- Voice input option (Android SpeechRecognizer, no external API)
- Session transcript saved to brain/transcripts/ on-device

Not in MVP:
- Multi-kid support
- Parent dashboard
- Sync across devices
- Custom model fine-tuning

## Stack

- Language: Kotlin
- On-device inference: MediaPipe LLM API (Google, supports Gemma 2B/3 natively on Android) or llama.cpp via JNI
- Voice: Android SpeechRecognizer (built-in, offline capable)
- Storage: plain files in app-private directory (same structure as brain/)
- UI: Jetpack Compose

**Model recommendation for MVP:** Gemma 3 2B via MediaPipe LLM API
- Ships as a downloadable model file (~1.5GB)
- Google's MediaPipe handles the JNI layer — no C++ build required
- Runs on Android 10+ with 6GB+ RAM

## Oliver entry points

Two modes based on what he brings:

**Dump mode** — he talks, it listens. Voice in. No pressure to respond correctly. No questions until he's done. Captures what's in his head before it evaporates.

**Build mode** — game design, YouTube concepts, ideas. Interactive, lateral, meets his pace. Treats game mechanics as legitimate architecture problems.

The scaffold doesn't switch modes on him. He implicitly selects by what he opens with.

## Build sequence

1. ~~Document direction~~ ← this file
2. Initialize Kotlin Android project in repo root
3. Get Gemma 2B running in-app via MediaPipe (proof of concept, single hardcoded prompt)
4. Wire brain/context-dump.md as system prompt
5. Basic chat UI (Compose)
6. Voice input via SpeechRecognizer
7. Session transcript save
8. Oliver-specific modes (dump / build)

## Open questions

- Does Oliver use voice or text? (First session will answer this)
- What does the launch screen look like for a 7-year-old AuDHD kid? No onboarding, no tutorial.
- How does it handle his regulated vs. decompressing state differently?
- Model download UX — 1.5GB first launch, needs to be honest and fast
