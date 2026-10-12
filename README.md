# ExamForge — AI-Powered College Examination Management System

ExamForge is an end-to-end examination platform built for higher education institutions. It provides role-specific portals for **College Management**, **Teachers (Faculty)**, and **Students**, featuring document-grounded AI question generation, server-authoritative proctored testing, auto-evaluation, and detailed analytics.

---

## 🚀 Key Features

* **Three Role-Specific Portals**:
  * **College Management (Admin)**: User provisioning, role assignment, departments, academic years, classes, exam scheduling, institutional analytics, and audit logging.
  * **Teacher (Faculty)**: Subject lesson management, PDF/TXT material uploads, deterministic blueprint formulation, AI question generation, question editing/approval, and class performance analytics with CSV export.
  * **Student**: Timed exam portal, server-synchronized countdown timer, question palette with review markers, instant autosave, auto-submission upon timeout, and topic/difficulty scorecards.
* **Grounded AI Question Generation**:
  * Generates questions strictly based on uploaded course syllabus documents (prevents AI hallucination).
  * Backend deterministic distribution arithmetic (percentages always sum to 100%).
  * Pluggable provider architecture: **Local Demo Provider** (zero-API-key mode), **Google Gemini 1.5 Flash**, and **OpenAI GPT-4o**.
* **Academic Integrity & Security**:
  * **Self-Registration & Authentication**: Users can create a new account (as Student, Teacher, or Management) or sign in with existing credentials.
  * Passwords hashed with BCrypt.
  * Stateless JWT authentication with role-based and resource-level authorization.
  * Server-enforced test windows and attempt deadlines.
  * Question answers (`isCorrect` and explanations) are completely stripped from student exam sessions until release.
  * Traversal-safe file storage and 15 MB file upload limit.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, Vite, TypeScript, Tailwind CSS, React Router 6, Axios, Recharts, Lucide Icons |
| **Backend** | Java 21, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Maven |
| **Database** | MySQL 8 / H2 (`MODE=MySQL` zero-dependency local dev), Flyway Migrations |
| **Document Processing** | Apache PDFBox 3.0.3 (text extraction with scanned PDF fallbacks) |
| **API Docs** | SpringDoc OpenAPI 2.6.0 / Swagger UI |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, Vitest, React Testing Library |

---

## 👥 Pre-Configured Demo Credentials

The platform is pre-seeded with sample departments, classes, subjects, lessons, blueprints, and users. Quick 1-click login buttons are also available directly on the login screen:

| Role | Email | Password | Pre-Assigned Subject / Class |
|---|---|---|---|
| **College Management** | `admin@college.edu` | `Admin@123` | Institutional Governance |
| **Teacher (Faculty)** | `teacher.sharma@college.edu` | `Teacher@123` | Database Management Systems (CS301) |
| **Student (Alex)** | `student.alex@college.edu` | `Student@123` | B.Tech CSE Year 3 — Section A |
| **Student (Priya)** | `student.priya@college.edu` | `Student@123` | B.Tech CSE Year 3 — Section A |

---

## 📁 Monorepo Structure

