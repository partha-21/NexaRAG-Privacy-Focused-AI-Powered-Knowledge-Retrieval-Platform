# Offline Document Assistant (Spring Boot + LangChain4j, no cloud)

Upload your own documents (PDF, DOCX, TXT...) and ask questions about them.
Embeddings run inside the JVM and the answers come from a local Ollama model,
so nothing leaves your machine at runtime.

## Features
- Spring Boot REST API and a simple web page (upload, list, delete, chat)
- Persistent index: saved to `data/`, reloaded on startup if your docs haven't changed
- Citations: every answer lists the source file, an excerpt and a similarity score
- Folder watching: drop files into `docs/` and the index updates by itself
- Unit tests for chunking and retrieval quality (no Ollama or network needed)

## Requirements
- JDK 17+
- Maven
- [Ollama](https://ollama.com) installed and running

## Run
```bash
ollama pull llama3.2          # one-time download (needs internet only here)
mvn spring-boot:run
# open http://localhost:8080
```
Maven needs internet the first time to download dependencies; after that the app runs offline.

Run the tests:
```bash
mvn test
```

## REST API
| Method | Path | Description |
|---|---|---|
| GET | `/api/documents` | List indexed files |
| POST | `/api/upload` | Multipart upload, field name `file` |
| DELETE | `/api/documents/{name}` | Remove a file |
| POST | `/api/ask` | Body `{"question": "..."}` returns answer plus sources |

## Config (`application.properties`)
`rag.docs-dir`, `rag.data-dir`, `rag.model`, `rag.ollama-url`, `rag.chunk-size`, `rag.chunk-overlap`

## Design notes and known limits
- Any change to the docs folder rebuilds the whole index. Simple and correct for personal-size
  collections; per-file incremental updates would need a store that supports delete-by-metadata.
- The vector store is LangChain4j's in-memory store saved to JSON. For large collections, swap in
  a database-backed store (H2/SQLite with vectors, Lucene, or pgvector).
- Citations show file name and excerpt. Page numbers would need a PDF parser that keeps page
  metadata (for example PDFBox page by page).
- Not yet compiled or run by the author of this scaffold: if a LangChain4j API name differs in your
  version, adjust the imports. Newer releases renamed `ChatLanguageModel` to `ChatModel`.

## Quick start for a demo
1. Install JDK 17+, Maven and Ollama.
2. Run `run.sh` (Mac/Linux) or `run.bat` (Windows). It pulls the model if missing and starts the app.
3. Open http://localhost:8080. Two sample files in `docs/` are indexed automatically.
4. Try asking: "What embedding model does this project use?"

## Opening in an IDE
- IntelliJ IDEA: File > Open > select the folder (it detects pom.xml). Run `RagApplication`.
- Eclipse: File > Import > Existing Maven Projects.
- VS Code: install the "Extension Pack for Java", open the folder, run `RagApplication`.

## Authentication (login / create account)
The app now requires an account. On first start, open the page, choose **Create account**, then sign in.

- Passwords are hashed with BCrypt and stored in `data/users.json` (never in plain text).
- Sessions use an HTTP-only cookie (8 hour timeout); CSRF protection is enabled.
- After 5 wrong passwords a username is locked for 5 minutes.
- Usernames are case-insensitive; passwords must be 8-64 characters.
- To stop others registering, set `rag.allow-registration=false` after creating your account.
- To reset everything, stop the app and delete `data/users.json`.
- Over HTTPS (for example behind a reverse proxy), also set `server.servlet.session.cookie.secure=true`.

API: `POST /api/auth/register` (JSON), `POST /api/auth/login` (form fields `username`, `password`),
`POST /api/auth/logout`, `GET /api/auth/me`. All other `/api/**` endpoints return 401 until you sign in.

Known limits: all signed-in users share the same document library (per-user libraries are a good next step),
and accounts live in a JSON file, which suits a personal or demo deployment rather than many users.

## Live demo
The `demo/` folder is a static, browser-only version of the UI with sample documents and keyword-matched
answers (no AI model). It deploys to Vercel as-is; see `demo/README.md`. The full offline app is this project.

## Pushing to GitHub
```bash
git init
git add .
git commit -m "Offline document assistant"
git branch -M main
git remote add origin https://github.com/<your-username>/offline-rag-spring.git
git push -u origin main
```
Keep private documents out of `docs/` (or add them to `.gitignore`) before pushing. The `data/` folder, which holds
user accounts and the index, is already ignored. The CI workflow in `.github/workflows/ci.yml` runs `mvn test` on every push.
