package com.nas.webserver.controller;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nas.webserver.service.BackupService;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    // Create a new backup
    @PostMapping
    public ResponseEntity<String> createBackup() throws IOException {
        String filename = backupService.createBackup();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(filename);
    }

    // List available backups
    @GetMapping
    public List<String> listBackups() throws IOException {
        return backupService.listBackups();
    }

    // Download an existing backup
    @GetMapping("/{filename}/download")
    public ResponseEntity<Resource> downloadBackup(
            @PathVariable String filename) throws IOException {

        Path backup = backupService.downloadBackup(filename);
        Resource resource = new FileSystemResource(backup);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentLength(resource.contentLength())
                .body(resource);
    }

    // Restore an existing backup
    @PostMapping("/{filename}/restore")
    public ResponseEntity<String> restoreBackup(
            @PathVariable String filename) throws IOException {

        backupService.restoreBackup(filename);

        return ResponseEntity.ok("Backup restored successfully");
    }

    // Delete an existing backup
    @DeleteMapping("/{filename}")
    public ResponseEntity<String> deleteBackup(
            @PathVariable String filename) throws IOException {

        backupService.deleteBackup(filename);

        return ResponseEntity.ok("Backup deleted successfully");
    }

    // Handle invalid backup names
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidBackup(
            IllegalArgumentException e) {

        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // Handle missing backups
    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<String> handleMissingBackup(Exception e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Backup not found");
    }

    // Handle restoring into nonempty storage
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<String> handleExistingFiles(Exception e) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Storage must be empty before restoring");
    }

    // Handle other backup errors
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleBackupError(Exception e) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Backup operation failed");
    }
}
