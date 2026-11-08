package com.nas.webserver;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.nas.webserver.controller.FolderController;
import com.nas.webserver.service.FolderService;

class FolderControllerTest {

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        FolderService folderService =
                new FolderService(tempDir.toString());

        mockMvc = MockMvcBuilders.standaloneSetup(
                new FolderController(folderService)).build();
    }

    // Test creating a folder
    @Test
    void testCreateFolder() throws Exception {
        mockMvc.perform(post("/api/folders")
                .param("name", "Documents"))
                .andExpect(status().isCreated())
                .andExpect(content().string(
                        "Folder created successfully"));

        assertTrue(Files.isDirectory(tempDir.resolve("Documents")));
    }

    // Test listing folders
    @Test
    void testListFolders() throws Exception {
        Files.createDirectory(tempDir.resolve("Documents"));

        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Documents"));
    }

    // Test renaming a folder
    @Test
    void testRenameFolder() throws Exception {
        Files.createDirectory(tempDir.resolve("Documents"));

        mockMvc.perform(put("/api/folders/Documents")
                .param("newName", "Pictures"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Folder renamed successfully"));

        assertTrue(Files.isDirectory(tempDir.resolve("Pictures")));
        assertFalse(Files.exists(tempDir.resolve("Documents")));
    }

    // Test deleting a folder
    @Test
    void testDeleteFolder() throws Exception {
        Files.createDirectory(tempDir.resolve("Documents"));

        mockMvc.perform(delete("/api/folders/Documents"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Folder deleted successfully"));

        assertFalse(Files.exists(tempDir.resolve("Documents")));
    }

    // Test duplicate folder creation
    @Test
    void testDuplicateFolder() throws Exception {
        Files.createDirectory(tempDir.resolve("Documents"));

        mockMvc.perform(post("/api/folders")
                .param("name", "Documents"))
                .andExpect(status().isConflict());
    }

    // Test invalid folder name
    @Test
    void testInvalidFolderName() throws Exception {
        mockMvc.perform(post("/api/folders")
                .param("name", "../outside"))
                .andExpect(status().isBadRequest());
    }

    // Test renaming a missing folder
    @Test
    void testRenameMissingFolder() throws Exception {
        mockMvc.perform(put("/api/folders/Missing")
                .param("newName", "NewFolder"))
                .andExpect(status().isNotFound());
    }

    // Test deleting a missing folder
    @Test
    void testDeleteMissingFolder() throws Exception {
        mockMvc.perform(delete("/api/folders/Missing"))
                .andExpect(status().isNotFound());
    }

    // Test renaming to an existing folder
    @Test
    void testRenameDuplicateFolder() throws Exception {
        Files.createDirectory(tempDir.resolve("Documents"));
        Files.createDirectory(tempDir.resolve("Pictures"));

        mockMvc.perform(put("/api/folders/Documents")
                .param("newName", "Pictures"))
                .andExpect(status().isConflict());
    }

    // Test deleting a nonempty folder
    @Test
    void testDeleteNonEmptyFolder() throws Exception {
        Path folder = Files.createDirectory(
                tempDir.resolve("Documents"));

        Files.writeString(folder.resolve("test.txt"), "Hello");

        mockMvc.perform(delete("/api/folders/Documents"))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Folder must be empty before deletion"));
    }
}
