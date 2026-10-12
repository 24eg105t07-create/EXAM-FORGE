package com.examforge.document;

import com.examforge.config.ApplicationProperties;
import com.examforge.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final ApplicationProperties properties;
    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(properties.getUpload().getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("Initialized upload directory at: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }

    public StoredFileResult storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.txt");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid path traversal filename: " + originalFilename);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!extension.equals("pdf") && !extension.equals("txt")) {
            throw new BadRequestException("Only PDF and TXT documents are supported. Received: " + extension);
        }

        long maxBytes = (long) properties.getUpload().getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(String.format("File size (%d MB) exceeds limit of %d MB",
                    file.getSize() / (1024 * 1024), properties.getUpload().getMaxFileSizeMb()));
        }

        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destination = uploadPath.resolve(uniqueFileName).normalize();

        if (!destination.startsWith(uploadPath)) {
            throw new BadRequestException("Path traversal attempt detected!");
        }

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFileResult(originalFilename, destination.toString(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public void deleteFile(String storedFilePath) {
        try {
            Path path = Paths.get(storedFilePath).normalize();
            if (path.startsWith(uploadPath) && Files.exists(path)) {
                Files.delete(path);
                log.info("Deleted file at: {}", path);
            }
        } catch (IOException e) {
            log.warn("Could not delete stored file: {}", storedFilePath, e);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }

    public record StoredFileResult(String originalName, String storedPath, long size, String mimeType) {}
}
