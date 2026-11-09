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

    public FileService(@Value("${nas.storage.path}") String storage)
            throws IOException {
        storagePath = Paths.get(storage).toAbsolutePath().normalize();
        Files.createDirectories(storagePath);
    }

    // Validate a file or folder name
    private void validateName(String name) {
        if (name == null || name.isBlank()
                || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\")
                || name.contains(":")) {
            throw new IllegalArgumentException("Invalid name");
        }
    }

    // Find the storage folder
    private Path resolveFolder(String folder) throws IOException {
        if (folder == null || folder.isBlank()) {
            return storagePath;
        }

        validateName(folder);

        Path directory = storagePath.resolve(folder).normalize();

        if (!directory.getParent().equals(storagePath)) {
            throw new IllegalArgumentException("Invalid folder path");
        }

        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(folder);
        }

        return directory;
    }

    // Find a file inside the selected folder
    private Path resolveFile(String filename, String folder)
            throws IOException {
        validateName(filename);

        Path directory = resolveFolder(folder);
        Path file = directory.resolve(filename).normalize();

        if (!file.getParent().equals(directory)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        return file;
    }

    // Upload a file to the main storage
    public void uploadFile(MultipartFile file) throws IOException {
        uploadFile(file, null);
    }

    // Upload a file inside a folder
    public void uploadFile(MultipartFile file, String folder)
            throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        Path destination = resolveFile(file.getOriginalFilename(), folder);

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination);
        }
    }

    // List files in the main storage
    public List<String> listFiles() throws IOException {
        return listFiles(null);
    }

    // List files inside a folder
    public List<String> listFiles(String folder) throws IOException {
        Path directory = resolveFolder(folder);

        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(path -> Files.isRegularFile(
                            path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    // Download a file from main storage
    public Path downloadFile(String filename) throws IOException {
        return downloadFile(filename, null);
    }

    // Download a file from a folder
    public Path downloadFile(String filename, String folder)
            throws IOException {
        Path file = resolveFile(filename, folder);

        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(filename);
        }

        return file;
    }

    // Delete a file from main storage
    public void deleteFile(String filename) throws IOException {
        deleteFile(filename, null);
    }

    // Delete a file from a folder
    public void deleteFile(String filename, String folder)
            throws IOException {
        Path file = resolveFile(filename, folder);

        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(filename);
        }

        Files.delete(file);
    }
}
