package com.examforge.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
@Service
public class PdfTextExtractorService {

    public ExtractionResult extractText(File file, String mimeType) {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".txt") || (mimeType != null && mimeType.contains("text/plain"))) {
            return extractFromTxt(file);
        } else if (filename.endsWith(".pdf") || (mimeType != null && mimeType.contains("pdf"))) {
            return extractFromPdf(file);
        } else {
            return new ExtractionResult(false, null, "Unsupported file format for text extraction: " + filename);
        }
    }

    private ExtractionResult extractFromTxt(File file) {
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (text.trim().isEmpty()) {
                return new ExtractionResult(false, null, "Uploaded text document is empty");
            }
            return new ExtractionResult(true, text.trim(), null);
        } catch (IOException e) {
            log.error("Failed to read text file: {}", file.getAbsolutePath(), e);
            return new ExtractionResult(false, null, "Error reading text document: " + e.getMessage());
        }
    }

    private ExtractionResult extractFromPdf(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                return new ExtractionResult(false, null, "Encrypted/password-protected PDFs are not supported");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                // Handled scanned PDFs gracefully per requirement
                return new ExtractionResult(false, null,
                        "OCR is not supported in the initial version. The PDF contains no extractable digital text layer (likely a scanned image).");
            }

            return new ExtractionResult(true, text.trim(), null);
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", file.getAbsolutePath(), e);
            return new ExtractionResult(false, null, "PDF text extraction failed: " + e.getMessage());
        }
    }

    public record ExtractionResult(boolean success, String text, String error) {}
}
