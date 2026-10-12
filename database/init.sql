-- ExamForge Initial Database Creation
CREATE DATABASE IF NOT EXISTS examforge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE examforge;

-- Database grants if running via docker compose
GRANT ALL PRIVILEGES ON examforge.* TO 'examforge_user'@'%';
FLUSH PRIVILEGES;
