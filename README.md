# GuardianIQ — AI Personal Assistant with PDF-Based RAG

<p align="center">
  <strong>An AI-powered personal assistant for conversational Q&A, document-grounded answers, and daily parent/guardian summaries.</strong>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-Spring%20Boot-orange" />
  <img alt="Backend" src="https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen" />
  <img alt="AI" src="https://img.shields.io/badge/AI-LangChain4j-blue" />
  <img alt="Vector database" src="https://img.shields.io/badge/Vector%20DB-Pinecone-purple" />
  <img alt="Database" src="https://img.shields.io/badge/Database-MySQL-blue" />
  <img alt="Frontend" src="https://img.shields.io/badge/Frontend-HTML%20%7C%20CSS%20%7C%20JavaScript-yellow" />
</p>

GuardianIQ is a web-based AI personal assistant built with **Java and Spring Boot**. Users can ask questions in a conversational interface and upload PDF documents to ask questions about their contents. Its document-question-answering flow uses **Retrieval-Augmented Generation (RAG)**, with Pinecone providing vector search so responses can be grounded in relevant document passages. The application also includes account authentication, conversation history, and scheduled daily email summaries for a registered parent or guardian.

> **Project status:** Academic/prototype project. External services and email reporting require valid credentials and configuration before they can run locally. Review privacy, consent, and child-safety requirements before deploying this application for real users.

## Contents

