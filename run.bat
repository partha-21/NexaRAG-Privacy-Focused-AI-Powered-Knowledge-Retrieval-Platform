@echo off
where mvn >nul 2>nul || (echo Maven not found. Install Maven or open this folder in IntelliJ/Eclipse/VS Code. & exit /b 1)
where ollama >nul 2>nul || echo Warning: Ollama not found. Install it from https://ollama.com
ollama list | findstr /C:"llama3.2" >nul || ollama pull llama3.2
mvn spring-boot:run
