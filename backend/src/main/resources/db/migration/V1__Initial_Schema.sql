-- V1__Initial_Schema.sql: Complete ExamForge Schema for MySQL 8 and H2 MySQL Mode

-- 1. Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL, -- MANAGEMENT, TEACHER, STUDENT
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    phone VARCHAR(30),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- 2. Departments
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Academic Years
CREATE TABLE academic_years (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year_name VARCHAR(50) NOT NULL UNIQUE, -- e.g. "2026-2027"
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Classes (Programs/Batches)
CREATE TABLE classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    semester INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

CREATE INDEX idx_classes_dept ON classes(department_id);

-- 5. Sections
CREATE TABLE sections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_section_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_section UNIQUE (class_id, name)
);

-- 6. Student Enrollments
CREATE TABLE student_enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    roll_number VARCHAR(50) NOT NULL UNIQUE,
    academic_year_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_enroll_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
    CONSTRAINT fk_enroll_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE
);

CREATE INDEX idx_enroll_student ON student_enrollments(student_id);
CREATE INDEX idx_enroll_section ON student_enrollments(section_id);

-- 7. Subjects
CREATE TABLE subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    credits INT DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subject_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

CREATE INDEX idx_subjects_dept ON subjects(department_id);

-- 8. Teacher-Subject Assignments
CREATE TABLE teacher_subject_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    section_id BIGINT,
    academic_year_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tsa_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tsa_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_tsa_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_tsa_ay FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE
);

CREATE INDEX idx_tsa_teacher ON teacher_subject_assignments(teacher_id);
CREATE INDEX idx_tsa_subject ON teacher_subject_assignments(subject_id);

-- 9. Lessons / Units
CREATE TABLE lessons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    unit_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_index INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesson_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
);

CREATE INDEX idx_lessons_subject ON lessons(subject_id);

-- 10. Documents
CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(100),
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    extracted_text LONGTEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_uploader FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_doc_lesson ON documents(lesson_id);
CREATE INDEX idx_doc_subject ON documents(subject_id);
CREATE INDEX idx_doc_status ON documents(processing_status);

-- 11. Exam Blueprints
CREATE TABLE exam_blueprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    total_questions INT NOT NULL,
    duration_minutes INT NOT NULL,
    max_marks DECIMAL(6,2) NOT NULL,
    easy_percentage INT NOT NULL,
    medium_percentage INT NOT NULL,
    hard_percentage INT NOT NULL,
    mcq_count INT NOT NULL,
    true_false_count INT NOT NULL,
    short_answer_count INT NOT NULL,
    marks_per_question DECIMAL(5,2) DEFAULT 1.0,
    instructions TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, ACTIVE, ARCHIVED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bp_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_bp_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 12. Blueprint Lesson Selections
CREATE TABLE blueprint_lesson_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blueprint_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    target_percentage INT NOT NULL,
    question_count INT NOT NULL,
    CONSTRAINT fk_bls_bp FOREIGN KEY (blueprint_id) REFERENCES exam_blueprints(id) ON DELETE CASCADE,
    CONSTRAINT fk_bls_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- 13. AI Generation Jobs
CREATE TABLE ai_generation_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blueprint_id BIGINT NOT NULL,
    initiated_by BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING', -- PROCESSING, COMPLETED, FAILED
    prompt_summary TEXT,
    raw_response LONGTEXT,
    error_message TEXT,
    total_questions_generated INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aijob_bp FOREIGN KEY (blueprint_id) REFERENCES exam_blueprints(id) ON DELETE CASCADE,
    CONSTRAINT fk_aijob_user FOREIGN KEY (initiated_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 14. Question Bank
CREATE TABLE question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL, -- MCQ, TRUE_FALSE, SHORT_ANSWER
    difficulty VARCHAR(50) NOT NULL, -- EASY, MEDIUM, HARD
    marks DECIMAL(5,2) NOT NULL DEFAULT 1.0,
    explanation TEXT,
    source_reference VARCHAR(500),
    is_ai_generated BOOLEAN DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qb_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_qb_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    CONSTRAINT fk_qb_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_qb_subject ON question_bank(subject_id);
CREATE INDEX idx_qb_lesson ON question_bank(lesson_id);
CREATE INDEX idx_qb_diff ON question_bank(difficulty);

-- 15. Question Options (for MCQ & TRUE_FALSE)
CREATE TABLE question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_label VARCHAR(10) NOT NULL, -- A, B, C, D, etc.
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_opt_question FOREIGN KEY (question_id) REFERENCES question_bank(id) ON DELETE CASCADE
);

