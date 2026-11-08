package com.nas.webserver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.nas.webserver.service.FileService;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() throws IOException {
        fileService = new FileService(tempDir.toString());
    }

    // Test uploading a file
    @Test
    void testUploadFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes());

        fileService.uploadFile(file);

        assertTrue(Files.exists(tempDir.resolve("test.txt")));
    }

    // Test listing files
    @Test
    void testListFiles() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Hello");

        assertEquals(1, fileService.listFiles().size());
        assertTrue(fileService.listFiles().contains("test.txt"));
    }

    // Test downloading a file
    @Test
    void testDownloadFile() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Hello");

        Path result = fileService.downloadFile("test.txt");

        assertEquals("Hello", Files.readString(result));
    }

    // Test deleting a file
    @Test
    void testDeleteFile() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Hello");

        fileService.deleteFile("test.txt");

        assertFalse(Files.exists(tempDir.resolve("test.txt")));
    }

    // Test rejecting duplicate files
    @Test
    void testDuplicateFile() throws IOException {
        Files.writeString(tempDir.resolve("test.txt"), "Original");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "New".getBytes());

        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> fileService.uploadFile(file));

        assertEquals("Original", Files.readString(tempDir.resolve("test.txt")));
    }

    // Test rejecting unsafe filenames
    @Test
    void testInvalidFilename() {
        assertThrows(IllegalArgumentException.class,
                () -> fileService.downloadFile("../secret.txt"));
    }
}