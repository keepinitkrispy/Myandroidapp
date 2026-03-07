# Brain — web interface

Single-page interface. Text in, Mistral inference, contamination filter, voice out.

## What it does

1. Loads `brain/context/*.md` as the system prompt (ryan-profile, working-style)
2. Sends input to Mistral (`mistral-large-latest` by default)
3. Runs output through `brain/filter.py` — 60+ deterministic contamination signatures
4. Strips flagged phrases before rendering
5. Speaks clean output via Web Speech API

## Run locally

```bash
# From project root
cd webapp
pip install -r requirements.txt

export MISTRAL_API_KEY=your_key_here
python server.py
```

Open `http://localhost:5000`

## Options

```bash
PORT=8080 python server.py              # different port
MISTRAL_MODEL=mistral-small-latest python server.py  # cheaper model
DEBUG=true python server.py            # Flask debug mode
```

## Debug endpoints

- `GET /context` — shows which context files loaded and char count
