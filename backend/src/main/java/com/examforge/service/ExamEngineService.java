package com.examforge.service;

import com.examforge.dto.ExamTakingDtos;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamEngineService {

    private final ExamScheduleRepository scheduleRepository;
    private final ExamAttemptRepository attemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionOptionRepository optionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ExamTakingDtos.StudentExamSessionDto startOrResumeExam(Long scheduleId) {
        Long studentId = SecurityUtils.getCurrentUserId();
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new UnauthorizedException("Only students can take examinations");
        }

        ExamSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam schedule not found: " + scheduleId));

        if (schedule.getStatus() != ScheduleStatus.SCHEDULED && schedule.getStatus() != ScheduleStatus.IN_PROGRESS) {
            throw new BadRequestException("This examination is " + schedule.getStatus() + " and not active.");
        }

        LocalDateTime now = LocalDateTime.now();
        // Server-side window verification
        if (now.isBefore(schedule.getStartTime())) {
            throw new BadRequestException("Examination has not started yet. Permitted start time: " + schedule.getStartTime());
        }

        if (now.isAfter(schedule.getEndTime())) {
            throw new BadRequestException("Examination window has closed. Permitted deadline was: " + schedule.getEndTime());
        }

        // Verify student eligibility
        boolean isEligible = (schedule.getSection() != null)
                ? enrollmentRepository.findByStudentId(studentId)
                    .map(e -> e.getSection().getId().equals(schedule.getSection().getId()))
                    .orElse(false)
                : enrollmentRepository.findByStudentId(studentId)
                    .map(e -> e.getSection().getClassEntity().getId().equals(schedule.getClassEntity().getId()))
                    .orElse(false);

        if (!isEligible) {
            throw new UnauthorizedException("You are not enrolled in the class or section for this examination");
        }

        // Check for existing attempt
        Optional<ExamAttempt> existingOpt = attemptRepository.findByScheduleIdAndStudentId(scheduleId, studentId);
        ExamAttempt attempt;

        if (existingOpt.isPresent()) {
            attempt = existingOpt.get();
            if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.EVALUATED) {
                throw new BadRequestException("You have already submitted this examination.");
            }
            // Check if deadline has passed while away
            if (now.isAfter(attempt.getDeadlineTime())) {
                attempt.setStatus(AttemptStatus.TIMED_OUT);
                attemptRepository.save(attempt);
                evaluateAttempt(attempt);
                throw new BadRequestException("Time limit expired for this examination attempt.");
            }
        } else {
            // Create fresh attempt
            // Deadline is min(now + duration, schedule.endTime)
            LocalDateTime calculatedDeadline = now.plusMinutes(schedule.getDurationMinutes());
            if (calculatedDeadline.isAfter(schedule.getEndTime())) {
                calculatedDeadline = schedule.getEndTime();
            }

            attempt = ExamAttempt.builder()
                    .schedule(schedule)
                    .student(student)
                    .startTime(now)
                    .deadlineTime(calculatedDeadline)
                    .status(AttemptStatus.IN_PROGRESS)
                    .build();

            attempt = attemptRepository.save(attempt);

            auditLogService.logAction("START_EXAM_ATTEMPT", "EXAM_ATTEMPT", attempt.getId(),
                    "Student " + student.getEmail() + " started exam: " + schedule.getTitle(), null);
        }

        return buildSessionDto(attempt);
    }

    @Transactional
    public ExamTakingDtos.SavedAnswerDto saveAnswer(Long scheduleId, ExamTakingDtos.SaveAnswerRequest request) {
        Long studentId = SecurityUtils.getCurrentUserId();
        ExamAttempt attempt = attemptRepository.findByScheduleIdAndStudentId(scheduleId, studentId)
                .orElseThrow(() -> new BadRequestException("No active attempt found for this examination"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot save answers. Attempt is " + attempt.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(attempt.getDeadlineTime())) {
            attempt.setStatus(AttemptStatus.TIMED_OUT);
            attemptRepository.save(attempt);
            evaluateAttempt(attempt);
            throw new BadRequestException("Exam time expired. Attempt automatically submitted.");
        }

        QuestionBank question = questionBankRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + request.getQuestionId()));

        QuestionOption option = (request.getSelectedOptionId() != null)
                ? optionRepository.findById(request.getSelectedOptionId()).orElse(null)
                : null;

        Optional<StudentAnswer> existingAnswerOpt = studentAnswerRepository.findByAttemptIdAndQuestionId(
                attempt.getId(), question.getId());

        StudentAnswer answer = existingAnswerOpt.orElseGet(() -> StudentAnswer.builder()
                .attempt(attempt)
                .question(question)
                .build());

        answer.setSelectedOption(option);
        answer.setTextAnswer(request.getTextAnswer());
        answer.setIsMarkedForReview(Boolean.TRUE.equals(request.getIsMarkedForReview()));
        answer.setLastSavedAt(now);

        StudentAnswer saved = studentAnswerRepository.save(answer);

        return ExamTakingDtos.SavedAnswerDto.builder()
                .questionId(saved.getQuestion().getId())
                .selectedOptionId(saved.getSelectedOption() != null ? saved.getSelectedOption().getId() : null)
                .textAnswer(saved.getTextAnswer())
                .isMarkedForReview(saved.getIsMarkedForReview())
                .lastSavedAt(saved.getLastSavedAt())
                .build();
    }

    @Transactional
    public ExamTakingDtos.SubmitExamResponse submitExam(Long scheduleId) {
        Long studentId = SecurityUtils.getCurrentUserId();
        ExamAttempt attempt = attemptRepository.findByScheduleIdAndStudentId(scheduleId, studentId)
                .orElseThrow(() -> new BadRequestException("No exam attempt found"));

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.EVALUATED) {
            return ExamTakingDtos.SubmitExamResponse.builder()
                    .attemptId(attempt.getId())
                    .status(attempt.getStatus())
                    .submittedAt(attempt.getEndTime())
                    .message("Exam was already submitted.")
                    .score(attempt.getTotalScore())
                    .maxMarks(attempt.getSchedule().getMaxMarks())
                    .build();
        }

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setEndTime(LocalDateTime.now());
        attemptRepository.save(attempt);

        // Immediate automatic grading for objective questions
        evaluateAttempt(attempt);

        auditLogService.logAction("SUBMIT_EXAM_ATTEMPT", "EXAM_ATTEMPT", attempt.getId(),
                "Student submitted exam attempt. Score: " + attempt.getTotalScore() + "/" + attempt.getSchedule().getMaxMarks(), null);

        boolean showScore = attempt.getSchedule().getResultReleasePolicy() == ResultReleasePolicy.IMMEDIATE
                || attempt.getSchedule().getIsResultsReleased();

        return ExamTakingDtos.SubmitExamResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus())
                .submittedAt(attempt.getEndTime())
                .message("Exam submitted successfully!")
                .score(showScore ? attempt.getTotalScore() : null)
                .maxMarks(attempt.getSchedule().getMaxMarks())
                .build();
    }

    @Transactional
    public void evaluateAttempt(ExamAttempt attempt) {
        List<StudentAnswer> answers = studentAnswerRepository.findByAttemptId(attempt.getId());
        BigDecimal totalScore = BigDecimal.ZERO;

        for (StudentAnswer answer : answers) {
            QuestionBank question = answer.getQuestion();

            if (question.getQuestionType() == QuestionType.MCQ || question.getQuestionType() == QuestionType.TRUE_FALSE) {
                if (answer.getSelectedOption() != null) {
                    boolean isCorrect = Boolean.TRUE.equals(answer.getSelectedOption().getIsCorrect());
                    answer.setIsCorrect(isCorrect);
                    answer.setIsEvaluated(true);
                    answer.setMarksAwarded(isCorrect ? question.getMarks() : BigDecimal.ZERO);
                    if (isCorrect) {
                        totalScore = totalScore.add(question.getMarks());
                    }
                } else {
                    answer.setIsCorrect(false);
                    answer.setIsEvaluated(true);
                    answer.setMarksAwarded(BigDecimal.ZERO);
                }
                studentAnswerRepository.save(answer);
            }
        }

        attempt.setTotalScore(totalScore);
        BigDecimal maxMarks = attempt.getSchedule().getMaxMarks();
        BigDecimal percentage = BigDecimal.ZERO;
        if (maxMarks.compareTo(BigDecimal.ZERO) > 0) {
            percentage = totalScore.multiply(BigDecimal.valueOf(100)).divide(maxMarks, 2, RoundingMode.HALF_UP);
        }
        attempt.setPercentage(percentage);
        attempt.setPassed(percentage.compareTo(BigDecimal.valueOf(40.0)) >= 0); // standard 40% passing score
        attempt.setStatus(AttemptStatus.EVALUATED);
        attempt.setEvaluatedAt(LocalDateTime.now());
        attemptRepository.save(attempt);
    }

    private ExamTakingDtos.StudentExamSessionDto buildSessionDto(ExamAttempt attempt) {
        ExamSchedule schedule = attempt.getSchedule();
        ExamPaper paper = schedule.getExamPaper();

        long remainingSeconds = Math.max(0, Duration.between(LocalDateTime.now(), attempt.getDeadlineTime()).getSeconds());

        // Secure student-facing questions: NO isCorrect, NO explanation!
        List<ExamTakingDtos.StudentQuestionDto> questions = paper.getPaperQuestions().stream().map(pq -> {
            QuestionBank q = pq.getQuestion();
            List<ExamTakingDtos.StudentOptionDto> opts = q.getOptions().stream().map(o ->
                    ExamTakingDtos.StudentOptionDto.builder()
                            .id(o.getId())
                            .optionLabel(o.getOptionLabel())
                            .optionText(o.getOptionText())
                            .build()
            ).toList();

            return ExamTakingDtos.StudentQuestionDto.builder()
                    .questionId(q.getId())
                    .questionOrder(pq.getQuestionOrder())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType())
                    .marks(pq.getMarks())
                    .options(opts)
                    .build();
        }).toList();

        // Load previously saved answers (refresh resilience)
        Map<Long, ExamTakingDtos.SavedAnswerDto> savedAnswers = new HashMap<>();
        List<StudentAnswer> answers = studentAnswerRepository.findByAttemptId(attempt.getId());
        for (StudentAnswer a : answers) {
            savedAnswers.put(a.getQuestion().getId(), ExamTakingDtos.SavedAnswerDto.builder()
                    .questionId(a.getQuestion().getId())
                    .selectedOptionId(a.getSelectedOption() != null ? a.getSelectedOption().getId() : null)
                    .textAnswer(a.getTextAnswer())
                    .isMarkedForReview(a.getIsMarkedForReview())
                    .lastSavedAt(a.getLastSavedAt())
                    .build());
        }

        return ExamTakingDtos.StudentExamSessionDto.builder()
                .attemptId(attempt.getId())
                .scheduleId(schedule.getId())
                .examTitle(schedule.getTitle())
                .subjectName(paper.getSubject().getName())
                .durationMinutes(schedule.getDurationMinutes())
                .maxMarks(schedule.getMaxMarks())
                .instructions(schedule.getInstructions())
                .startTime(attempt.getStartTime())
                .deadlineTime(attempt.getDeadlineTime())
                .remainingSeconds(remainingSeconds)
                .status(attempt.getStatus())
                .questions(questions)
                .savedAnswers(savedAnswers)
                .build();
    }
}
