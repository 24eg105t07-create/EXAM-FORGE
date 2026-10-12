package com.examforge.service;

import com.examforge.config.ApplicationProperties;
import com.examforge.entity.*;
import com.examforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataBootstrapService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherSubjectAssignmentRepository assignmentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationProperties properties;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking ExamForge initial database bootstrap...");

        // 1. Bootstrap Admin
        String adminEmail = properties.getBootstrap().getAdminEmail();
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .fullName(properties.getBootstrap().getAdminName())
                    .role(Role.MANAGEMENT)
                    .status(UserStatus.ACTIVE)
                    .phone("+91-9876543210")
                    .build();
            userRepository.save(admin);
            log.info("Bootstrap: Created SuperAdmin account: {}", adminEmail);
        } else {
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
        }

        // 2. Bootstrap Teacher
        String teacherEmail = "teacher.sharma@college.edu";
        User teacher = userRepository.findByEmail(teacherEmail).orElse(null);
        if (teacher == null) {
            teacher = User.builder()
                    .email(teacherEmail)
                    .passwordHash(passwordEncoder.encode("Teacher@123"))
                    .fullName("Dr. Rajesh Sharma")
                    .role(Role.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .phone("+91-9876543211")
                    .build();
            teacher = userRepository.save(teacher);
            log.info("Bootstrap: Created Demo Teacher account: {}", teacherEmail);
        } else {
            teacher.setPasswordHash(passwordEncoder.encode("Teacher@123"));
            teacher.setStatus(UserStatus.ACTIVE);
            teacher = userRepository.save(teacher);
        }

        // 3. Bootstrap Students
        String student1Email = "student.alex@college.edu";
        User student1 = userRepository.findByEmail(student1Email).orElse(null);
        if (student1 == null) {
            student1 = User.builder()
                    .email(student1Email)
                    .passwordHash(passwordEncoder.encode("Student@123"))
                    .fullName("Alex Johnson")
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .phone("+91-9876543212")
                    .build();
            student1 = userRepository.save(student1);
            log.info("Bootstrap: Created Demo Student account: {}", student1Email);
        } else {
            student1.setPasswordHash(passwordEncoder.encode("Student@123"));
            student1.setStatus(UserStatus.ACTIVE);
            student1 = userRepository.save(student1);
        }

        String student2Email = "student.priya@college.edu";
        User student2 = userRepository.findByEmail(student2Email).orElse(null);
        if (student2 == null) {
            student2 = User.builder()
                    .email(student2Email)
                    .passwordHash(passwordEncoder.encode("Student@123"))
                    .fullName("Priya Nair")
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .phone("+91-9876543213")
                    .build();
            student2 = userRepository.save(student2);
            log.info("Bootstrap: Created Demo Student account: {}", student2Email);
        } else {
            student2.setPasswordHash(passwordEncoder.encode("Student@123"));
            student2.setStatus(UserStatus.ACTIVE);
            student2 = userRepository.save(student2);
        }

        // 4. Link Teacher Assignment if not present
        Subject dbms = subjectRepository.findByCode("CS301").orElse(null);
        ClassEntity cseClass = classRepository.findById(1L).orElse(null);
        Section sectionA = sectionRepository.findById(1L).orElse(null);
        AcademicYear currentYear = academicYearRepository.findByIsCurrentTrue().orElse(null);

        if (dbms != null && cseClass != null && currentYear != null && teacher != null) {
            if (!assignmentRepository.existsByTeacherIdAndSubjectId(teacher.getId(), dbms.getId())) {
                TeacherSubjectAssignment assignment = TeacherSubjectAssignment.builder()
                        .teacher(teacher)
                        .subject(dbms)
                        .classEntity(cseClass)
                        .section(sectionA)
                        .academicYear(currentYear)
                        .build();
                assignmentRepository.save(assignment);
                log.info("Bootstrap: Assigned teacher {} to subject {}", teacherEmail, dbms.getName());
            }
        }

        // 5. Link Student Enrollments if not present
        if (student1 != null && sectionA != null && currentYear != null) {
            if (enrollmentRepository.findByStudentId(student1.getId()).isEmpty()) {
                StudentEnrollment enroll1 = StudentEnrollment.builder()
                        .student(student1)
                        .section(sectionA)
                        .rollNumber("2026CSE001")
                        .academicYear(currentYear)
                        .status("ACTIVE")
                        .build();
                enrollmentRepository.save(enroll1);
                log.info("Bootstrap: Enrolled student {} into {}", student1Email, sectionA.getName());
            }
        }

        if (student2 != null && sectionA != null && currentYear != null) {
            if (enrollmentRepository.findByStudentId(student2.getId()).isEmpty()) {
                StudentEnrollment enroll2 = StudentEnrollment.builder()
                        .student(student2)
                        .section(sectionA)
                        .rollNumber("2026CSE002")
                        .academicYear(currentYear)
                        .status("ACTIVE")
                        .build();
                enrollmentRepository.save(enroll2);
                log.info("Bootstrap: Enrolled student {} into {}", student2Email, sectionA.getName());
            }
        }

        log.info("ExamForge bootstrap process completed successfully.");
    }
}
