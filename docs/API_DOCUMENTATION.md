# ExamForge — REST API Reference Guide

All protected endpoints require the HTTP header:
`Authorization: Bearer <jwt-access-token>`

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON is available at: `http://localhost:8080/v3/api-docs`

---

## 1. Authentication (`/api/v1/auth`)

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | Authenticates credentials and issues access & refresh tokens |
| POST | `/api/v1/auth/refresh` | Public | Exchanges valid refresh token for a new access token |
| POST | `/api/v1/auth/logout` | Authenticated | Revokes current session |
| GET | `/api/v1/auth/me` | Authenticated | Retrieves current authenticated user profile |
| POST | `/api/v1/auth/change-password` | Authenticated | Updates account password |

---

## 2. Institutional Administration (`/api/v1/management`)

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| GET | `/api/v1/management/stats` | MANAGEMENT | Institutional overview counters and status summaries |
| GET | `/api/v1/users` | MANAGEMENT | Filter and search all users |
| POST | `/api/v1/users` | MANAGEMENT | Provision new teacher or student account |
| PUT | `/api/v1/users/{id}` | MANAGEMENT | Update user details |
| PUT | `/api/v1/users/{id}/status` | MANAGEMENT | Activate or deactivate account |
| POST | `/api/v1/academic/departments` | MANAGEMENT | Create department |
| GET | `/api/v1/academic/departments` | Authenticated | List all departments |
| POST | `/api/v1/academic/classes` | MANAGEMENT | Create class |
| GET | `/api/v1/academic/classes` | Authenticated | List all classes |
| POST | `/api/v1/academic/enrollments` | MANAGEMENT | Enroll student into class |
| POST | `/api/v1/management/schedules` | MANAGEMENT | Schedule an approved examination |
| POST | `/api/v1/management/schedules/{id}/cancel` | MANAGEMENT | Cancel scheduled examination |
| POST | `/api/v1/management/schedules/{id}/release-results` | MANAGEMENT | Manually release results |
| GET | `/api/v1/audit-logs` | MANAGEMENT | View administrative audit logs |

---

## 3. Teacher Material & Blueprints (`/api/v1/teacher`)

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| GET | `/api/v1/teacher/assigned-subjects` | TEACHER | List subjects assigned to current faculty member |
| GET | `/api/v1/subjects/{subjectId}/lessons` | TEACHER, MANAGEMENT | List lessons within a subject |
| POST | `/api/v1/subjects/{subjectId}/lessons` | TEACHER | Create new lesson topic |
| POST | `/api/v1/documents/upload` | TEACHER | Upload PDF/TXT lesson document (multipart/form-data) |
| GET | `/api/v1/documents/lesson/{lessonId}` | TEACHER | List documents associated with a lesson |
| DELETE | `/api/v1/documents/{documentId}` | TEACHER | Delete an unreferenced document |
| POST | `/api/v1/blueprints` | TEACHER | Create exam design blueprint |
| GET | `/api/v1/blueprints/subject/{subjectId}`| TEACHER | List blueprints for subject |
| GET | `/api/v1/blueprints/{id}` | TEACHER | Get blueprint details with lesson allocation |

---

## 4. AI Question Generation & Paper Review

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| POST | `/api/v1/ai/generate` | TEACHER | Generate question paper from blueprint via AI |
| GET | `/api/v1/exam-papers/{id}` | TEACHER, MANAGEMENT | Retrieve complete question paper |
| PUT | `/api/v1/exam-papers/{id}` | TEACHER | Update paper title, instructions, or duration |
| PUT | `/api/v1/exam-papers/{paperId}/questions/{questionId}` | TEACHER | Edit question text, marks, options, or explanation |
| POST | `/api/v1/exam-papers/{paperId}/questions/{questionId}/regenerate` | TEACHER | AI-regenerate a single question |
| POST | `/api/v1/exam-papers/{id}/approve` | TEACHER | Approve paper and lock against modifications |

---

## 5. Student Examination Engine (`/api/v1/student`)

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| GET | `/api/v1/student/available-schedules` | STUDENT | List upcoming/active exams for student's class |
| POST | `/api/v1/student/exams/{scheduleId}/start` | STUDENT | Start or resume proctored exam session |
| POST | `/api/v1/student/exams/{scheduleId}/save-answer` | STUDENT | Autosave option selection or text answer |
| POST | `/api/v1/student/exams/{scheduleId}/submit` | STUDENT | Submit exam attempt and trigger evaluation |

---

## 6. Results & Analytics (`/api/v1/results`)

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| GET | `/api/v1/results/my-history` | STUDENT | View history of student attempts and release status |
| GET | `/api/v1/results/attempt/{attemptId}` | STUDENT, TEACHER, MGMT | View detailed scorecard and question review |
| GET | `/api/v1/results/class/{scheduleId}` | TEACHER, MANAGEMENT | Get aggregated class performance analytics |
| GET | `/api/v1/results/class/{scheduleId}/csv` | TEACHER, MANAGEMENT | Download CSV export of exam marks |