```
ExamForge/
├── backend/                      # Spring Boot 3.3.4 Java 21 backend
│   ├── src/main/java/com/examforge/
│   │   ├── ai/                   # AI providers (LocalDemo, Gemini, OpenAI, Factory, Math)
│   │   ├── config/               # Security & OpenAPI configuration
│   │   ├── controller/           # REST endpoints
│   │   ├── document/             # PDFBox parser & safe storage service
│   │   ├── dto/                  # Request / Response DTOs
│   │   ├── entity/               # JPA entities & Enums (22 models)
│   │   ├── exception/            # Centralized exception handlers
│   │   ├── repository/           # Spring Data JPA repositories
│   │   ├── security/             # JWT filter, UserPrincipal, SecurityUtils
│   │   └── service/              # Business logic & transaction boundaries
│   ├── src/main/resources/
│   │   ├── db/migration/         # Flyway schema V1 & demo data V2
│   │   ├── application.yml       # Base configuration
│   │   ├── application-dev.yml   # Zero-dependency H2 MySQL mode
│   │   └── application-mysql.yml # Containerized MySQL 8 mode
│   └── pom.xml
├── frontend/                     # React 18 + TypeScript + Vite frontend
│   ├── src/
│   │   ├── api/                  # Axios instance with JWT interceptor
│   │   ├── components/           # Common UI, Navbar, Sidebar, Modals, Badges
│   │   ├── context/              # AuthContext for session management
│   │   ├── pages/
│   │   │   ├── auth/             # LoginPage with 1-click demo logins
│   │   │   ├── management/       # Admin dashboard, users, academic, scheduling, audit
│   │   │   ├── teacher/          # Documents, blueprints, AI generation, review, analytics
│   │   │   ├── student/          # Dashboard, instructions, timed exam room, scorecards
│   │   │   └── common/           # 404 and Access Denied pages
│   │   ├── types/                # TypeScript interfaces
│   │   ├── App.tsx               # Route declarations & role guards
│   │   └── main.tsx              # DOM mounting
│   ├── package.json
│   └── vite.config.ts
├── database/
│   └── init.sql                  # MySQL initialization script
├── docs/
│   ├── ARCHITECTURE.md           # System design & database documentation
│   └── API_DOCUMENTATION.md      # Complete REST endpoints reference
├── docker-compose.yml            # Multi-container orchestration (MySQL + App)
├── .env.example                  # Environment variable reference
└── README.md
```

---

## 🏃 Getting Started Locally

### Prerequisites
* **Java 21** (JDK 21)
* **Apache Maven 3.9+**
* **Node.js 18+** & npm

---

### Step 1: Start Backend (Zero-Dependency Dev Mode)

The backend runs out-of-the-box using the `dev` profile with an embedded, persistent H2 database operating in MySQL compatibility mode. No local MySQL installation is required:

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

* Backend REST API will start on: **`http://localhost:8080`**
* Interactive Swagger UI: **`http://localhost:8080/swagger-ui.html`**

*(Optional)* If you prefer running MySQL 8 via Docker:
```powershell
docker compose up -d db
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

### Step 2: Start Frontend

In a separate terminal window:

```powershell
cd frontend
npm install
npm run dev
```

* Frontend application will start on: **`http://localhost:5173`**
* Requests to `/api` are automatically proxied to the backend at `http://localhost:8080`.

---

## 🧪 Verification & Automated Testing

### Backend Test Suite (JUnit 5 & Mockito)
Runs 9 comprehensive unit and integration tests covering deterministic blueprint calculations, local AI question generation, authentication, exam grading, and schedule validation:

```powershell
cd backend
mvn test
```

### Frontend Test Suite (Vitest & React Testing Library)
Runs component tests and verifying rendering of role selectors, badges, and layout components:

```powershell
cd frontend
npm run test
```

### Production Build Verification
Verifies TypeScript compilation and creates the production Vite bundle:

```powershell
cd frontend
npm run build
```

---

## 🤖 Configuring AI Providers

ExamForge supports three AI providers configured via environment variables or `application.yml`:

| Provider | Setting | Configuration |
|---|---|---|
| **Local Demo** *(Default)* | `AI_PROVIDER=LOCAL_DEMO` | Zero external API keys needed. Generates questions grounded in lesson notes with guaranteed topic/difficulty distributions. |
| **Google Gemini** | `AI_PROVIDER=GEMINI` | Set `GEMINI_API_KEY=your_key_here`. Uses `gemini-1.5-flash` with structured JSON schema output. |
| **OpenAI** | `AI_PROVIDER=OPENAI` | Set `OPENAI_API_KEY=your_key_here`. Uses `gpt-4o` with structured JSON schema output. |

---

## 📄 License

Proprietary — Developed for Educational Institutional Examination Management.
