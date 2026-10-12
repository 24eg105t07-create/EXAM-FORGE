package com.examforge.service;

import com.examforge.dto.ResultDtos;
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final ExamAttemptRepository attemptRepository;
    private final ExamScheduleRepository scheduleRepository;
    private final StudentAnswerRepository answerRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SubjectService subjectService;

    @Transactional(readOnly = true)
    public List<ResultDtos.StudentAttemptSummaryDto> getStudentAttemptHistory(Long studentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // Prevent cross-student privacy leak
        if (SecurityUtils.isStudent() && !currentUserId.equals(studentId)) {
            throw new UnauthorizedException("You are not authorized to view another student's exam history");
        }

        return attemptRepository.findByStudentId(studentId).stream().map(a -> {
            boolean released = isResultReleased(a.getSchedule());
            return ResultDtos.StudentAttemptSummaryDto.builder()
                    .attemptId(a.getId())
                    .scheduleId(a.getSchedule().getId())
                    .examTitle(a.getSchedule().getTitle())
                    .subjectName(a.getSchedule().getExamPaper().getSubject().getName())
                    .startTime(a.getStartTime())
                    .endTime(a.getEndTime())
                    .status(a.getStatus().name())
                    .totalScore(released ? a.getTotalScore() : null)
                    .maxMarks(a.getSchedule().getMaxMarks())
                    .percentage(released ? a.getPercentage() : null)
                    .passed(released ? a.getPassed() : null)
                    .isResultReleased(released)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public ResultDtos.DetailedResultDto getDetailedResult(Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (SecurityUtils.isStudent() && !attempt.getStudent().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to view this result");
        }

        if (SecurityUtils.isTeacher()) {
            subjectService.validateTeacherSubjectAccess(currentUserId, attempt.getSchedule().getExamPaper().getSubject().getId());
        }

        if (SecurityUtils.isStudent() && !isResultReleased(attempt.getSchedule())) {
            throw new BadRequestException("Examination results have not been released yet for this paper.");
        }

        List<StudentAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
        ExamPaper paper = attempt.getSchedule().getExamPaper();

        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;

        Map<String, List<BreakdownItem>> topicMap = new HashMap<>();
        Map<String, List<BreakdownItem>> diffMap = new HashMap<>();

        List<ResultDtos.QuestionResultDetailDto> answerDetails = new ArrayList<>();

        for (ExamPaperQuestion epq : paper.getPaperQuestions()) {
            QuestionBank q = epq.getQuestion();
            StudentAnswer ans = answers.stream()
                    .filter(a -> a.getQuestion().getId().equals(q.getId()))
                    .findFirst()
                    .orElse(null);

            boolean hasAnswered = ans != null && (ans.getSelectedOption() != null || (ans.getTextAnswer() != null && !ans.getTextAnswer().isBlank()));
            boolean isCorrect = ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
            BigDecimal marksAwarded = ans != null ? ans.getMarksAwarded() : BigDecimal.ZERO;

            if (!hasAnswered) {
                unansweredCount++;
            } else if (isCorrect) {
                correctCount++;
            } else {
                incorrectCount++;
            }

            // Topic Breakdown
            String topic = q.getLesson().getTitle();
            topicMap.computeIfAbsent(topic, k -> new ArrayList<>())
                    .add(new BreakdownItem(isCorrect, epq.getMarks(), marksAwarded));

            // Difficulty Breakdown
            String diff = q.getDifficulty().name();
            diffMap.computeIfAbsent(diff, k -> new ArrayList<>())
                    .add(new BreakdownItem(isCorrect, epq.getMarks(), marksAwarded));

            // Find correct option
            QuestionOption correctOpt = q.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .findFirst().orElse(null);

            answerDetails.add(ResultDtos.QuestionResultDetailDto.builder()
                    .questionId(q.getId())
                    .questionOrder(epq.getQuestionOrder())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .lessonTitle(q.getLesson().getTitle())
                    .selectedOptionId(ans != null && ans.getSelectedOption() != null ? ans.getSelectedOption().getId() : null)
                    .selectedOptionLabel(ans != null && ans.getSelectedOption() != null ? ans.getSelectedOption().getOptionLabel() : null)
                    .selectedOptionText(ans != null && ans.getSelectedOption() != null ? ans.getSelectedOption().getOptionText() : null)
                    .correctOptionLabel(correctOpt != null ? correctOpt.getOptionLabel() : null)
                    .correctOptionText(correctOpt != null ? correctOpt.getOptionText() : null)
                    .studentTextAnswer(ans != null ? ans.getTextAnswer() : null)
                    .isCorrect(isCorrect)
                    .marksAwarded(marksAwarded)
                    .maxMarks(epq.getMarks())
                    .explanation(q.getExplanation())
                    .teacherComment(ans != null ? ans.getTeacherComment() : null)
                    .build());
        }

        return ResultDtos.DetailedResultDto.builder()
                .attemptId(attempt.getId())
                .scheduleId(attempt.getSchedule().getId())
                .examTitle(attempt.getSchedule().getTitle())
                .subjectName(paper.getSubject().getName())
                .studentName(attempt.getStudent().getFullName())
                .rollNumber(enrollmentRepository.findByStudentId(attempt.getStudent().getId()).map(StudentEnrollment::getRollNumber).orElse("N/A"))
                .totalScore(attempt.getTotalScore())
                .maxMarks(attempt.getSchedule().getMaxMarks())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .totalQuestions(paper.getPaperQuestions().size())
                .correctCount(correctCount)
                .incorrectCount(incorrectCount)
                .unansweredCount(unansweredCount)
                .topicBreakdown(calculateBreakdownMap(topicMap))
                .difficultyBreakdown(calculateBreakdownMap(diffMap))
                .answers(answerDetails)
                .evaluatedAt(attempt.getEvaluatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ResultDtos.ClassAnalyticsDto getClassAnalytics(Long scheduleId) {
        ExamSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (SecurityUtils.isTeacher()) {
            subjectService.validateTeacherSubjectAccess(currentUserId, schedule.getExamPaper().getSubject().getId());
        }

        List<StudentEnrollment> enrollments = (schedule.getSection() != null)
                ? enrollmentRepository.findBySectionId(schedule.getSection().getId())
                : enrollmentRepository.findBySectionClassEntityId(schedule.getClassEntity().getId());

        List<ExamAttempt> attempts = attemptRepository.findByScheduleId(scheduleId);

        int totalEnrolled = enrollments.size();
        int attemptsCount = attempts.size();
        long passedCount = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();

        BigDecimal sumScores = attempts.stream()
                .map(ExamAttempt::getTotalScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgScore = attemptsCount > 0
                ? sumScores.divide(BigDecimal.valueOf(attemptsCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal highestScore = attempts.stream()
                .map(ExamAttempt::getTotalScore)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowestScore = attempts.stream()
                .map(ExamAttempt::getTotalScore)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal passPct = attemptsCount > 0
                ? BigDecimal.valueOf(passedCount * 100.0 / attemptsCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Rank student attempts descending by score
        List<ExamAttempt> sortedAttempts = new ArrayList<>(attempts);
        sortedAttempts.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));

        List<ResultDtos.StudentRankDto> rankings = new ArrayList<>();
        int rank = 1;
        for (ExamAttempt a : sortedAttempts) {
            String roll = enrollmentRepository.findByStudentId(a.getStudent().getId()).map(StudentEnrollment::getRollNumber).orElse("N/A");
            rankings.add(ResultDtos.StudentRankDto.builder()
                    .rank(rank++)
                    .studentId(a.getStudent().getId())
                    .studentName(a.getStudent().getFullName())
                    .rollNumber(roll)
                    .score(a.getTotalScore())
                    .percentage(a.getPercentage())
                    .passed(a.getPassed())
                    .build());
        }

        return ResultDtos.ClassAnalyticsDto.builder()
                .scheduleId(schedule.getId())
                .examTitle(schedule.getTitle())
                .subjectName(schedule.getExamPaper().getSubject().getName())
                .className(schedule.getClassEntity().getName())
                .totalEnrolled(totalEnrolled)
                .attemptsCount(attemptsCount)
                .passedCount((int) passedCount)
                .passPercentage(passPct)
                .averageScore(avgScore)
                .highestScore(highestScore)
                .lowestScore(lowestScore)
                .maxMarks(schedule.getMaxMarks())
                .rankings(rankings)
                .build();
    }

    public byte[] exportResultsToCsv(Long scheduleId) {
        ResultDtos.ClassAnalyticsDto analytics = getClassAnalytics(scheduleId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("Rank,Roll Number,Student Name,Score,Max Marks,Percentage,Passed");
            for (ResultDtos.StudentRankDto rank : analytics.getRankings()) {
                writer.printf("%d,%s,\"%s\",%.2f,%.2f,%.2f%%,%s%n",
                        rank.getRank(),
                        rank.getRollNumber(),
                        rank.getStudentName(),
                        rank.getScore(),
                        analytics.getMaxMarks(),
                        rank.getPercentage(),
                        Boolean.TRUE.equals(rank.getPassed()) ? "PASS" : "FAIL");
            }
            writer.flush();
        }
        return out.toByteArray();
    }

    private boolean isResultReleased(ExamSchedule schedule) {
        if (Boolean.TRUE.equals(schedule.getIsResultsReleased())) {
            return true;
        }
        if (schedule.getResultReleasePolicy() == ResultReleasePolicy.IMMEDIATE) {
            return true;
        }
        if (schedule.getResultReleasePolicy() == ResultReleasePolicy.AFTER_EXAM_WINDOW) {
            return LocalDateTime.now().isAfter(schedule.getEndTime());
        }
        return false;
    }

    private Map<String, ResultDtos.BreakdownScoreDto> calculateBreakdownMap(Map<String, List<BreakdownItem>> itemsMap) {
        Map<String, ResultDtos.BreakdownScoreDto> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<BreakdownItem>> entry : itemsMap.entrySet()) {
            List<BreakdownItem> list = entry.getValue();
            int total = list.size();
            int correct = (int) list.stream().filter(BreakdownItem::isCorrect).count();
            BigDecimal totalMarks = list.stream().map(BreakdownItem::maxMarks).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal marksObt = list.stream().map(BreakdownItem::marksAwarded).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pct = totalMarks.compareTo(BigDecimal.ZERO) > 0
                    ? marksObt.multiply(BigDecimal.valueOf(100)).divide(totalMarks, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.put(entry.getKey(), ResultDtos.BreakdownScoreDto.builder()
                    .totalQuestions(total)
                    .correctCount(correct)
                    .totalMarks(totalMarks)
                    .marksObtained(marksObt)
                    .percentage(pct)
                    .build());
        }
        return result;
    }

    private record BreakdownItem(boolean isCorrect, BigDecimal maxMarks, BigDecimal marksAwarded) {}
}
