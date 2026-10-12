package com.examforge.service;

import com.examforge.dto.AcademicDtos;
import com.examforge.entity.*;
import com.examforge.exception.BadRequestException;
import com.examforge.exception.ResourceNotFoundException;
import com.examforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicService {

    private final DepartmentRepository departmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final TeacherSubjectAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<AcademicDtos.DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream().map(d -> AcademicDtos.DepartmentDto.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .build()).toList();
    }

    @Transactional
    public AcademicDtos.DepartmentDto createDepartment(AcademicDtos.CreateDepartmentRequest request) {
        if (departmentRepository.existsByCode(request.getCode().trim().toUpperCase())) {
            throw new BadRequestException("Department code already exists: " + request.getCode());
        }
        Department dept = Department.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();
        Department saved = departmentRepository.save(dept);
        auditLogService.logAction("CREATE_DEPARTMENT", "DEPARTMENT", saved.getId(), "Created department " + saved.getCode(), null);
        return AcademicDtos.DepartmentDto.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .name(saved.getName())
                .description(saved.getDescription())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // --- Academic Years ---
    @Transactional(readOnly = true)
    public List<AcademicDtos.AcademicYearDto> getAllAcademicYears() {
        return academicYearRepository.findAll().stream().map(ay -> AcademicDtos.AcademicYearDto.builder()
                .id(ay.getId())
                .yearName(ay.getYearName())
                .startDate(ay.getStartDate())
                .endDate(ay.getEndDate())
                .isCurrent(ay.getIsCurrent())
                .build()).toList();
    }

    @Transactional
    public AcademicDtos.AcademicYearDto createAcademicYear(AcademicDtos.CreateAcademicYearRequest request) {
        AcademicYear ay = AcademicYear.builder()
                .yearName(request.getYearName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(Boolean.TRUE.equals(request.getIsCurrent()))
                .build();
        AcademicYear saved = academicYearRepository.save(ay);
        return AcademicDtos.AcademicYearDto.builder()
                .id(saved.getId())
                .yearName(saved.getYearName())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .isCurrent(saved.getIsCurrent())
                .build();
    }

    // --- Classes & Sections ---
    @Transactional(readOnly = true)
    public List<AcademicDtos.ClassDto> getAllClasses() {
        return classRepository.findAll().stream().map(c -> AcademicDtos.ClassDto.builder()
                .id(c.getId())
                .departmentId(c.getDepartment().getId())
                .departmentName(c.getDepartment().getName())
                .code(c.getCode())
                .name(c.getName())
                .semester(c.getSemester())
                .build()).toList();
    }

    @Transactional
    public AcademicDtos.ClassDto createClass(AcademicDtos.CreateClassRequest request) {
        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.getDepartmentId()));

        ClassEntity cls = ClassEntity.builder()
                .department(dept)
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .semester(request.getSemester())
                .build();
        ClassEntity saved = classRepository.save(cls);
        return AcademicDtos.ClassDto.builder()
                .id(saved.getId())
                .departmentId(dept.getId())
                .departmentName(dept.getName())
                .code(saved.getCode())
                .name(saved.getName())
                .semester(saved.getSemester())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.SectionDto> getSectionsByClass(Long classId) {
        return sectionRepository.findByClassEntityId(classId).stream().map(s -> AcademicDtos.SectionDto.builder()
                .id(s.getId())
                .classId(s.getClassEntity().getId())
                .className(s.getClassEntity().getName())
                .name(s.getName())
                .build()).toList();
    }

    @Transactional
    public AcademicDtos.SectionDto createSection(AcademicDtos.CreateSectionRequest request) {
        ClassEntity cls = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        Section sec = Section.builder()
                .classEntity(cls)
                .name(request.getName().trim())
                .build();
        Section saved = sectionRepository.save(sec);
        return AcademicDtos.SectionDto.builder()
                .id(saved.getId())
                .classId(cls.getId())
                .className(cls.getName())
                .name(saved.getName())
                .build();
    }

    // --- Student Enrollment ---
    @Transactional
    public AcademicDtos.StudentEnrollmentDto enrollStudent(AcademicDtos.EnrollStudentRequest request) {
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + request.getStudentId()));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("User is not a student");
        }

        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.getSectionId()));
        AcademicYear ay = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.getAcademicYearId()));

        if (enrollmentRepository.existsByRollNumber(request.getRollNumber().trim())) {
            throw new BadRequestException("Roll number already registered: " + request.getRollNumber());
        }

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(student)
                .section(section)
                .rollNumber(request.getRollNumber().trim().toUpperCase())
                .academicYear(ay)
                .status("ACTIVE")
                .build();
        StudentEnrollment saved = enrollmentRepository.save(enrollment);

        auditLogService.logAction("ENROLL_STUDENT", "STUDENT_ENROLLMENT", saved.getId(),
                "Enrolled " + student.getEmail() + " into " + section.getName() + " with roll " + saved.getRollNumber(), null);

        return mapToEnrollmentDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.StudentEnrollmentDto> getEnrollmentsByClass(Long classId) {
        return enrollmentRepository.findBySectionClassEntityId(classId).stream()
                .map(this::mapToEnrollmentDto)
                .toList();
    }

    // --- Teacher Assignments ---
    @Transactional
    public AcademicDtos.TeacherAssignmentDto assignTeacher(AcademicDtos.AssignTeacherRequest request) {
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + request.getTeacherId()));
        if (teacher.getRole() != Role.TEACHER) {
            throw new BadRequestException("User is not a teacher");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));
        ClassEntity cls = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));
        Section sec = (request.getSectionId() != null) ? sectionRepository.findById(request.getSectionId()).orElse(null) : null;
        AcademicYear ay = academicYearRepository.findById(request.getAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + request.getAcademicYearId()));

        TeacherSubjectAssignment assignment = TeacherSubjectAssignment.builder()
                .teacher(teacher)
                .subject(subject)
                .classEntity(cls)
                .section(sec)
                .academicYear(ay)
                .build();
        TeacherSubjectAssignment saved = assignmentRepository.save(assignment);

        auditLogService.logAction("ASSIGN_TEACHER", "TEACHER_ASSIGNMENT", saved.getId(),
                "Assigned teacher " + teacher.getEmail() + " to subject " + subject.getCode(), null);

        return AcademicDtos.TeacherAssignmentDto.builder()
                .id(saved.getId())
                .teacherId(teacher.getId())
                .teacherName(teacher.getFullName())
                .teacherEmail(teacher.getEmail())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .classId(cls.getId())
                .className(cls.getName())
                .sectionId(sec != null ? sec.getId() : null)
                .sectionName(sec != null ? sec.getName() : "All Sections")
                .academicYearId(ay.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.TeacherAssignmentDto> getAllTeacherAssignments() {
        return assignmentRepository.findAll().stream().map(a -> AcademicDtos.TeacherAssignmentDto.builder()
                .id(a.getId())
                .teacherId(a.getTeacher().getId())
                .teacherName(a.getTeacher().getFullName())
                .teacherEmail(a.getTeacher().getEmail())
                .subjectId(a.getSubject().getId())
                .subjectName(a.getSubject().getName())
                .subjectCode(a.getSubject().getCode())
                .classId(a.getClassEntity().getId())
                .className(a.getClassEntity().getName())
                .sectionId(a.getSection() != null ? a.getSection().getId() : null)
                .sectionName(a.getSection() != null ? a.getSection().getName() : "All Sections")
                .academicYearId(a.getAcademicYear().getId())
                .build()).toList();
    }

    private AcademicDtos.StudentEnrollmentDto mapToEnrollmentDto(StudentEnrollment e) {
        return AcademicDtos.StudentEnrollmentDto.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getFullName())
                .studentEmail(e.getStudent().getEmail())
                .sectionId(e.getSection().getId())
                .sectionName(e.getSection().getName())
                .classId(e.getSection().getClassEntity().getId())
                .className(e.getSection().getClassEntity().getName())
                .rollNumber(e.getRollNumber())
                .academicYearId(e.getAcademicYear().getId())
                .academicYearName(e.getAcademicYear().getYearName())
                .status(e.getStatus())
                .build();
    }
}
