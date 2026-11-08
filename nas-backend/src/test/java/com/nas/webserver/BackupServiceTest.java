package com.nas.webserver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileAlreadyExistsException;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.nas.webserver.service.BackupService;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    private Path storagePath;
    private Path backupPath;
    private BackupService backupService;

    @BeforeEach
    void setUp() throws IOException {
        storagePath = tempDir.resolve("uploads");
        backupPath = tempDir.resolve("backups");

        backupService = new BackupService(
                storagePath.toString(), backupPath.toString());
    }

    // Test creating a backup
    @Test
    void testCreateBackup() throws IOException {
        Files.writeString(storagePath.resolve("test.txt"), "Hello");

        String backup = backupService.createBackup();

        assertTrue(Files.exists(backupPath.resolve(backup)));
        assertTrue(backup.startsWith("backup-"));
        assertTrue(backup.endsWith(".zip"));
    }

    // Test backup contains stored files
    @Test
    void testBackupContents() throws IOException {
        Files.writeString(storagePath.resolve("test.txt"), "Hello");

        String backup = backupService.createBackup();

        try (ZipFile zip = new ZipFile(backupPath.resolve(backup).toFile())) {
            assertNotNull(zip.getEntry("test.txt"));
        }
    }

    // Test backing up folders
    @Test
    void testBackupFolders() throws IOException {
        Path folder = Files.createDirectory(
                storagePath.resolve("Documents"));

        Files.writeString(folder.resolve("notes.txt"), "Notes");

        String backup = backupService.createBackup();

        try (ZipFile zip = new ZipFile(backupPath.resolve(backup).toFile())) {
            assertNotNull(zip.getEntry("Documents/"));
            assertNotNull(zip.getEntry("Documents/notes.txt"));
        }
    }

    // Test listing backups
    @Test
    void testListBackups() throws IOException {
        backupService.createBackup();
        backupService.createBackup();

        assertEquals(2, backupService.listBackups().size());
    }

    // Test restoring files
    @Test
    void testRestoreBackup() throws IOException {
        Files.writeString(storagePath.resolve("test.txt"), "Hello");

        String backup = backupService.createBackup();

        Files.delete(storagePath.resolve("test.txt"));

        backupService.restoreBackup(backup);

        assertEquals("Hello",
                Files.readString(storagePath.resolve("test.txt")));
    }

    // Test restoring nested folders
    @Test
    void testRestoreFolders() throws IOException {
        Path folder = Files.createDirectory(
                storagePath.resolve("Documents"));

        Files.writeString(folder.resolve("notes.txt"), "Notes");

        String backup = backupService.createBackup();

        Files.delete(folder.resolve("notes.txt"));
        Files.delete(folder);

        backupService.restoreBackup(backup);

        assertEquals("Notes",
                Files.readString(storagePath.resolve(
                        "Documents/notes.txt")));
    }

    // Test deleting a backup
    @Test
    void testDeleteBackup() throws IOException {
        String backup = backupService.createBackup();

        backupService.deleteBackup(backup);

        assertFalse(Files.exists(backupPath.resolve(backup)));
    }

    // Test invalid backup filenames
    @Test
    void testInvalidBackupName() {
        assertThrows(IllegalArgumentException.class,
                () -> backupService.restoreBackup("../outside.zip"));
    }

    // Test restoring a missing backup
    @Test
    void testRestoreMissingBackup() {
        assertThrows(NoSuchFileException.class,
                () -> backupService.restoreBackup(
                        "backup-00000000-0000-0000-0000-000000000000.zip"));
    }

    // Test preventing overwrite during restore
    @Test
    void testRestoreNonEmptyStorage() throws IOException {
        String backup = backupService.createBackup();

        Files.writeString(storagePath.resolve("existing.txt"), "Data");

        assertThrows(FileAlreadyExistsException.class,
                () -> backupService.restoreBackup(backup));
    }

    // Test deleting a missing backup
    @Test
    void testDeleteMissingBackup() {
        assertThrows(NoSuchFileException.class,
                () -> backupService.deleteBackup(
                        "backup-00000000-0000-0000-0000-000000000000.zip"));
    }

    // Test backup storage separation
    @Test
    void testOverlappingStoragePaths() {
        assertThrows(IllegalArgumentException.class,
                () -> new BackupService(
                        tempDir.toString(),
                        tempDir.resolve("backups").toString()));
    }
}
