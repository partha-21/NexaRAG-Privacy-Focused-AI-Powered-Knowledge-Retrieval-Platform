# Live demo (static, for Vercel / GitHub Pages)

This folder is a browser-only version of the Offline Document Assistant. It uses the same interface
as the real app, but the Spring Boot backend and the local language model are replaced by a small mock
that runs in the page:

- Three sample documents are preloaded; you can also upload your own `.txt`, `.md`, `.csv` or `.json` files.
- Questions are answered by keyword matching in the browser and shown with source citations.
- No AI model, no server and no data leaves your browser.
- Sign-in is skipped in the demo.

The real project (`../`) runs fully offline with Spring Boot, LangChain4j, ONNX embeddings and an Ollama model.

## Deploy on Vercel
1. Push this repository to GitHub.
2. In Vercel: **Add New > Project**, import the repository.
3. Set **Root Directory** to `demo`, and **Framework Preset** to `Other`. Leave build and output settings empty.
4. Deploy. You get a link like `https://offline-rag-demo.vercel.app`.

CLI alternative:
```bash
cd demo
npx vercel --prod
```

## Before you publish
- In `index.html`, replace `YOUR-USERNAME` in the banner link with your GitHub username.
- Open the page locally by double-clicking `index.html`, or run `npx serve demo`.
