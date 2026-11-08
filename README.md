# Linux NAS Web Server

## Project Overview

This project focuses on building a web-based interface for managing a Network Attached Storage (NAS) server using Ubuntu Linux, Java Spring Boot, MySQL, and Apache.

The goal is to allow users to remotely manage files, control user access, monitor server performance, and perform backups through a web interface.

## Main Features

### 1. File Management
- Upload, download, and delete files.
- Create, rename, and delete folders.
- Organize and manage stored files.

### 2. User Management
- Create, modify, and delete user accounts.
- Implement user authentication and login.
- Assign user roles and permissions.

### 3. System Monitoring
- Monitor CPU usage.
- Monitor disk storage usage.
- View system logs.

### 4. Backup and Restore
- Schedule automatic backups.
- Restore files and system configurations.

## Technologies

- **Operating System:** Ubuntu Linux
- **Backend:** Java Spring Boot
- **Frontend:** HTML, CSS, JavaScript
- **Database:** MySQL
- **Web Server:** Apache
- **Version Control:** Git and GitHub

## Project Structure

```text
linux-nas-webserver/
├── nas-backend/
│   ├── src/main/java/com/nas/webserver/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── static/
│   │   │   ├── css/
│   │   │   └── js/
│   │   ├── templates/
│   │   └── application.properties
│   └── pom.xml
└── README.md
```

### Folder Descriptions

- **controller/** — Handles API requests.
- **service/** — Contains application logic.
- **repository/** — Handles database operations.
- **model/** — Stores data models.
- **config/** — Contains security and application settings.
- **static/** — Stores CSS and JavaScript.
- **templates/** — Stores HTML pages.
- **application.properties** — Spring Boot configuration.
- **pom.xml** — Maven dependencies.

## Team Responsibilities

### Ibrahim — Linux Server & System Monitoring
- Set up and configure Ubuntu Linux, Apache, and MySQL.
- Configure firewall, networking, and server security.
- Implement system monitoring features.
- Deploy and test the application on the Linux server.

### Mahnoor — File Management & Backup
- Implement file and folder management functionality.
- Develop file upload, download, and deletion features.
- Implement automatic backup scheduling and restoration.
- Create the file management interface.

### Berenice — Database & User Management
- Design and configure the MySQL database.
- Implement user authentication and authorization.
- Create user management functionality.
- Assign roles and permissions.
- Store user information and file metadata.

## Project Goal

Develop a functional and secure NAS web server that allows users to manage files, monitor server resources, and control access through a user-friendly web interface.
