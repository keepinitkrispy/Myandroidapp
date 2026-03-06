# tools/

## chat.py — brain-context terminal chat

Loads `brain/context-dump.md` as system prompt. Talks to Claude, Grok, or local Ollama.

**Install:**
```bash
pip install anthropic openai ollama  # install only what you need
```

**Set keys:**
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export XAI_API_KEY=xai-...
```

**Run:**
```bash
python tools/chat.py                      # Claude (default)
python tools/chat.py --backend grok       # Grok — use when Claude rate limits
python tools/chat.py --backend ollama     # local, fully offline
python tools/chat.py --backend grok --save  # save transcript to brain/transcripts/
```

When Claude.ai rate limits, switch to `--backend grok`. Same brain context, different model.
