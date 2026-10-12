package com.examforge.service;

import com.examforge.document.DocumentStorageService;
import com.examforge.document.PdfTextExtractorService;
import com.examforge.dto.DocumentDtos;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final LessonRepository lessonRepository;
    private final DocumentRepository documentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ExamPaperRepository examPaperRepository;
    private final SubjectService subjectService;
    private final DocumentStorageService storageService;
    private final PdfTextExtractorService extractorService;
    private final AuditLogService auditLogService;

    // --- Lessons ---
    @Transactional(readOnly = true)
    public List<DocumentDtos.LessonDto> getLessonsBySubject(Long subjectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, subjectId);

        return lessonRepository.findBySubjectIdOrderByOrderIndexAsc(subjectId).stream()
                .map(this::mapToLessonDto)
                .toList();
    }

    @Transactional
    public DocumentDtos.LessonDto createLesson(DocumentDtos.CreateLessonRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, request.getSubjectId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.getSubjectId()));

        Lesson lesson = Lesson.builder()
                .subject(subject)
                .unitNumber(request.getUnitNumber())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 1)
                .build();
        Lesson saved = lessonRepository.save(lesson);

        auditLogService.logAction("CREATE_LESSON", "LESSON", saved.getId(),
                "Created lesson: " + saved.getTitle() + " for subject " + subject.getCode(), null);

        return mapToLessonDto(saved);
    }

    // --- Documents ---
    @Transactional
    public DocumentDtos.DocumentDto uploadDocument(Long lessonId, MultipartFile file) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        subjectService.validateTeacherSubjectAccess(currentUserId, lesson.getSubject().getId());

        User uploader = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Store file safely
        DocumentStorageService.StoredFileResult storedResult = storageService.storeFile(file);

        Document doc = Document.builder()
                .lesson(lesson)
                .subject(lesson.getSubject())
                .uploader(uploader)
                .fileName(storedResult.originalName())
                .storedFilePath(storedResult.storedPath())
                .fileSize(storedResult.size())
                .mimeType(storedResult.mimeType() != null ? storedResult.mimeType() : "application/octet-stream")
                .processingStatus(ProcessingStatus.PROCESSING)
                .build();

        Document savedDoc = documentRepository.save(doc);

        // Synchronously extract text
        try {
            File diskFile = new File(savedDoc.getStoredFilePath());
            PdfTextExtractorService.ExtractionResult extraction = extractorService.extractText(diskFile, savedDoc.getMimeType());

            if (extraction.success()) {
                savedDoc.setProcessingStatus(ProcessingStatus.COMPLETED);
                savedDoc.setExtractedText(extraction.text());
                savedDoc.setErrorMessage(null);
            } else {
                savedDoc.setProcessingStatus(ProcessingStatus.FAILED);
                savedDoc.setErrorMessage(extraction.error());
            }
        } catch (Exception e) {
            savedDoc.setProcessingStatus(ProcessingStatus.FAILED);
            savedDoc.setErrorMessage("Extraction failure: " + e.getMessage());
        }

        Document finalDoc = documentRepository.save(savedDoc);

        auditLogService.logAction("UPLOAD_DOCUMENT", "DOCUMENT", finalDoc.getId(),
                "Uploaded document " + finalDoc.getFileName() + " (status: " + finalDoc.getProcessingStatus() + ")", null);

        return mapToDocumentDto(finalDoc);
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentDto> getDocumentsBySubject(Long subjectId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, subjectId);

        return documentRepository.findBySubjectId(subjectId).stream()
                .map(this::mapToDocumentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentDto> getDocumentsByLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        subjectService.validateTeacherSubjectAccess(currentUserId, lesson.getSubject().getId());

        return documentRepository.findByLessonId(lessonId).stream()
                .map(this::mapToDocumentDto)
                .toList();
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        subjectService.validateTeacherSubjectAccess(currentUserId, doc.getSubject().getId());

        // Check if teacher is owner or admin
        if (!SecurityUtils.isManagement() && !doc.getUploader().getId().equals(currentUserId)) {
            throw new UnauthorizedException("You can only delete your own uploaded documents");
        }

        // Check if locked by approved or scheduled examination paper
        List<ExamPaper> approvedPapers = examPaperRepository.findBySubjectIdAndStatus(doc.getSubject().getId(), PaperStatus.APPROVED);
        if (!approvedPapers.isEmpty()) {
            throw new BadRequestException("Cannot delete document because approved exam papers depend on this subject material.");
        }

        storageService.deleteFile(doc.getStoredFilePath());
        documentRepository.delete(doc);

        auditLogService.logAction("DELETE_DOCUMENT", "DOCUMENT", documentId,
                "Deleted document " + doc.getFileName(), null);
    }

    private DocumentDtos.LessonDto mapToLessonDto(Lesson l) {
        int docCount = documentRepository.findByLessonId(l.getId()).size();
        return DocumentDtos.LessonDto.builder()
                .id(l.getId())
                .subjectId(l.getSubject().getId())
                .subjectName(l.getSubject().getName())
                .unitNumber(l.getUnitNumber())
                .title(l.getTitle())
                .description(l.getDescription())
                .orderIndex(l.getOrderIndex())
                .documentCount(docCount)
                .build();
    }

    private DocumentDtos.DocumentDto mapToDocumentDto(Document d) {
        String snippet = null;
        if (d.getExtractedText() != null) {
            snippet = d.getExtractedText().length() > 250
                    ? d.getExtractedText().substring(0, 250) + "..."
                    : d.getExtractedText();
        }
        return DocumentDtos.DocumentDto.builder()
                .id(d.getId())
                .lessonId(d.getLesson().getId())
                .lessonTitle(d.getLesson().getTitle())
                .subjectId(d.getSubject().getId())
                .subjectName(d.getSubject().getName())
                .fileName(d.getFileName())
                .fileSize(d.getFileSize())
                .mimeType(d.getMimeType())
                .processingStatus(d.getProcessingStatus())
                .extractedSnippet(snippet)
                .errorMessage(d.getErrorMessage())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
