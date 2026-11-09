package com.nas.webserver.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileAlreadyExistsException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nas.webserver.service.FileService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // Upload a file to main storage or a folder
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String folder)
            throws IOException {

        fileService.uploadFile(file, folder);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("File uploaded successfully");
    }

    // List files in main storage or a folder
    @GetMapping
    public List<String> listFiles(
            @RequestParam(required = false) String folder)
            throws IOException {

        return fileService.listFiles(folder);
    }

    // Download a file
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String filename,
            @RequestParam(required = false) String folder)
            throws IOException {

        Path path = fileService.downloadFile(filename, folder);
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(path.getFileName().toString())
                                .build().toString())
                .body(resource);
    }

    // Delete a file
    @DeleteMapping("/{filename}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String filename,
            @RequestParam(required = false) String folder)
            throws IOException {

        fileService.deleteFile(filename, folder);

        return ResponseEntity.ok("File deleted successfully");
    }

    // Handle invalid names
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidFile(
            IllegalArgumentException e) {

        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // Handle missing files or folders
    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<String> handleMissingFile(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("File or folder not found");
    }

    // Handle duplicate uploads
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<String> handleDuplicateFile(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("File already exists");
    }

    // Handle other storage errors
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleStorageError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("File operation failed");
    }
}
