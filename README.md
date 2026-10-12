# NexaRAG — Privacy-Focused AI-Powered Knowledge Retrieval Platform

NexaRAG is a privacy-focused document question-answering platform that enables users to upload documents and retrieve relevant information using Retrieval-Augmented Generation (RAG). The full application runs locally, combining semantic search with a locally hosted language model to generate answers grounded in document content.

## Live Demo

**[Open NexaRAG Live Demo](YOUR_VERCEL_LIVE_DEMO_URL)**

> **Demo Notice:** The live link is a browser-based demonstration of the user interface and basic document retrieval workflow. It uses sample documents and keyword-based matching. The complete Spring Boot backend, semantic embedding pipeline, and local AI model are not deployed on Vercel. The demo does not represent the full functionality of the application.

## Project Overview

NexaRAG is designed to provide document-based knowledge retrieval without depending on cloud-based AI services during local application use. Users can add documents, ask questions about their content, and receive answers accompanied by source references.

The complete application uses a local embedding model to represent document content as vectors and a local Ollama language model to generate context-aware answers.

## Key Features

- **Document Upload:** Upload supported documents, including PDF, DOCX, and TXT files.
- **Semantic Search:** Retrieve relevant document passages using vector embeddings.
- **Context-Aware Answers:** Generate responses grounded in retrieved document content.
- **Source Citations:** Display source filenames, relevant excerpts, and similarity scores.
- **Automatic Indexing:** Monitor document folders and update the index when files change.
- **Persistent Index:** Store the document index locally and reload it after application restarts.
- **Authentication:** Support account registration and login with BCrypt password hashing and session-based authentication.
- **Privacy-Focused Processing:** Keep document indexing and language-model inference on the local machine.

## Technology Stack

- **Language:** Java 17
- **Backend:** Spring Boot
- **AI Framework:** LangChain4j
- **Embedding Model:** all-MiniLM-L6-v2 using ONNX
- **Local Language Model:** Ollama
- **Document Processing:** Apache Tika
- **Security:** Spring Security, BCrypt, HTTP-only session cookies
- **Testing:** JUnit and Spring Boot Test
- **Demo Hosting:** Vercel

## How It Works

1. **Document ingestion:** Documents are uploaded or placed in the configured documents folder.
2. **Text extraction:** Supported files are processed to extract their text content.
3. **Embedding generation:** The text is divided into chunks and converted into vector embeddings locally.
4. **Relevant passage retrieval:** The user's question is compared with indexed content to find relevant passages.
5. **Answer generation:** The retrieved passages are provided to the local Ollama model as context.
6. **Source attribution:** The application returns the answer with source information to help users verify the retrieved content.

## Live Demo vs. Full Application

| Capability | Vercel Demo | Full Local Application |
|---|---|---|
| Web interface | Yes | Yes |
| Sample documents | Yes | Yes |
| Upload supported files | Yes, limited browser-side formats | Yes, supported document formats |
| Question answering | Keyword-based matching | RAG with semantic retrieval and a local LLM |
| Vector embeddings | No | Yes |
| Ollama integration | No | Yes |
| Spring Boot backend | No | Yes |
| Source references | Demo citations | Retrieved document sources and excerpts |
| Local document processing | Browser-only demo processing | Local backend processing |

## Project Structure

```text
offline-rag-spring/
├── demo/                 # Static browser-based demo for Vercel
├── docs/                 # Sample documents and project notes
├── src/
│   ├── main/
│   │   ├── java/         # Spring Boot application and RAG services
│   │   └── resources/    # Application configuration and UI
│   └── test/             # Automated tests
├── pom.xml               # Maven configuration
├── run.bat               # Windows startup script
├── run.sh                # Linux/macOS startup script
└── README.md
```

## Privacy and Limitations

The full application is designed for local processing. Its document index, account data, and model interactions are handled locally rather than by a hosted AI API.

The Vercel demo is a separate, simplified implementation. It uses browser-side keyword matching and does not execute the full RAG pipeline or connect to Ollama.

The current implementation is intended for personal and demonstration use. Production deployment would require additional considerations, including per-user document isolation, persistent production-grade storage, and security hardening.

## License

See the [LICENSE](LICENSE) file for licensing information.
