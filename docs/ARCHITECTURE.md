# ExamForge — System Architecture & Design Specification

ExamForge is an enterprise-grade AI-powered Examination Management Platform designed for higher education institutions. It enforces a strict separation of concerns, multi-role access boundaries, grounded AI question generation, server-authoritative timed examination workflows, and automated evaluation.

---

## 1. System Topology & Architectural Layers

```
                                  +---------------------------------------+
                                  |         React 18 + Vite (SPA)         |
                                  |  Tailwind CSS + Recharts + TS Client  |
                                  +---------------------------------------+
                                                     |
                                            HTTPS / REST (JWT)
                                                     v
+---------------------------------------------------------------------------------------------------------+
|                                    Spring Boot 3.3.4 Backend (Java 21)                                  |
|                                                                                                         |
|  +--------------------+   +---------------------+   +---------------------+   +---------------------+   |
|  | Security & Auth    |   | Document Engine     |   | AI Orchestrator     |   | Exam Engine         |   |
|  | - Spring Security  |   | - PDFBox 3.x        |   | - LocalDemoProvider |   | - Server Timer Sync |   |
|  | - JWT Filter       |   | - Traversal Guard   |   | - Gemini Provider   |   | - Auto-saving       |   |
|  | - RBAC Guard       |   | - 15MB Size Guard   |   | - OpenAI Provider   |   | - Auto-Evaluation   |   |
|  +--------------------+   +---------------------+   +---------------------+   +---------------------+   |
|                                                     |                                                   |
|  +--------------------------------------------------+------------------------------------------------+  |
|  |                       Service Layer Business Logic & Transaction Boundaries                        |  |
|  +----------------------------------------------------------------------------------------------------+  |
|                                                     |                                                   |
|  +--------------------------------------------------+------------------------------------------------+  |
|  |                       Spring Data JPA Repositories & Hibernate Persistence                         |  |
|  +----------------------------------------------------------------------------------------------------+  |
+---------------------------------------------------------------------------------------------------------+
                                                     |
                                   Flyway DB Migrations (V1, V2)
                                                     v
                                  +---------------------------------------+
                                  |    Database Persistence Tier          |
                                  |    - Dev Mode: Embedded H2 (MySQL)    |
                                  |    - Prod Mode: MySQL 8 Container     |
                                  +---------------------------------------+
```

---

## 2. Core Modules

### 2.1 Security & Authentication Layer
- **Stateless JWT**: Bearer tokens signed with HMAC-SHA256 (`jjwt-api 0.12.6`).
- **Role Hierarchy**:
  - `MANAGEMENT`: College administrative governance, user provisioning, teacher-subject mappings, scheduling, audit inspection.
  - `TEACHER`: Syllabus document ingestion, blueprint formulation, AI question drafting, review, approval, analytics.
  - `STUDENT`: Proctored exam participation, synchronized countdown, autosaved responses, scorecard analytics.
- **Resource Authorization**:
  - Teachers may only access documents, blueprints, and papers belonging to subjects assigned to them.
  - Students cannot view questions until active window start; questions never leak answers (`isCorrect` is stripped).
  - Attempt results are hidden until the exam's `ResultReleasePolicy` is satisfied.

### 2.2 Document Processing Engine
- **Text Extraction**: Uses Apache PDFBox 3.0.3 to safely parse instructional documents into textual lesson chunks.
- **Security Protections**:
  - Path traversal defense: Normalizes paths and verifies `file.getCanonicalPath().startsWith(storageDir.getCanonicalPath())`.
  - Configurable upload limits (15 MB per file).
  - Supported formats: `.pdf` and `.txt`. Scanned image PDFs without selectable text trigger a friendly notification indicating OCR is not enabled.

### 2.3 Grounded AI Question Generation Pipeline
- **Blueprint Arithmetic**: Deterministic rounding algorithm guarantees that question type, topic, and difficulty percentage distributions sum precisely to 100% of the total question count.
- **Anti-Hallucination Grounding**: Context sent to AI providers is strictly constrained to the text extracted from the selected lesson documents.
- **Pluggable AI Providers**:
  - `LOCAL_DEMO`: Zero-external-dependency rule-based generation grounded in lesson content. Ideal for instant local setups, unit tests, and air-gapped demo environments.
  - `GEMINI`: Google Gemini 1.5 Flash structured JSON generation via Gemini REST API.
  - `OPENAI`: OpenAI GPT-4o / GPT-4o-mini structured JSON generation.
- **Draft & Review State Machine**: Generated questions enter `DRAFT` status and cannot be scheduled until a teacher reviews, edits, and marks the exam paper as `APPROVED`. Once approved, papers are locked against modifications.

### 2.4 Server-Authoritative Exam Taking Engine
- **Deadline Enforcment**: Exam expiration is calculated as `min(attemptStartTime + durationMinutes, schedule.endTime)`. Client device clock manipulation cannot alter the deadline.
- **Autosave Pipeline**: Every option selection or text input automatically persists to the server via debounced/immediate POST requests.
- **Auto-Submission**: When the countdown reaches zero or the server detects deadline expiry, the attempt status transitions to `SUBMITTED` or `TIMED_OUT` and is sent to evaluation.
- **Objective Evaluation**: Multiple Choice (MCQ) and True/False questions are automatically evaluated immediately upon submission, computing marks and topic-wise breakdown scores.

---

## 3. Database Schema Design (22 Entities)

1. `users`: Core account details, credentials, and activation state.
2. `roles`: Role definitions (`MANAGEMENT`, `TEACHER`, `STUDENT`).
3. `user_roles`: User-role join table.
4. `departments`: Institutional departments (e.g., Computer Science).
5. `academic_years`: Institutional calendar years.
6. `classes`: Academic classes (e.g., B.Tech CSE Year 3).
7. `sections`: Subsections (e.g., Section A).
8. `student_enrollments`: Mapping of students to class and academic year.
9. `subjects`: Courses of study.
10. `teacher_subject_assignments`: Faculty assignments to subjects.
11. `lessons`: Course modules / units.
12. `documents`: Uploaded lesson documents and metadata.
13. `document_processing_jobs`: Document parsing state tracking.
14. `exam_blueprints`: Teacher exam design specifications.
15. `blueprint_lesson_selections`: Lesson allocation per blueprint.
16. `questions`: Question bank with text, difficulty, and type.
17. `question_options`: Distractor and correct options for MCQs.
18. `exam_papers`: Assembled question papers.
19. `exam_paper_questions`: Paper question ordering and marks allocation.
20. `exam_schedules`: Timed examination schedules and release policies.
21. `exam_attempts`: Student test sessions and scores.
22. `student_answers`: Autosaved and submitted student responses.
23. `audit_logs`: Audit trail of administrative and teacher operations.
24. `notifications`: System notifications for users.
