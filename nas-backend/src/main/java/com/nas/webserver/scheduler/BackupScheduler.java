package com.nas.webserver.scheduler;

import java.io.IOException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nas.webserver.service.BackupService;

@Component
public class BackupScheduler {

    private final BackupService backupService;

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    // Automatically create a backup every 24 hours
    @Scheduled(fixedDelayString = "${nas.backup.interval-ms:86400000}")
    public void createAutomaticBackup() {
        try {
            String filename = backupService.createBackup();
            System.out.println("Automatic backup created: " + filename);

        } catch (IOException e) {
            System.err.println(
                    "Automatic backup failed: " + e.getMessage());
        }
    }
}
