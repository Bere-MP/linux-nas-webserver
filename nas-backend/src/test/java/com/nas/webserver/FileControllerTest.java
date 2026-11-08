
package com.nas.webserver;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.nas.webserver.controller.FileController;
import com.nas.webserver.service.FileService;

class FileControllerTest {

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        FileService fileService = new FileService(tempDir.toString());

        mockMvc = MockMvcBuilders.standaloneSetup(
                new FileController(fileService)).build();
    }

    // Test uploading a file
    @Test
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(content().string("File uploaded successfully"));

        assertEquals("Hello", Files.readString(tempDir.resolve("test.txt")));
    }

    // Test listing files
    @Test
    void testListFiles() throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "Hello");

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("test.txt"));
    }

    // Test downloading a file
    @Test
    void testDownloadFile() throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "Hello");

        mockMvc.perform(get("/api/files/download/test.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().bytes("Hello".getBytes()));
    }

    // Test deleting a file
    @Test
    void testDeleteFile() throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "Hello");

        mockMvc.perform(delete("/api/files/test.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("File deleted successfully"));

        assertFalse(Files.exists(tempDir.resolve("test.txt")));
    }

    // Test rejecting duplicate uploads
    @Test
    void testDuplicateFile() throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "Original");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "New".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isConflict());

        assertEquals("Original", Files.readString(tempDir.resolve("test.txt")));
    }

    // Test downloading a missing file
    @Test
    void testMissingFile() throws Exception {
        mockMvc.perform(get("/api/files/download/missing.txt"))
                .andExpect(status().isNotFound());
    }

    // Test rejecting unsafe filenames
    @Test
    void testInvalidFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../secret.txt", "text/plain", "Test".getBytes());

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest());
    }
}
