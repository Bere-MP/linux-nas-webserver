package com.nas.webserver.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final Path storagePath;

    public FileService(@Value("${nas.storage.path}") String storage) throws IOException {
        storagePath = Paths.get(storage).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    // Validate filenames and keep files inside storage
    private Path resolveFile(String filename) {
        if (filename == null || filename.isBlank()
                || filename.equals(".") || filename.equals("..")
                || filename.contains("/") || filename.contains("\\")
                || filename.contains(":")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        Path file = storagePath.resolve(filename).normalize();

        if (!file.getParent().equals(storagePath)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        return file;
    }

    // Upload without overwriting existing files
    public void uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        Path destination = resolveFile(file.getOriginalFilename());

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination);
        }
    }

    // List stored files
    public List<String> listFiles() throws IOException {
        try (Stream<Path> files = Files.list(storagePath)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    // Retrieve a file for downloading
    public Path downloadFile(String filename) throws IOException {
        Path file = resolveFile(filename);

        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(filename);
        }

        return file;
    }

    // Delete a stored file
    public void deleteFile(String filename) throws IOException {
        Path file = resolveFile(filename);

        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(filename);
        }

        Files.delete(file);
    }
}