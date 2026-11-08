package com.nas.webserver.controller;

import java.io.IOException;
import java.util.List;
import java.nio.file.NoSuchFileException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.DirectoryNotEmptyException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nas.webserver.service.FolderService;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    // Create a folder
    @PostMapping
    public ResponseEntity<String> createFolder(@RequestParam String name)
            throws IOException {
        folderService.createFolder(name);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Folder created successfully");
    }

    // List folders
    @GetMapping
    public List<String> listFolders() throws IOException {
        return folderService.listFolders();
    }

    // Rename a folder
    @PutMapping("/{name}")
    public ResponseEntity<String> renameFolder(
            @PathVariable String name,
            @RequestParam String newName) throws IOException {
        folderService.renameFolder(name, newName);
        return ResponseEntity.ok("Folder renamed successfully");
    }

    // Delete an empty folder
    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteFolder(@PathVariable String name)
            throws IOException {
        folderService.deleteFolder(name);
        return ResponseEntity.ok("Folder deleted successfully");
    }

    // Handle invalid folder names
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidFolder(
            IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // Handle missing folders
    @ExceptionHandler(NoSuchFileException.class)
    public ResponseEntity<String> handleMissingFolder(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Folder not found");
    }

    // Handle duplicate folders
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<String> handleDuplicateFolder(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Folder already exists");
    }

    // Handle nonempty folders
    @ExceptionHandler(DirectoryNotEmptyException.class)
    public ResponseEntity<String> handleNonEmptyFolder(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Folder must be empty before deletion");
    }

    // Handle other storage errors
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleStorageError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Folder operation failed");
    }
}
