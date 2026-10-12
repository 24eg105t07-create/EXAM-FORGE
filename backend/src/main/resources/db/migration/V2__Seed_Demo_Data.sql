-- V2__Seed_Demo_Data.sql: Seed Departments, Academic Years, Classes, Sections, Subjects, and Lessons

-- 1. Departments
INSERT INTO departments (id, code, name, description) VALUES
(1, 'CSE', 'Computer Science and Engineering', 'Department of Computer Science & Engineering'),
(2, 'ECE', 'Electronics and Communication Engineering', 'Department of Electronics & Communication Engineering'),
(3, 'IT', 'Information Technology', 'Department of Information Technology');

-- 2. Academic Years
INSERT INTO academic_years (id, year_name, start_date, end_date, is_current) VALUES
(1, '2026-2027', '2026-07-01', '2027-06-30', TRUE);

-- 3. Classes
INSERT INTO classes (id, department_id, code, name, semester) VALUES
(1, 1, 'CS-301', 'B.Tech Computer Science 3rd Year', 5),
(2, 3, 'IT-301', 'B.Tech Information Technology 3rd Year', 5);

-- 4. Sections
INSERT INTO sections (id, class_id, name) VALUES
(1, 1, 'Section A'),
(2, 1, 'Section B'),
(3, 2, 'Section A');

-- 5. Subjects
INSERT INTO subjects (id, department_id, code, name, description, credits) VALUES
(1, 1, 'CS301', 'Database Management Systems', 'Relational models, SQL, transactions, indexing, query optimization', 4),
(2, 1, 'CS302', 'Operating Systems', 'Processes, threads, CPU scheduling, memory management, file systems', 4),
(3, 1, 'CS303', 'Computer Networks', 'OSI layers, TCP/IP, routing protocols, socket programming', 3);

-- 6. Lessons for DBMS (Subject 1)
INSERT INTO lessons (id, subject_id, unit_number, title, description, order_index) VALUES
(1, 1, 1, 'Relational Data Model & SQL Queries', 'DDL, DML, Joins, Aggregations, Subqueries, and Relational Algebra', 1),
(2, 1, 2, 'Transactions & Concurrency Control', 'ACID properties, serializability, two-phase locking, deadlocks', 2),
(3, 1, 3, 'Database Normalization & Indexing', 'Functional dependencies, 1NF to BCNF, B-trees, hash indexes', 3);
