package com.examforge.service;

import com.examforge.dto.AcademicDtos;
import com.examforge.entity.Department;
import com.examforge.entity.Subject;
import com.examforge.entity.TeacherSubjectAssignment;
import com.examforge.exception.BadRequestException;
import com.examforge.exception.ResourceNotFoundException;
import com.examforge.exception.UnauthorizedException;
import com.examforge.repository.DepartmentRepository;
import com.examforge.repository.SubjectRepository;
import com.examforge.repository.TeacherSubjectAssignmentRepository;
import com.examforge.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherSubjectAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AcademicDtos.SubjectDto> getAllSubjects() {
        return subjectRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.SubjectDto> getSubjectsForTeacher(Long teacherId) {
        List<TeacherSubjectAssignment> assignments = assignmentRepository.findByTeacherId(teacherId);
        return assignments.stream()
                .map(TeacherSubjectAssignment::getSubject)
                .distinct()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicDtos.SubjectDto getSubjectById(Long id) {
        Subject s = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
        return mapToDto(s);
    }

    @Transactional
    public AcademicDtos.SubjectDto createSubject(AcademicDtos.CreateSubjectRequest request) {
        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.getDepartmentId()));

        if (subjectRepository.findByCode(request.getCode().trim().toUpperCase()).isPresent()) {
            throw new BadRequestException("Subject code already exists: " + request.getCode());
        }

        Subject subject = Subject.builder()
                .department(dept)
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .credits(request.getCredits() != null ? request.getCredits() : 3)
                .build();
        Subject saved = subjectRepository.save(subject);

        auditLogService.logAction("CREATE_SUBJECT", "SUBJECT", saved.getId(), "Created subject " + saved.getCode(), null);

        return mapToDto(saved);
    }

    public void validateTeacherSubjectAccess(Long teacherId, Long subjectId) {
        if (SecurityUtils.isManagement()) {
            return; // Management has global college access
        }
        boolean hasAccess = assignmentRepository.existsByTeacherIdAndSubjectId(teacherId, subjectId);
        if (!hasAccess) {
            throw new UnauthorizedException("You are not assigned to this subject (Subject ID: " + subjectId + ")");
        }
    }

    private AcademicDtos.SubjectDto mapToDto(Subject s) {
        return AcademicDtos.SubjectDto.builder()
                .id(s.getId())
                .departmentId(s.getDepartment().getId())
                .departmentName(s.getDepartment().getName())
                .code(s.getCode())
                .name(s.getName())
                .description(s.getDescription())
                .credits(s.getCredits())
                .build();
    }
}
