# Source Analysis — Youtube-Video-Summeriser

Source: `https://github.com/Yuvraj-ai/Youtube-Video-Summeriser` @ `3b7db1a`
Analyzed: 2026-09-30

## Repository structure

```
main.py            26 lines  Streamlit frontend (entry point)
uany.py            65 lines  LangChain core: load → chunk → embed → retrieve → answer
requirements.txt   27 lines  Unpinned lower-bound deps
readme.md         130 lines  Docs
.devcontainer/              Codespaces config, auto-runs streamlit
```

No tests. No CI. No package metadata. No database. No `.env` (the dotenv code is commented out at `uany.py:10-12`).

## Deterministic inventory (skill scripts)

- `analyze_repository.py`: 2 Python files, 1 entry point (`main.py`), 2 functions, 0 classes, imports `streamlit, langchain, langchain_community, langchain_core, langchain_google_genai, pydantic, textwrap, uany`.
- `detect_streamlit.py`: Streamlit confirmed in `main.py` — constructs used: `title, sidebar, form, form_submit_button, text_input, text_area, subheader, text`.
- `detect_secrets.py`: **0 findings.** No hardcoded credentials.

## Architecture (as observed)

Presentation (Streamlit) is fused with orchestration: `main.py` calls `uany.py` directly, per rerun. There is no service layer, no API boundary, no persistence, and no configuration module.

```
Streamlit form (api key, url, question)
      |
      |  runs on EVERY rerun whenever query and url are non-empty
      v
uany.create_vector_db_from_youtube_url()   ← download + chunk + embed + FAISS, rebuilt per rerun
uany.get_response_from_query()             ← similarity_search(k=4) → PromptTemplate → LLMChain
      |
      v
st.text(textwrap.fill(response, 80))
```

## Workflows

1. User pastes a Gemini API key, a YouTube URL, and a question into one sidebar form.
2. On every Streamlit rerun (i.e. on every keystroke, since there is no submit gate), the video is re-indexed and the question re-answered.
3. The answer is printed as one hard-wrapped block under `ANSWER:`.

There is no chat history, no multi-turn, no saved sessions, no caching, no sharing, no export.

## Observed defects (evidence-based)

These are properties of the source as written, not redesign proposals.

| # | Defect | Evidence |
|---|---|---|
| D1 | **Submit button is created but never read.** The pipeline executes on `if query and youtube_url`, so the form submit does not gate anything. | `main.py:19` vs `main.py:22` |
| D2 | **Full re-index on every rerun.** Transcript download, chunking, embedding and FAISS construction repeat per keystroke. Costly and slow. | `main.py:22-24`, `uany.py:16-26` |
| D3 | **Retrieved sources discarded.** `get_response_from_query` returns `(response, docs)` but `docs` is dropped, so the user never sees which transcript chunks supported the answer. | `uany.py:65`, `main.py:24` |
| D4 | **Answer text mangled twice.** `response.replace("\n", "  ")` then `textwrap.fill(..., 80)`. Both destroy paragraph structure from the LLM. | `uany.py:63`, `main.py:26` |
| D5 | **Deprecated import paths.** `langchain.text_splitter` and `langchain.chains.LLMChain` are legacy; the modern equivalents are `langchain_text_splitters` and LCEL. | `uany.py:2`, `uany.py:6`, `uany.py:59` |
| D6 | **Retired embedding model.** `models/embedding-001` has been superseded by Google's newer embedding models. Likely to fail or be unsupported against the current API. | `uany.py:19` |
| D7 | **No error handling anywhere.** Any loader, quota, or network failure surfaces as a raw Streamlit traceback to the user. | `main.py:22-26` |
| D8 | **Unused dependencies.** `tiktoken` and `textwrap3` are declared but the code uses stdlib `textwrap`. | `requirements.txt:25-26`, `main.py:3` |
| D9 | **No empty-input guard for the API key.** `max_tokens=None`/`timeout=None` and an empty key fail deep inside the Google client. | `main.py:9`, `uany.py:34-42` |
| D10 | **Transcript variant is not requested.** `YoutubeLoader.from_youtube_url` is called without `add_video_dashes`, so it does not deliberately target auto-generated captions. Many videos only have auto captions. | `uany.py:17` |
| D11 | **Single fixed retrieval width.** `k=4` is hardcoded and not user-adjustable. | `uany.py:28` |

## Uncertainties (do not treat as fact)

- **U1** — Whether the author intended the submit button to gate execution, or deliberately wanted live updating. The unused variable suggests the former was intended; nothing documents it.
- **U2** — Whether `embedding-001` still resolves for this project. Cannot be confirmed without a live key.
- **U3** — Whether the author considers this a personal tool or a product intended for other users. This materially changes the trust boundary and therefore the whole architecture. The repo has no auth, no accounts, and no user model.
- **U4** — Intended transcript language handling. `readme.md:121` lists multilingual transcripts as a *future* improvement, so English-only appears to be the current intent.

## Migration drivers

1. Streamlit is a browser app; the target is a native Android app. The web layout should not be reproduced literally.
2. The Python stack (langchain + FAISS) is orchestration glue around four network calls: fetch transcript, embed, search, generate. None of it requires a server if the Gemini calls are made from the device with a user-supplied key.
3. D1, D2, D3, D4, D7 are all defects that a port should not carry forward. The port should be a clean reimplementation of the intended capability, not a transliteration.
