package com.examforge.service;

import com.examforge.dto.ExamScheduleDtos;
import com.examforge.entity.*;
import com.examforge.exception.BadRequestException;
import com.examforge.exception.ResourceNotFoundException;
import com.examforge.exception.UnauthorizedException;
import com.examforge.repository.*;
import com.examforge.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    private final ExamScheduleRepository scheduleRepository;
    private final ExamPaperRepository paperRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExamAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public ExamScheduleDtos.ExamScheduleDto scheduleExam(ExamScheduleDtos.CreateScheduleRequest request) {
        if (!SecurityUtils.isManagement()) {
            throw new UnauthorizedException("Only college management can schedule examinations");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        User scheduler = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ExamPaper paper = paperRepository.findById(request.getExamPaperId())
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper not found: " + request.getExamPaperId()));

        if (paper.getStatus() != PaperStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED examination papers can be scheduled. Current status: " + paper.getStatus());
        }

        ClassEntity cls = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.getClassId()));

        Section section = (request.getSectionId() != null)
                ? sectionRepository.findById(request.getSectionId()).orElse(null)
                : null;

        // Validate time window
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new BadRequestException("End time must be strictly after start time");
        }

        if (request.getDurationMinutes() <= 0) {
            throw new BadRequestException("Exam duration must be positive");
        }

        // Validate student eligibility
        List<StudentEnrollment> enrollments = (section != null)
                ? enrollmentRepository.findBySectionId(section.getId())
                : enrollmentRepository.findBySectionClassEntityId(cls.getId());

        if (enrollments.isEmpty()) {
            throw new BadRequestException("The selected class/section has no enrolled students.");
        }

        // Conflict check
        List<ExamSchedule> conflicts = scheduleRepository.findConflictingSchedules(
                cls.getId(), request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            ExamSchedule conflict = conflicts.getFirst();
            throw new BadRequestException(String.format(
                    "Schedule conflict detected: Exam '%s' is already scheduled for this class between %s and %s",
                    conflict.getTitle(), conflict.getStartTime(), conflict.getEndTime()));
        }

        ExamSchedule schedule = ExamSchedule.builder()
                .examPaper(paper)
                .classEntity(cls)
                .section(section)
                .scheduledBy(scheduler)
                .title(request.getTitle().trim())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .maxMarks(request.getMaxMarks() != null ? request.getMaxMarks() : paper.getMaxMarks())
                .instructions(request.getInstructions())
                .resultReleasePolicy(request.getResultReleasePolicy())
                .isResultsReleased(false)
                .status(ScheduleStatus.SCHEDULED)
                .build();

        ExamSchedule saved = scheduleRepository.save(schedule);

        // Notify enrolled students
        for (StudentEnrollment enrollment : enrollments) {
            notificationService.createNotification(
                    enrollment.getStudent().getId(),
                    "New Exam Scheduled: " + saved.getTitle(),
                    "An examination for " + paper.getSubject().getName() + " is scheduled on " + saved.getStartTime(),
                    NotificationType.INFO,
                    "/student/exams/" + saved.getId()
            );
        }

        auditLogService.logAction("SCHEDULE_EXAM", "EXAM_SCHEDULE", saved.getId(),
                "Scheduled exam '" + saved.getTitle() + "' for class " + cls.getName(), null);

        return mapToScheduleDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleDtos.ExamScheduleDto> getAllSchedules() {
        return scheduleRepository.findAll().stream().map(this::mapToScheduleDto).toList();
    }

    @Transactional(readOnly = true)
    public ExamScheduleDtos.ExamScheduleDto getScheduleById(Long id) {
        ExamSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam schedule not found: " + id));
        return mapToScheduleDto(schedule);
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleDtos.ExamScheduleDto> getAvailableForStudent(Long studentId) {
        return scheduleRepository.findAvailableForStudent(studentId).stream()
                .map(this::mapToScheduleDto)
                .toList();
    }

    @Transactional
    public ExamScheduleDtos.ExamScheduleDto cancelSchedule(Long scheduleId, ExamScheduleDtos.CancelScheduleRequest request) {
        if (!SecurityUtils.isManagement()) {
            throw new UnauthorizedException("Only management can cancel exam schedules");
        }

        ExamSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam schedule not found: " + scheduleId));

        schedule.setStatus(ScheduleStatus.CANCELLED);
        schedule.setCancelledReason(request.getReason());
        ExamSchedule saved = scheduleRepository.save(schedule);

        auditLogService.logAction("CANCEL_EXAM_SCHEDULE", "EXAM_SCHEDULE", saved.getId(),
                "Cancelled exam schedule: " + request.getReason(), null);

        return mapToScheduleDto(saved);
    }

    @Transactional
    public void releaseResults(Long scheduleId) {
        ExamSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam schedule not found: " + scheduleId));

        schedule.setIsResultsReleased(true);
        scheduleRepository.save(schedule);

        auditLogService.logAction("RELEASE_EXAM_RESULTS", "EXAM_SCHEDULE", schedule.getId(),
                "Manually released results for exam: " + schedule.getTitle(), null);
    }

    public ExamScheduleDtos.ExamScheduleDto mapToScheduleDto(ExamSchedule s) {
        int eligibleCount = (s.getSection() != null)
                ? enrollmentRepository.findBySectionId(s.getSection().getId()).size()
                : enrollmentRepository.findBySectionClassEntityId(s.getClassEntity().getId()).size();

        int attemptsCount = attemptRepository.findByScheduleId(s.getId()).size();

        return ExamScheduleDtos.ExamScheduleDto.builder()
                .id(s.getId())
                .examPaperId(s.getExamPaper().getId())
                .examPaperTitle(s.getExamPaper().getTitle())
                .subjectId(s.getExamPaper().getSubject().getId())
                .subjectName(s.getExamPaper().getSubject().getName())
                .classId(s.getClassEntity().getId())
                .className(s.getClassEntity().getName())
                .sectionId(s.getSection() != null ? s.getSection().getId() : null)
                .sectionName(s.getSection() != null ? s.getSection().getName() : "All Sections")
                .title(s.getTitle())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .durationMinutes(s.getDurationMinutes())
                .maxMarks(s.getMaxMarks())
                .instructions(s.getInstructions())
                .resultReleasePolicy(s.getResultReleasePolicy())
                .isResultsReleased(s.getIsResultsReleased())
                .status(s.getStatus())
                .cancelledReason(s.getCancelledReason())
                .eligibleStudentsCount(eligibleCount)
                .attemptsCount(attemptsCount)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
