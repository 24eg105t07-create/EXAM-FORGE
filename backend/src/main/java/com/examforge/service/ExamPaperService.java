package com.examforge.service;

import com.examforge.ai.AiModels;
import com.examforge.ai.AiProvider;
import com.examforge.ai.AiProviderFactory;
import com.examforge.dto.ExamPaperDtos;
import com.examforge.dto.QuestionDtos;
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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPaperService {

    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperQuestionRepository paperQuestionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final AiProviderFactory aiProviderFactory;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ExamPaperDtos.ExamPaperDto getPaperById(Long paperId) {
        ExamPaper paper = examPaperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper not found: " + paperId));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isManagement()) {
            subjectService.validateTeacherSubjectAccess(currentUserId, paper.getSubject().getId());
        }

        return mapToPaperDto(paper);
    }

    @Transactional(readOnly = true)
    public List<ExamPaperDtos.ExamPaperDto> getPapersBySubject(Long subjectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isManagement()) {
            subjectService.validateTeacherSubjectAccess(currentUserId, subjectId);
        }

        return examPaperRepository.findBySubjectId(subjectId).stream()
                .map(this::mapToPaperDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamPaperDtos.ExamPaperDto> getApprovedPapers() {
        return examPaperRepository.findByStatus(PaperStatus.APPROVED).stream()
                .map(this::mapToPaperDto)
                .toList();
    }

    @Transactional
    public ExamPaperDtos.ExamPaperDto updateQuestionInPaper(Long paperId, Long questionId, QuestionDtos.CreateQuestionRequest request) {
        ExamPaper paper = examPaperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper not found: " + paperId));

        assertPaperEditable(paper);

        QuestionBank question = questionBankRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        question.setQuestionText(request.getQuestionText().trim());
        question.setDifficulty(request.getDifficulty());
        if (request.getMarks() != null) question.setMarks(request.getMarks());
        question.setExplanation(request.getExplanation());
        question.setSourceReference(request.getSourceReference());

        // Update options
        if (request.getOptions() != null) {
            question.getOptions().clear();
            int optOrder = 1;
            for (QuestionDtos.OptionDto opt : request.getOptions()) {
                QuestionOption qo = QuestionOption.builder()
                        .question(question)
                        .optionLabel(opt.getOptionLabel())
                        .optionText(opt.getOptionText())
                        .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                        .orderIndex(optOrder++)
                        .build();
                question.getOptions().add(qo);
            }
        }

        questionBankRepository.save(question);
        auditLogService.logAction("UPDATE_PAPER_QUESTION", "QUESTION_BANK", question.getId(),
                "Updated question in paper " + paper.getTitle(), null);

        return mapToPaperDto(paper);
    }

    @Transactional
    public ExamPaperDtos.ExamPaperDto deleteQuestionFromPaper(Long paperId, Long questionId) {
        ExamPaper paper = examPaperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper not found: " + paperId));

        assertPaperEditable(paper);

        ExamPaperQuestion epq = paper.getPaperQuestions().stream()
                .filter(q -> q.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found in paper"));

        paper.getPaperQuestions().remove(epq);
        paperQuestionRepository.delete(epq);

        // Re-index question order
        int order = 1;
        for (ExamPaperQuestion q : paper.getPaperQuestions()) {
            q.setQuestionOrder(order++);
        }
        paper.setTotalQuestions(paper.getPaperQuestions().size());
        examPaperRepository.save(paper);

        auditLogService.logAction("DELETE_PAPER_QUESTION", "EXAM_PAPER", paper.getId(),
                "Removed question ID " + questionId + " from paper", null);

        return mapToPaperDto(paper);
    }

    @Transactional
    public ExamPaperDtos.ExamPaperDto regenerateSingleQuestion(Long paperId, Long questionId, String instructions) {
        ExamPaper paper = examPaperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper not found: " + paperId));

        assertPaperEditable(paper);

        ExamPaperQuestion epq = paper.getPaperQuestions().stream()
                .filter(q -> q.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not in paper: " + questionId));

        QuestionBank existing = epq.getQuestion();
        AiProvider provider = aiProviderFactory.getProvider();

        AiModels.QuestionGenerationRequest aiReq = AiModels.QuestionGenerationRequest.builder()
                .subjectName(paper.getSubject().getName())
                .subjectCode(paper.getSubject().getCode())
                .lessons(List.of(AiModels.LessonContext.builder()
                        .lessonId(existing.getLesson().getId())
                        .title(existing.getLesson().getTitle())
                        .unitNumber(existing.getLesson().getUnitNumber())
                        .targetQuestionCount(1)
                        .extractedDocumentContent("")
                        .build()))
                .totalQuestions(1)
                .marksPerQuestion(existing.getMarks())
                .build();

        AiModels.GeneratedQuestion existingGq = AiModels.GeneratedQuestion.builder()
                .lessonId(existing.getLesson().getId())
                .difficulty(existing.getDifficulty())
                .questionType(existing.getQuestionType())
                .marks(existing.getMarks())
                .questionText(existing.getQuestionText())
                .build();

        AiModels.GeneratedQuestion regenerated = provider.regenerateQuestion(aiReq, existingGq, instructions);

        // Update QuestionBank entry
        existing.setQuestionText(regenerated.getQuestionText());
        existing.setExplanation(regenerated.getExplanation());
        existing.setSourceReference(regenerated.getSourceReference());

        if (regenerated.getOptions() != null && !regenerated.getOptions().isEmpty()) {
            existing.getOptions().clear();
            int optOrder = 1;
            for (AiModels.GeneratedOption gOpt : regenerated.getOptions()) {
                QuestionOption option = QuestionOption.builder()
                        .question(existing)
                        .optionLabel(gOpt.getOptionLabel())
                        .optionText(gOpt.getOptionText())
                        .isCorrect(gOpt.isCorrect())
                        .orderIndex(optOrder++)
                        .build();
                existing.getOptions().add(option);
            }
        }

        questionBankRepository.save(existing);
        auditLogService.logAction("REGENERATE_QUESTION", "QUESTION_BANK", existing.getId(),
                "Regenerated question using AI provider " + provider.getProviderName(), null);

        return mapToPaperDto(paper);
    }

    @Transactional
    public ExamPaperDtos.ExamPaperDto approvePaper(Long paperId) {
        ExamPaper paper = examPaperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam paper not found: " + paperId));

        assertPaperEditable(paper);

        if (paper.getPaperQuestions().isEmpty()) {
            throw new BadRequestException("Cannot approve an empty examination paper");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        User approver = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        paper.setStatus(PaperStatus.APPROVED);
        paper.setIsLocked(true);
        paper.setApprovedAt(LocalDateTime.now());
        paper.setApprovedBy(approver);

        ExamPaper saved = examPaperRepository.save(paper);

        auditLogService.logAction("APPROVE_EXAM_PAPER", "EXAM_PAPER", saved.getId(),
                "Approved question paper '" + saved.getTitle() + "' (Version: " + saved.getVersion() + ")", null);

        return mapToPaperDto(saved);
    }

    private void assertPaperEditable(ExamPaper paper) {
        if (paper.getIsLocked() || paper.getStatus() == PaperStatus.APPROVED || paper.getStatus() == PaperStatus.PUBLISHED) {
            throw new BadRequestException("Exam paper is locked and cannot be edited. It has already been approved or scheduled.");
        }
    }

    public ExamPaperDtos.ExamPaperDto mapToPaperDto(ExamPaper paper) {
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
