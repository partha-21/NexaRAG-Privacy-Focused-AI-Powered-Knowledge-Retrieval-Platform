#!/usr/bin/env bash
# Starts Ollama check, then the app. Requires JDK 17+, Maven, and Ollama.
set -e
if ! command -v mvn >/dev/null; then echo "Maven not found. Install Maven or open the folder in IntelliJ/Eclipse/VS Code."; exit 1; fi
if command -v ollama >/dev/null; then
  ollama list | grep -q "llama3.2" || ollama pull llama3.2
else
  echo "Warning: Ollama not found. Install it from https://ollama.com (needed for answers)."
fi
mvn spring-boot:run
