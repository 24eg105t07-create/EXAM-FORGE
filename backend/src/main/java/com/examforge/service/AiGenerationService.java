package com.examforge.service;

import com.examforge.ai.AiModels;
import com.examforge.ai.AiProvider;
import com.examforge.ai.AiProviderFactory;
import com.examforge.ai.BlueprintCalculationUtil;
import com.examforge.dto.ExamPaperDtos;
import com.examforge.dto.QuestionDtos;
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
public class AiGenerationService {

    private final ExamBlueprintRepository blueprintRepository;
    private final DocumentRepository documentRepository;
    private final AiGenerationJobRepository jobRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionOptionRepository optionRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperQuestionRepository paperQuestionRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final AiProviderFactory aiProviderFactory;
    private final AuditLogService auditLogService;

    @Transactional
    public ExamPaperDtos.ExamPaperDto generateQuestionPaper(QuestionDtos.GenerateQuestionsRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ExamBlueprint blueprint = blueprintRepository.findById(request.getBlueprintId())
                .orElseThrow(() -> new ResourceNotFoundException("Blueprint not found: " + request.getBlueprintId()));

        subjectService.validateTeacherSubjectAccess(currentUserId, blueprint.getSubject().getId());

        User teacher = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AiProvider provider = aiProviderFactory.getProvider();
        log.info("Starting question paper generation via provider: {}", provider.getProviderName());

        // 1. Gather lesson contexts and documents
        List<AiModels.LessonContext> lessonContexts = new ArrayList<>();
        for (BlueprintLessonSelection selection : blueprint.getLessonSelections()) {
            Lesson lesson = selection.getLesson();
            List<Document> documents = documentRepository.findByLessonId(lesson.getId());

            StringBuilder docSnippets = new StringBuilder();
            for (Document doc : documents) {
                if (doc.getProcessingStatus() == ProcessingStatus.COMPLETED && doc.getExtractedText() != null) {
                    docSnippets.append("--- Document: ").append(doc.getFileName()).append(" ---\n");
                    docSnippets.append(doc.getExtractedText().substring(0, Math.min(doc.getExtractedText().length(), 4000)));
                    docSnippets.append("\n\n");
                }
            }

            lessonContexts.add(AiModels.LessonContext.builder()
                    .lessonId(lesson.getId())
                    .title(lesson.getTitle())
                    .unitNumber(lesson.getUnitNumber())
                    .targetQuestionCount(selection.getQuestionCount())
                    .extractedDocumentContent(docSnippets.toString())
                    .build());
        }

        // 2. Compute exact difficulty counts
        BlueprintCalculationUtil.DifficultyDistribution diffDist = BlueprintCalculationUtil.calculateDifficultyDistribution(
                blueprint.getTotalQuestions(),
                blueprint.getEasyPercentage(),
                blueprint.getMediumPercentage(),
                blueprint.getHardPercentage()
        );

        AiModels.QuestionGenerationRequest aiRequest = AiModels.QuestionGenerationRequest.builder()
                .subjectName(blueprint.getSubject().getName())
                .subjectCode(blueprint.getSubject().getCode())
                .lessons(lessonContexts)
                .totalQuestions(blueprint.getTotalQuestions())
                .easyCount(diffDist.easyCount())
                .mediumCount(diffDist.mediumCount())
                .hardCount(diffDist.hardCount())
                .mcqCount(blueprint.getMcqCount())
                .trueFalseCount(blueprint.getTrueFalseCount())
                .shortAnswerCount(blueprint.getShortAnswerCount())
                .marksPerQuestion(blueprint.getMarksPerQuestion())
                .additionalInstructions(request.getAdditionalInstructions())
                .build();

        // 3. Record Generation Job
        AiGenerationJob job = AiGenerationJob.builder()
                .blueprint(blueprint)
                .initiatedBy(teacher)
                .provider(provider.getProviderName())
                .status("PROCESSING")
                .promptSummary("Generating " + blueprint.getTotalQuestions() + " questions for " + blueprint.getSubject().getName())
                .build();
        job = jobRepository.save(job);

        // 4. Execute AI Generation
        AiModels.GenerationResult result = provider.generateQuestions(aiRequest);

        job.setStatus(result.isSuccess() ? "COMPLETED" : "FAILED");
        job.setRawResponse(result.getRawResponse());
        job.setErrorMessage(result.getErrorMessage());
        job.setTotalQuestionsGenerated(result.getQuestions() != null ? result.getQuestions().size() : 0);
        jobRepository.save(job);

        if (!result.isSuccess() || result.getQuestions() == null || result.getQuestions().isEmpty()) {
            throw new BadRequestException("AI generation failed: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown error"));
        }

        // 5. Create Draft Exam Paper (Never auto-publish!)
        ExamPaper paper = ExamPaper.builder()
                .blueprint(blueprint)
                .subject(blueprint.getSubject())
                .creator(teacher)
                .title(blueprint.getTitle() + " (Draft Paper)")
                .totalQuestions(result.getQuestions().size())
                .maxMarks(blueprint.getMaxMarks())
                .durationMinutes(blueprint.getDurationMinutes())
                .status(PaperStatus.DRAFT)
                .version(1)
                .isLocked(false)
                .build();
        paper = examPaperRepository.save(paper);

