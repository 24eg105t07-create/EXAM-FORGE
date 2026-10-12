package com.examforge.service;

import com.examforge.ai.BlueprintCalculationUtil;
import com.examforge.dto.BlueprintDtos;
import com.examforge.entity.*;
import com.examforge.exception.BadRequestException;
import com.examforge.exception.ResourceNotFoundException;
import com.examforge.repository.*;
import com.examforge.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlueprintService {

    private final ExamBlueprintRepository blueprintRepository;
    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final AuditLogService auditLogService;

    @Transactional
    public BlueprintDtos.BlueprintDto createBlueprint(BlueprintDtos.CreateBlueprintRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, request.getSubjectId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. Validate difficulty percentages
        BlueprintCalculationUtil.DifficultyDistribution diffDist = BlueprintCalculationUtil.calculateDifficultyDistribution(
                request.getTotalQuestions(),
                request.getEasyPercentage(),
                request.getMediumPercentage(),
                request.getHardPercentage()
        );

        // 2. Validate question type counts sum to total questions
        int qTypeSum = request.getMcqCount() + request.getTrueFalseCount() + request.getShortAnswerCount();
        if (qTypeSum != request.getTotalQuestions()) {
            throw new BadRequestException(String.format(
                    "Question type counts (MCQ %d + True/False %d + Short Answer %d = %d) must equal total questions %d",
                    request.getMcqCount(), request.getTrueFalseCount(), request.getShortAnswerCount(),
                    qTypeSum, request.getTotalQuestions()));
        }

        // 3. Validate lesson selections and percentages
        Map<Long, Integer> lessonPctMap = new HashMap<>();
        for (BlueprintDtos.LessonSelectionItem item : request.getLessonSelections()) {
            lessonPctMap.put(item.getLessonId(), item.getTargetPercentage());
        }
        Map<Long, Integer> lessonDist = BlueprintCalculationUtil.calculateLessonDistribution(
                request.getTotalQuestions(), lessonPctMap);

        ExamBlueprint bp = ExamBlueprint.builder()
                .subject(subject)
                .creator(creator)
                .title(request.getTitle().trim())
                .totalQuestions(request.getTotalQuestions())
                .durationMinutes(request.getDurationMinutes())
                .maxMarks(request.getMaxMarks() != null ? request.getMaxMarks() : BigDecimal.valueOf(request.getTotalQuestions()))
                .easyPercentage(request.getEasyPercentage())
                .mediumPercentage(request.getMediumPercentage())
                .hardPercentage(request.getHardPercentage())
                .mcqCount(request.getMcqCount())
                .trueFalseCount(request.getTrueFalseCount())
                .shortAnswerCount(request.getShortAnswerCount())
                .marksPerQuestion(request.getMarksPerQuestion() != null ? request.getMarksPerQuestion() : BigDecimal.valueOf(1.0))
                .instructions(request.getInstructions())
                .status("DRAFT")
                .build();

        for (Map.Entry<Long, Integer> entry : lessonDist.entrySet()) {
            Lesson lesson = lessonRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + entry.getKey()));
            BlueprintLessonSelection sel = BlueprintLessonSelection.builder()
                    .blueprint(bp)
                    .lesson(lesson)
                    .targetPercentage(lessonPctMap.get(entry.getKey()))
                    .questionCount(entry.getValue())
                    .build();
            bp.getLessonSelections().add(sel);
        }

        ExamBlueprint saved = blueprintRepository.save(bp);

        auditLogService.logAction("CREATE_BLUEPRINT", "EXAM_BLUEPRINT", saved.getId(),
                "Created exam blueprint '" + saved.getTitle() + "' with " + saved.getTotalQuestions() + " questions", null);

        return mapToDto(saved, diffDist);
    }

    @Transactional(readOnly = true)
    public List<BlueprintDtos.BlueprintDto> getBlueprintsBySubject(Long subjectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, subjectId);

        return blueprintRepository.findBySubjectId(subjectId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlueprintDtos.BlueprintDto getBlueprintById(Long id) {
        ExamBlueprint bp = blueprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blueprint not found: " + id));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, bp.getSubject().getId());

        return mapToDto(bp);
    }

    public BlueprintDtos.BlueprintDto mapToDto(ExamBlueprint bp) {
        BlueprintCalculationUtil.DifficultyDistribution diffDist = BlueprintCalculationUtil.calculateDifficultyDistribution(
                bp.getTotalQuestions(), bp.getEasyPercentage(), bp.getMediumPercentage(), bp.getHardPercentage());
        return mapToDto(bp, diffDist);
    }

    private BlueprintDtos.BlueprintDto mapToDto(ExamBlueprint bp, BlueprintCalculationUtil.DifficultyDistribution diffDist) {
        List<BlueprintDtos.BlueprintLessonBreakdownDto> breakdowns = bp.getLessonSelections().stream().map(s ->
                BlueprintDtos.BlueprintLessonBreakdownDto.builder()
                        .lessonId(s.getLesson().getId())
                        .lessonTitle(s.getLesson().getTitle())
                        .targetPercentage(s.getTargetPercentage())
                        .questionCount(s.getQuestionCount())
                        .build()
        ).toList();

        return BlueprintDtos.BlueprintDto.builder()
                .id(bp.getId())
                .subjectId(bp.getSubject().getId())
                .subjectName(bp.getSubject().getName())
                .title(bp.getTitle())
                .totalQuestions(bp.getTotalQuestions())
                .durationMinutes(bp.getDurationMinutes())
                .maxMarks(bp.getMaxMarks())
                .easyPercentage(bp.getEasyPercentage())
                .mediumPercentage(bp.getMediumPercentage())
                .hardPercentage(bp.getHardPercentage())
                .easyQuestionCount(diffDist.easyCount())
                .mediumQuestionCount(diffDist.mediumCount())
                .hardQuestionCount(diffDist.hardCount())
                .mcqCount(bp.getMcqCount())
                .trueFalseCount(bp.getTrueFalseCount())
                .shortAnswerCount(bp.getShortAnswerCount())
                .marksPerQuestion(bp.getMarksPerQuestion())
                .instructions(bp.getInstructions())
                .status(bp.getStatus())
                .lessonBreakdowns(breakdowns)
                .createdAt(bp.getCreatedAt())
                .build();
    }
}
