package com.examforge.controller;

import com.examforge.dto.DocumentDtos;
import com.examforge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasAnyRole('TEACHER', 'MANAGEMENT')")
@RequiredArgsConstructor
@Tag(name = "Teacher Documents", description = "Lesson organization and document extraction")
public class DocumentController {

    private final DocumentService documentService;

    // --- Lessons ---
    @GetMapping("/lessons")
    @Operation(summary = "Get lessons by subject")
    public ResponseEntity<List<DocumentDtos.LessonDto>> getLessons(@RequestParam Long subjectId) {
        return ResponseEntity.ok(documentService.getLessonsBySubject(subjectId));
    }

    @PostMapping("/lessons")
    @Operation(summary = "Create unit/lesson for subject")
    public ResponseEntity<DocumentDtos.LessonDto> createLesson(@Valid @RequestBody DocumentDtos.CreateLessonRequest request) {
        return new ResponseEntity<>(documentService.createLesson(request), HttpStatus.CREATED);
    }

    // --- Documents ---
    @GetMapping("/documents")
    @Operation(summary = "Get all documents for subject")
    public ResponseEntity<List<DocumentDtos.DocumentDto>> getDocumentsBySubject(@RequestParam Long subjectId) {
        return ResponseEntity.ok(documentService.getDocumentsBySubject(subjectId));
    }

    @GetMapping("/lessons/{lessonId}/documents")
    @Operation(summary = "Get documents for specific lesson")
    public ResponseEntity<List<DocumentDtos.DocumentDto>> getDocumentsByLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(documentService.getDocumentsByLesson(lessonId));
    }

    @PostMapping(value = "/lessons/{lessonId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload PDF/TXT document to lesson")
    public ResponseEntity<DocumentDtos.DocumentDto> uploadDocument(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file
    ) {
        return new ResponseEntity<>(documentService.uploadDocument(lessonId, file), HttpStatus.CREATED);
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "Delete uploaded document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
