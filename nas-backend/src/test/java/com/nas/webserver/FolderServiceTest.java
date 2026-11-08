package com.nas.webserver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.nas.webserver.service.FolderService;

class FolderServiceTest {

    @TempDir
    Path tempDir;

    private FolderService folderService;

    @BeforeEach
    void setUp() throws IOException {
        folderService = new FolderService(tempDir.toString());
    }

    // Test creating a folder
    @Test
    void testCreateFolder() throws IOException {
        folderService.createFolder("Documents");

        assertTrue(Files.isDirectory(tempDir.resolve("Documents")));
    }

    // Test listing folders
    @Test
    void testListFolders() throws IOException {
        folderService.createFolder("Documents");
        folderService.createFolder("Pictures");

        assertEquals(2, folderService.listFolders().size());
        assertTrue(folderService.listFolders().contains("Documents"));
        assertTrue(folderService.listFolders().contains("Pictures"));
    }

    // Test renaming a folder
    @Test
    void testRenameFolder() throws IOException {
        folderService.createFolder("OldFolder");
        folderService.renameFolder("OldFolder", "NewFolder");

        assertFalse(Files.exists(tempDir.resolve("OldFolder")));
        assertTrue(Files.isDirectory(tempDir.resolve("NewFolder")));
    }

    // Test deleting an empty folder
    @Test
    void testDeleteFolder() throws IOException {
        folderService.createFolder("Documents");
        folderService.deleteFolder("Documents");

        assertFalse(Files.exists(tempDir.resolve("Documents")));
    }

    // Test duplicate folder names
    @Test
    void testDuplicateFolder() throws IOException {
        folderService.createFolder("Documents");

        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> folderService.createFolder("Documents"));
    }

    // Test invalid folder names
    @Test
    void testInvalidFolderName() {
        assertThrows(IllegalArgumentException.class,
                () -> folderService.createFolder("../outside"));
    }

    // Test renaming a missing folder
    @Test
    void testRenameMissingFolder() {
        assertThrows(java.nio.file.NoSuchFileException.class,
                () -> folderService.renameFolder("Missing", "NewFolder"));
    }

    // Test deleting a missing folder
    @Test
    void testDeleteMissingFolder() {
        assertThrows(java.nio.file.NoSuchFileException.class,
                () -> folderService.deleteFolder("Missing"));
    }

    // Test preventing rename to an existing folder
    @Test
    void testRenameDuplicateFolder() throws IOException {
        folderService.createFolder("Documents");
        folderService.createFolder("Pictures");

        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> folderService.renameFolder("Documents", "Pictures"));
    }

    // Test preventing deletion of nonempty folders
    @Test
    void testDeleteNonEmptyFolder() throws IOException {
        folderService.createFolder("Documents");
        Files.writeString(tempDir.resolve("Documents/test.txt"), "Hello");

        assertThrows(java.nio.file.DirectoryNotEmptyException.class,
                () -> folderService.deleteFolder("Documents"));
    }
}
