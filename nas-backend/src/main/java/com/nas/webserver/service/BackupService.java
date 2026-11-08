package com.nas.webserver.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BackupService {

    private final Path storagePath;
    private final Path backupPath;

    public BackupService(
            @Value("${nas.storage.path}") String storage,
            @Value("${nas.backup.path}") String backups)
            throws IOException {

        storagePath = Paths.get(storage).toAbsolutePath().normalize();
        backupPath = Paths.get(backups).toAbsolutePath().normalize();

        Files.createDirectories(storagePath);
        Files.createDirectories(backupPath);

        if (backupPath.startsWith(storagePath)
                || storagePath.startsWith(backupPath)) {
            throw new IllegalArgumentException(
                    "Storage and backup directories must be separate");
        }
    }

    // Validate backup filenames
    private Path resolveBackup(String filename) {
        if (filename == null || !filename.matches(
                "backup-[a-f0-9-]{36}\\.zip")) {
            throw new IllegalArgumentException("Invalid backup name");
        }

        return backupPath.resolve(filename);
    }

    // Create a ZIP backup of stored files and folders
    public String createBackup() throws IOException {
        String filename = "backup-" + UUID.randomUUID() + ".zip";
        Path destination = resolveBackup(filename);

        try (OutputStream output = Files.newOutputStream(
                destination, StandardOpenOption.CREATE_NEW);
             ZipOutputStream zip = new ZipOutputStream(output);
             Stream<Path> paths = Files.walk(storagePath)) {

            for (Path path : paths.toList()) {
                if (path.equals(storagePath)) {
                    continue;
                }

                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Symbolic links are not supported");
                }

                String entryName = storagePath.relativize(path)
                        .toString().replace('\\', '/');

                if (Files.isDirectory(path)) {
                    zip.putNextEntry(new ZipEntry(entryName + "/"));
                    zip.closeEntry();
                } else if (Files.isRegularFile(path)) {
                    zip.putNextEntry(new ZipEntry(entryName));

                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            Files.deleteIfExists(destination);
            throw e;
        }

        return filename;
    }

    // List available ZIP backups
    public List<String> listBackups() throws IOException {
        try (Stream<Path> files = Files.list(backupPath)) {
            return files
                    .filter(path -> Files.isRegularFile(
                            path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches(
                            "backup-[a-f0-9-]{36}\\.zip"))
                    .sorted()
                    .toList();
        }
    }

    // Restore a backup into an empty storage directory
    public void restoreBackup(String filename) throws IOException {
        Path source = resolveBackup(filename);

        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(filename);
        }

        try (Stream<Path> paths = Files.list(storagePath)) {
            if (paths.findAny().isPresent()) {
                throw new FileAlreadyExistsException(
                        "Storage must be empty before restoring");
            }
        }

        try (InputStream input = Files.newInputStream(source);
             ZipInputStream zip = new ZipInputStream(input)) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                Path destination = storagePath.resolve(
                        entry.getName()).normalize();

                // Prevent ZIP path traversal
                if (!destination.startsWith(storagePath)
                        || destination.equals(storagePath)) {
                    throw new IOException("Unsafe ZIP entry");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());

                    Files.copy(zip, destination);
                }

                zip.closeEntry();
            }
        }
    }

    // Delete an existing backup
    public void deleteBackup(String filename) throws IOException {
        Path backup = resolveBackup(filename);

        if (!Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchFileException(filename);
        }

        Files.delete(backup);
    }
}