CREATE INDEX idx_opt_question ON question_options(question_id);

-- 16. Exam Papers
CREATE TABLE exam_papers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blueprint_id BIGINT,
    subject_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    total_questions INT NOT NULL,
    max_marks DECIMAL(6,2) NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, UNDER_REVIEW, APPROVED, PUBLISHED, ARCHIVED
    version INT NOT NULL DEFAULT 1,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    approved_at TIMESTAMP NULL,
    approved_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ep_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_ep_status ON exam_papers(status);
CREATE INDEX idx_ep_subject ON exam_papers(subject_id);

-- 17. Exam Paper Questions
CREATE TABLE exam_paper_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    marks DECIMAL(5,2) NOT NULL DEFAULT 1.0,
    CONSTRAINT fk_epq_paper FOREIGN KEY (exam_paper_id) REFERENCES exam_papers(id) ON DELETE CASCADE,
    CONSTRAINT fk_epq_question FOREIGN KEY (question_id) REFERENCES question_bank(id) ON DELETE CASCADE,
    CONSTRAINT uq_epq_order UNIQUE (exam_paper_id, question_order)
);

CREATE INDEX idx_epq_paper ON exam_paper_questions(exam_paper_id);

-- 18. Exam Schedules
CREATE TABLE exam_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_paper_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    section_id BIGINT,
    scheduled_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL,
    max_marks DECIMAL(6,2) NOT NULL,
    instructions TEXT,
    result_release_policy VARCHAR(50) NOT NULL DEFAULT 'AFTER_EXAM_WINDOW', -- IMMEDIATE, AFTER_EXAM_WINDOW, MANUAL
    is_results_released BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    cancelled_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_es_paper FOREIGN KEY (exam_paper_id) REFERENCES exam_papers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_es_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_es_sched_by FOREIGN KEY (scheduled_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_es_class ON exam_schedules(class_id);
CREATE INDEX idx_es_status ON exam_schedules(status);
CREATE INDEX idx_es_dates ON exam_schedules(start_time, end_time);

-- 19. Exam Attempts
CREATE TABLE exam_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    deadline_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, SUBMITTED, EVALUATED, TIMED_OUT, CANCELLED
    total_score DECIMAL(6,2) DEFAULT 0.0,
    percentage DECIMAL(5,2) DEFAULT 0.0,
    passed BOOLEAN DEFAULT FALSE,
    feedback TEXT,
    evaluated_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ea_schedule FOREIGN KEY (schedule_id) REFERENCES exam_schedules(id) ON DELETE CASCADE,
    CONSTRAINT fk_ea_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_attempt_sched_stud UNIQUE (schedule_id, student_id)
);

CREATE INDEX idx_ea_sched ON exam_attempts(schedule_id);
CREATE INDEX idx_ea_student ON exam_attempts(student_id);
CREATE INDEX idx_ea_status ON exam_attempts(status);

-- 20. Student Answers
CREATE TABLE student_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT,
    text_answer TEXT,
    is_marked_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    is_evaluated BOOLEAN NOT NULL DEFAULT FALSE,
    is_correct BOOLEAN DEFAULT FALSE,
    marks_awarded DECIMAL(5,2) DEFAULT 0.0,
    evaluated_by BIGINT,
    teacher_comment TEXT,
    last_saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sa_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_question FOREIGN KEY (question_id) REFERENCES question_bank(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_opt FOREIGN KEY (selected_option_id) REFERENCES question_options(id) ON DELETE SET NULL,
    CONSTRAINT fk_sa_eval_by FOREIGN KEY (evaluated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_answer_attempt_q UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_sa_attempt ON student_answers(attempt_id);

-- 21. Notifications
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'INFO', -- INFO, WARNING, SUCCESS, ALERT
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user ON notifications(user_id, is_read);

-- 22. Audit Logs
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_audit_action ON audit_logs(action);
