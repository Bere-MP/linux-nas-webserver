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

import com.nas.webserver.controller.BackupController;
import com.nas.webserver.service.BackupService;

class BackupControllerTest {

    @TempDir
    Path tempDir;

    private Path storagePath;
    private Path backupPath;
    private BackupService backupService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        storagePath = tempDir.resolve("uploads");
        backupPath = tempDir.resolve("backups");

        backupService = new BackupService(
                storagePath.toString(), backupPath.toString());

        mockMvc = MockMvcBuilders.standaloneSetup(
                new BackupController(backupService)).build();
    }

    // Test creating a backup
    @Test
    void testCreateBackup() throws Exception {
        Files.writeString(storagePath.resolve("test.txt"), "Hello");

        mockMvc.perform(post("/api/backups"))
                .andExpect(status().isCreated())
                .andExpect(content().string(
                        org.hamcrest.Matchers.startsWith("backup-")));

        assertEquals(1, backupService.listBackups().size());
    }

    // Test listing backups
    @Test
    void testListBackups() throws Exception {
        String backup = backupService.createBackup();

        mockMvc.perform(get("/api/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(backup));
    }

    // Test restoring a backup
    @Test
    void testRestoreBackup() throws Exception {
        Files.writeString(storagePath.resolve("test.txt"), "Hello");

        String backup = backupService.createBackup();
        Files.delete(storagePath.resolve("test.txt"));

        mockMvc.perform(post("/api/backups/{filename}/restore", backup))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Backup restored successfully"));

        assertEquals("Hello",
                Files.readString(storagePath.resolve("test.txt")));
    }

    // Test deleting a backup
    @Test
    void testDeleteBackup() throws Exception {
        String backup = backupService.createBackup();

        mockMvc.perform(delete("/api/backups/{filename}", backup))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Backup deleted successfully"));

        assertFalse(Files.exists(backupPath.resolve(backup)));
    }

    // Test restoring a missing backup
    @Test
    void testRestoreMissingBackup() throws Exception {
        String missing =
                "backup-00000000-0000-0000-0000-000000000000.zip";

        mockMvc.perform(post("/api/backups/{filename}/restore", missing))
                .andExpect(status().isNotFound());
    }

    // Test deleting a missing backup
    @Test
    void testDeleteMissingBackup() throws Exception {
        String missing =
                "backup-00000000-0000-0000-0000-000000000000.zip";

        mockMvc.perform(delete("/api/backups/{filename}", missing))
                .andExpect(status().isNotFound());
    }

    // Test invalid backup filename
    @Test
    void testInvalidBackupName() throws Exception {
        mockMvc.perform(delete("/api/backups/invalid.zip"))
                .andExpect(status().isBadRequest());
    }

    // Test restoring into nonempty storage
    @Test
    void testRestoreNonEmptyStorage() throws Exception {
        String backup = backupService.createBackup();

        Files.writeString(storagePath.resolve("existing.txt"), "Data");

        mockMvc.perform(post("/api/backups/{filename}/restore", backup))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Storage must be empty before restoring"));
    }
}