- [Highlights](#highlights)
- [Technology stack](#technology-stack)
- [How it works](#how-it-works)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup and installation](#setup-and-installation)
- [Configuration](#configuration)
- [Run the application](#run-the-application)
- [Using GuardianIQ](#using-guardiandiq)
- [API overview](#api-overview)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)
- [Future improvements](#future-improvements)
- [Contributing](#contributing)
- [License](#license)

## Highlights

### Conversational AI

- Chat with an AI assistant through the web interface.
- Send chat requests to the Spring Boot backend, which connects to an LLM through LangChain4j and an OpenAI-compatible API endpoint.
- Keep conversation records and review previous messages.

### PDF question answering with RAG

- Upload a PDF and extract its text.
- Split extracted text into smaller chunks suitable for retrieval.
- Store/search document representations through Pinecone.
- Retrieve relevant document content when a question is asked and use it as context for the AI response.
- Support questions that refer to the uploaded document instead of relying only on the model's general knowledge.

**RAG flow:**

```text
PDF upload
   │
   ▼
Extract text ──► Split into chunks ──► Create/store searchable representations
                                             │
                                             ▼
User asks a question ──► Retrieve relevant chunks
                                             │
                                             ▼
                                  LLM generates an answer
                                             │
                                             ▼
                                      Show the answer
```

The quality of document-grounded answers depends on PDF text extraction, chunking, retrieval relevance, and the configured language model. A RAG response is not a guarantee that every answer is correct; users should verify important information against the original document.

### Accounts and conversation history

- Registration and login endpoints.
- Spring Security-based request protection and user authentication.
- Conversation and message persistence in MySQL.
- Conversation-history functionality for returning to previous chats.

### Daily parent/guardian summaries

- Scheduled reporting using Spring's scheduling support.
- Email summaries of a user's AI activity to the configured parent/guardian email address, subject to correct SMTP configuration and the application's reporting logic.
- Email delivery requires a working SMTP account and valid credentials.

### Web interface

- Static HTML, CSS, and JavaScript frontend served by the Spring Boot application.
- Login/registration interface and chat dashboard.
- PDF upload and document-question-answering workflow.

## Technology stack

| Area | Technology | Purpose |
|---|---|---|
| Language | Java | Application and backend logic |
| Backend framework | Spring Boot | REST endpoints, application configuration, and service wiring |
| Security | Spring Security | Authentication and endpoint protection |
| AI integration | LangChain4j | Connects application logic to the language model |
| Language model API | OpenRouter using an OpenAI-compatible endpoint | Generates conversational and RAG-assisted responses |
| Retrieval approach | Retrieval-Augmented Generation (RAG) | Supplies relevant document passages as context to the model |
| Vector search | Pinecone | Stores/searches document representations for retrieval |
| Relational database | MySQL | Stores user, conversation, and message records |
| Data access | Spring Data JPA / project-configured persistence | Relational data access |
| Frontend | HTML, CSS, JavaScript | Browser interface and client-side interactions |
| Email | Spring JavaMailSender / SMTP | Sends parent/guardian summaries |
| Scheduling | Spring scheduling (`@Scheduled`) | Triggers periodic daily reporting |
| Build | Maven and Maven Wrapper | Dependency management and build/run commands |

> Check `pom.xml` for the exact Java and dependency versions used by your checkout.

## How it works

1. **Sign in:** A user registers or logs in through the web interface.
2. **Ask a question:** The browser sends the chat request to the Spring Boot backend.
3. **Process the request:** The backend handles the conversation and calls the configured language model through LangChain4j.
4. **Use a PDF as context:** For document questions, the RAG service searches Pinecone for relevant chunks and supplies the retrieved context to the language model.
5. **Save the conversation:** User messages and assistant responses are persisted in the relational database according to the application's conversation logic.
6. **Send daily summaries:** The scheduled reporting service prepares and emails a daily activity summary using the configured mail account.

## Architecture

```text
┌──────────────────────────────────┐
│ Browser                          │
│ HTML · CSS · JavaScript          │
└────────────────┬─────────────────┘
                 │ HTTP requests
                 ▼
┌──────────────────────────────────┐
│ Spring Boot application          │
│                                  │
│ Controllers · Security           │
│ Chat service · PDF service       │
│ Conversation/history services    │
│ RAG service · Scheduled reports  │
└───────┬────────────┬─────────┬───┘
        │            │         │
        ▼            ▼         ▼
   ┌─────────┐  ┌──────────┐  ┌──────────────┐
   │ MySQL   │  │ Pinecone │  │ OpenRouter   │
   │ users & │  │ vector   │  │ language     │
   │ chats   │  │ search   │  │ model API    │
   └─────────┘  └──────────┘  └──────────────┘
        │
        ▼
   ┌──────────────────────────────┐
   │ SMTP email service            │
   │ Daily parent/guardian report  │
   └──────────────────────────────┘
```

## Project structure

```text
ai-personal-assistant/
├── .mvn/                                  # Maven Wrapper support files
├── mvnw                                   # Maven Wrapper (Unix-like systems)
├── mvnw.cmd                               # Maven Wrapper (Windows)
├── pom.xml                                # Maven dependencies and build configuration
└── src/
    └── main/
        ├── java/com/aimonitor/aipersonalassistant/
        │   ├── AiPersonalAssistantApplication.java
        │   ├── config/                     # Security, Pinecone, scheduling, etc.
        │   ├── controller/                 # Authentication, chat, and PDF endpoints
        │   ├── dto/                        # Request/response data objects
        │   ├── entity/                     # Database entities
        │   ├── repository/                 # Data access repositories
        │   └── service/                    # Chat, RAG, PDF, history, reports, etc.
        └── resources/
            ├── static/
            │   ├── css/                    # Stylesheets
            │   ├── js/                     # Frontend JavaScript
            │   ├── index.html              # Entry/authentication page
            │   └── dashboard.html          # Chat dashboard
            └── application.properties      # Local configuration (not committed)
```

*The layout above reflects the current source structure; individual classes and features may change as the project evolves.*

## Prerequisites

Install or create accounts for the services you plan to use:

- **Java JDK:** A version compatible with the Java version configured in `pom.xml`.
- **MySQL Server:** A local or hosted MySQL instance.
- **Pinecone account:** An index configured for this application's retrieval implementation.
- **OpenRouter API key:** Access to the selected model configured in the application.
- **SMTP email account:** Required for daily parent/guardian report delivery. For Gmail, use an app password where required; do not use your normal account password.
- **Git:** Optional for cloning and version control. Maven Wrapper scripts are included, so a separate Maven installation is generally unnecessary.

## Setup and installation

### 1. Clone the repository

```bash
git clone https://github.com/NEHRU22/GuardianIQ.git
cd GuardianIQ
```

### 2. Create the MySQL database

Open MySQL and create the database expected by the application:

```sql
CREATE DATABASE ai_personal_assistant;
```

If your local configuration uses another database name, make the database and JDBC URL match.

### 3. Configure local credentials

Create this file locally:

```text
src/main/resources/application.properties
```

Use the configuration keys below as a starting point. Replace every placeholder with your own values and confirm the property names against the application's `PineconeConfig` and service code. **Do not commit this file or any real API keys/passwords.**

```properties
spring.application.name=ai-personal-assistant
server.port=8080

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ai_personal_assistant
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

# OpenRouter / OpenAI-compatible endpoint
langchain4j.open-ai.chat-model.base-url=https://openrouter.ai/api/v1
langchain4j.open-ai.chat-model.api-key=YOUR_OPENROUTER_API_KEY
langchain4j.open-ai.chat-model.model-name=nvidia/nemotron-3.5-lightning:free
langchain4j.open-ai.chat-model.timeout=120s

# Pinecone — use the host for your own index
pinecone.api-key=YOUR_PINECONE_API_KEY
pinecone.index-host=YOUR_PINECONE_INDEX_HOST
pinecone.namespace=pdf-documents

# SMTP / email (Gmail example)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL_ADDRESS
spring.mail.password=YOUR_EMAIL_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

**Configuration notes:**

- The model identifier above is the one previously used by this project; confirm that it is available to your OpenRouter account before running.
- Set `pinecone.index-host` to your own Pinecone index host. Ensure the index and the application's vector-writing/searching implementation are compatible.
- The application may derive a namespace from the authenticated user. If it does, make sure the configured default namespace matches your implementation's expectations.
- If your existing configuration includes additional properties, retain the required ones locally. Avoid copying personal credentials into README examples, screenshots, issues, or commits.

### 4. Build the application

**Windows Command Prompt:**

```cmd
mvnw.cmd clean package
```

**PowerShell:**

```powershell
.\mvnw.cmd clean package
```

**macOS/Linux:**

```bash
./mvnw clean package
```

If dependency resolution fails, verify your internet connection, Java version, and Maven configuration.

## Run the application

From the project root:

**Windows:**

```cmd
mvnw.cmd spring-boot:run
```

**macOS/Linux:**

```bash
./mvnw spring-boot:run
```

When the application starts successfully, open:

- **Application:** http://localhost:8080/
- **Dashboard:** http://localhost:8080/dashboard.html

The exact landing page and route behavior depend on the application's current Spring Security and frontend configuration. Check the startup log if the port or route differs.

## Using GuardianIQ

1. Start the application and open it in a browser.
2. Register an account or log in.
3. Start a conversation and ask the assistant a question.
4. Open the PDF question-answering/upload flow and upload a supported PDF.
5. Ask a question about the uploaded PDF and review whether the response is supported by its content.
6. Configure a parent/guardian email address and valid SMTP details if you want to test scheduled daily summaries.

Use non-sensitive sample documents while testing. Do not upload confidential, personal, educational, financial, medical, or other sensitive documents to external AI/vector services unless you have verified the applicable data handling and obtained the required permissions.

## API overview

The project includes controller endpoints for authentication, chat, and PDF workflows. The known controller groups are:

| Controller | Responsibility |
|---|---|
| `AuthController` | Registration and login flows |
| `ChatController` | Chat requests and conversation-related operations |
| `PdfController` | PDF upload/document question-answering operations |

The precise paths, HTTP methods, request fields, and response formats are defined in these controller classes. Check the current source before integrating an external client; this README intentionally does not guess endpoint paths that may have changed.

## Security notes

- **Never commit secrets.** Keep `src/main/resources/application.properties`, `cookies.txt`, `.env` files, API keys, SMTP passwords, and database credentials out of version control.
- Before pushing changes, inspect `git status` and review staged files with `git diff --cached --stat` and `git diff --cached`.
- If a credential was ever committed or pushed, deleting the file in a later commit is not enough. Revoke/rotate the exposed credential and clean repository history if needed.
- Use HTTPS and appropriate authentication/authorization for deployed environments. Do not expose a development instance directly to the public internet.
- Parent/guardian reports can disclose sensitive conversation data. Before real-world use—especially with minors—design clear notice, appropriate consent, data minimization, access controls, retention/deletion policies, and secure email handling. A prototype's implementation should not be treated as a complete legal or safety compliance solution.
- Configure database access with a dedicated least-privilege account instead of using a database administrator account in production.
- Keep dependencies patched and avoid logging credentials or entire sensitive conversations.

## Troubleshooting

| Problem | What to check |
|---|---|
| Application fails to start | Confirm the Java version, review the startup logs, and verify `application.properties`. |
| MySQL connection error | Check that MySQL is running, the database exists, and the JDBC URL/username/password are correct. |
| Language-model request fails | Verify the OpenRouter API key, model identifier, network connectivity, API availability, and configured timeout. |
| PDF answers have little or no relevance | Confirm that text was extracted from the PDF, chunks were stored successfully, retrieval uses the expected namespace, and the question relates to the document. Scanned PDFs may require OCR support if text extraction produces no usable text. |
| Pinecone request fails | Verify the index host, API key, index configuration, and namespace behavior. Never paste the API key into public logs/issues. |
| Email report is not received | Check SMTP host/port, app password, recipient address, scheduler configuration, email-provider restrictions, and application logs. Check spam/junk folders too. |
| Login or dashboard route is blocked | Review `SecurityConfig`, authentication state, frontend request URLs, and browser developer-console/network errors. |

## Future improvements

Potential next steps for the project include:

- Add automated tests for authentication, chat history, PDF ingestion, retrieval, and scheduled reporting.
- Add a documented API specification and request/response examples.
- Move secrets to environment variables or a secret manager instead of a local properties file.
- Add document management controls, including listing, deleting, and re-indexing uploaded PDFs.
- Improve citation/source display so users can see which PDF passages informed an answer.
- Add stronger file validation, upload limits, and tests for malformed or scanned PDFs.
- Provide configurable report schedules, retention controls, and transparent user/guardian settings.
- Add Docker-based local development and a CI workflow for builds and tests.

## Contributing

Contributions and suggestions are welcome.

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Make and test your changes.
4. Commit your work with a clear message.
5. Open a pull request describing the change and how it was tested.

Please never include credentials, cookies, private documents, or other sensitive data in a pull request.

## License

No license is currently specified in this repository. Unless a license is added, all rights remain with the copyright holder and reuse or redistribution is not automatically granted. Add a `LICENSE` file if you want to explicitly permit others to use, modify, or distribute the project.

---

<p align="center">Built as a learning project exploring conversational AI, Spring Boot, vector search, and document-grounded question answering.</p>