        // 6. Save generated questions into Question Bank & Exam Paper Questions
        int order = 1;
        Set<String> seenQuestions = new HashSet<>();

        for (AiModels.GeneratedQuestion gq : result.getQuestions()) {
            // Check for duplicates
            String normalizedText = gq.getQuestionText().trim().toLowerCase();
            if (seenQuestions.contains(normalizedText)) {
                log.warn("Skipping exact duplicate question in generation output: {}", gq.getQuestionText());
                continue;
            }
            seenQuestions.add(normalizedText);

            Lesson lesson = lessonContexts.stream()
                    .filter(l -> l.getLessonId().equals(gq.getLessonId()))
                    .findFirst()
                    .map(lc -> blueprint.getLessonSelections().stream()
                            .map(BlueprintLessonSelection::getLesson)
                            .filter(l -> l.getId().equals(lc.getLessonId()))
                            .findFirst().orElse(blueprint.getLessonSelections().getFirst().getLesson()))
                    .orElse(blueprint.getLessonSelections().getFirst().getLesson());

            QuestionBank qb = QuestionBank.builder()
                    .subject(blueprint.getSubject())
                    .lesson(lesson)
                    .questionText(gq.getQuestionText())
                    .questionType(gq.getQuestionType() != null ? gq.getQuestionType() : QuestionType.MCQ)
                    .difficulty(gq.getDifficulty() != null ? gq.getDifficulty() : Difficulty.MEDIUM)
                    .marks(gq.getMarks() != null ? gq.getMarks() : blueprint.getMarksPerQuestion())
                    .explanation(gq.getExplanation())
                    .sourceReference(gq.getSourceReference())
                    .isAiGenerated(true)
                    .createdBy(teacher)
                    .build();

            if (gq.getOptions() != null) {
                int optOrder = 1;
                for (AiModels.GeneratedOption opt : gq.getOptions()) {
                    QuestionOption option = QuestionOption.builder()
                            .question(qb)
                            .optionLabel(opt.getOptionLabel())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.isCorrect())
                            .orderIndex(optOrder++)
                            .build();
                    qb.getOptions().add(option);
                }
            }

            QuestionBank savedQb = questionBankRepository.save(qb);

            ExamPaperQuestion epq = ExamPaperQuestion.builder()
                    .examPaper(paper)
                    .question(savedQb)
                    .questionOrder(order++)
                    .marks(savedQb.getMarks())
                    .build();
            paper.getPaperQuestions().add(epq);
        }

        examPaperRepository.save(paper);

        auditLogService.logAction("GENERATE_QUESTION_PAPER", "EXAM_PAPER", paper.getId(),
                "AI generated " + paper.getPaperQuestions().size() + " questions as draft using provider " + provider.getProviderName(), null);

        return mapToPaperDto(paper);
    }

    private ExamPaperDtos.ExamPaperDto mapToPaperDto(ExamPaper paper) {
        List<ExamPaperDtos.PaperQuestionDto> questions = paper.getPaperQuestions().stream().map(pq -> {
            QuestionBank q = pq.getQuestion();
            List<QuestionDtos.OptionDto> options = q.getOptions().stream().map(o ->
                    QuestionDtos.OptionDto.builder()
                            .id(o.getId())
                            .optionLabel(o.getOptionLabel())
                            .optionText(o.getOptionText())
                            .isCorrect(o.getIsCorrect())
                            .orderIndex(o.getOrderIndex())
                            .build()
            ).toList();

            QuestionDtos.QuestionDto qDto = QuestionDtos.QuestionDto.builder()
                    .id(q.getId())
                    .subjectId(q.getSubject().getId())
                    .lessonId(q.getLesson().getId())
                    .lessonTitle(q.getLesson().getTitle())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .marks(q.getMarks())
                    .explanation(q.getExplanation())
                    .sourceReference(q.getSourceReference())
                    .isAiGenerated(q.getIsAiGenerated())
                    .options(options)
                    .createdAt(q.getCreatedAt())
                    .build();

            return ExamPaperDtos.PaperQuestionDto.builder()
                    .id(pq.getId())
                    .questionOrder(pq.getQuestionOrder())
                    .marks(pq.getMarks())
                    .question(qDto)
                    .build();
        }).toList();

        return ExamPaperDtos.ExamPaperDto.builder()
                .id(paper.getId())
                .blueprintId(paper.getBlueprint() != null ? paper.getBlueprint().getId() : null)
                .subjectId(paper.getSubject().getId())
                .subjectName(paper.getSubject().getName())
                .subjectCode(paper.getSubject().getCode())
                .creatorId(paper.getCreator().getId())
                .creatorName(paper.getCreator().getFullName())
                .title(paper.getTitle())
                .totalQuestions(paper.getTotalQuestions())
                .maxMarks(paper.getMaxMarks())
                .durationMinutes(paper.getDurationMinutes())
                .status(paper.getStatus())
                .version(paper.getVersion())
                .isLocked(paper.getIsLocked())
                .approvedAt(paper.getApprovedAt())
                .approvedByName(paper.getApprovedBy() != null ? paper.getApprovedBy().getFullName() : null)
                .questions(questions)
                .createdAt(paper.getCreatedAt())
                .build();
    }
}
