const folderList = document.getElementById("folderList");
const folderFileList = document.getElementById("folderFileList");
const toast = document.getElementById("toast");

const foldersView = document.getElementById("foldersView");
const folderContentsView = document.getElementById("folderContentsView");

const folderUploadForm = document.getElementById("folderUploadForm");
const folderFileInput = document.getElementById("folderFileInput");
const folderUploadZone = document.getElementById("folderUploadZone");
const folderSelectedFile = document.getElementById("folderSelectedFile");

const selectedFolders = new Set();
const selectedFiles = new Set();

let currentFolder = null;
let chosenFolderFile = null;
let renameTarget = null;
let deleteTargets = [];
let deleteType = null;
let busy = false;
let toastTimeout;

// Show notifications
function showToast(message, error = false) {
    toast.textContent = message;
    toast.classList.toggle("error", error);
    toast.classList.add("show");

    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        toast.classList.remove("show");
    }, 3500);
}

// Update selection
function updateSelection(type) {
    const isFolder = type === "folder";
    const list = isFolder ? folderList : folderFileList;
    const selected = isFolder ? selectedFolders : selectedFiles;

    const selectAll = document.getElementById(
        isFolder ? "selectAllFolders" : "selectAllFiles"
    );

    const count = document.getElementById(
        isFolder ? "selectedCount" : "selectedFileCount"
    );

    const deleteButton = document.getElementById(
        isFolder ? "deleteSelectedButton" : "deleteSelectedFiles"
    );

    const boxes = [
        ...list.querySelectorAll("." + type + "-checkbox")
    ];

    const checked = boxes.filter(box => box.checked).length;

    count.textContent = selected.size +
        (isFolder ? " folders selected" : " files selected");

    deleteButton.disabled = selected.size === 0 || busy;
    selectAll.checked = boxes.length > 0 && checked === boxes.length;
    selectAll.indeterminate =
        checked > 0 && checked < boxes.length;
}

// Add a selection checkbox
function addCheckbox(cell, name, type) {
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.className = type + "-checkbox";
    checkbox.dataset.name = name;

    const selected = type === "folder"
        ? selectedFolders : selectedFiles;

    checkbox.checked = selected.has(name);
    checkbox.setAttribute("aria-label", "Select " + name);

    checkbox.addEventListener("change", () => {
        if (checkbox.checked) {
            selected.add(name);
        } else {
            selected.delete(name);
        }

        updateSelection(type);
    });

    cell.append(checkbox);
}

// Display file or clickable folder name
function addName(cell, name, iconText, clickable = false) {
    const wrapper = document.createElement(
        clickable ? "button" : "div"
    );

    wrapper.className = "file-name";

    const icon = document.createElement("span");
    icon.className = "file-icon";
    icon.textContent = iconText;

    const label = document.createElement("span");
    label.textContent = name;

    wrapper.append(icon, label);

    if (clickable) {
        wrapper.type = "button";
        wrapper.classList.add("folder-link");
        wrapper.addEventListener("click", () => openFolder(name));
    }

    cell.append(wrapper);
}

// Load all folders
async function loadFolders() {
    try {
        const response = await fetch("/api/folders");

        if (!response.ok) {
            throw new Error("Unable to load folders.");
        }

        const folders = await response.json();
        folderList.replaceChildren();

        for (const name of selectedFolders) {
            if (!folders.includes(name)) {
                selectedFolders.delete(name);
            }
        }

        if (folders.length === 0) {
            const row = folderList.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 3;
            cell.textContent = "No folders created yet.";
        }

        folders.forEach(name => {
            const row = folderList.insertRow();

            addCheckbox(row.insertCell(), name, "folder");
            addName(row.insertCell(), name, "📁", true);

            const actionCell = row.insertCell();
            const actions = document.createElement("div");
            actions.className = "action-buttons";

            const renameButton = document.createElement("button");
            renameButton.className = "secondary-btn";
            renameButton.textContent = "Rename";
            renameButton.addEventListener("click", () => {
                openRenameModal(name);
            });

            const deleteButton = document.createElement("button");
            deleteButton.className = "secondary-btn";
            deleteButton.textContent = "Delete";
            deleteButton.addEventListener("click", () => {
                openDeleteModal("folder", [name]);
            });

            actions.append(renameButton, deleteButton);
            actionCell.append(actions);
        });

        updateSelection("folder");

    } catch (error) {
        showToast(error.message, true);
    }
}

// Open a folder
async function openFolder(name) {
    if (busy) return;

    currentFolder = name;
    selectedFiles.clear();

    // Reset upload selection
    folderFileInput.value = "";
    updateFolderSelectedFile(null);

    foldersView.hidden = true;
    folderContentsView.hidden = false;

    document.getElementById("pageTitle").textContent = name;
    document.getElementById("pageDescription").textContent =
        "Manage files inside this folder.";

    document.getElementById("currentFolderTitle").textContent =
        "📁 " + name;

    await loadFolderFiles();
}

// Return to all folders
document.getElementById("backToFolders")
    .addEventListener("click", () => {
        if (busy) return;

        currentFolder = null;
        selectedFiles.clear();

        folderFileInput.value = "";
        updateFolderSelectedFile(null);

        folderContentsView.hidden = true;
        foldersView.hidden = false;

        document.getElementById("pageTitle").textContent =
            "My Folders";

        document.getElementById("pageDescription").textContent =
            "Create and organize your storage folders.";

        loadFolders();
    });

// Load files inside the current folder
async function loadFolderFiles() {
    if (!currentFolder) return;

    const folder = currentFolder;

    try {
        const response = await fetch(
            "/api/files?folder=" + encodeURIComponent(folder)
        );

        if (!response.ok) {
            throw new Error("Unable to load folder files.");
        }

        const files = await response.json();

        // Ignore results if the user changed folders
        if (currentFolder !== folder) return;

        folderFileList.replaceChildren();

        for (const name of selectedFiles) {
            if (!files.includes(name)) {
                selectedFiles.delete(name);
            }
        }

        if (files.length === 0) {
            const row = folderFileList.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 3;
            cell.textContent = "This folder is empty.";
        }

        files.forEach(name => {
            const row = folderFileList.insertRow();

            addCheckbox(row.insertCell(), name, "file");
            addName(row.insertCell(), name, "📄");

            const actionCell = row.insertCell();
            const actions = document.createElement("div");
            actions.className = "action-buttons";

            const downloadButton = document.createElement("button");
            downloadButton.className = "secondary-btn";
            downloadButton.textContent = "Download";

            downloadButton.addEventListener("click", () => {
                window.location.href =
                    "/api/files/download/" +
                    encodeURIComponent(name) +
                    "?folder=" + encodeURIComponent(folder);
            });

            const deleteButton = document.createElement("button");
            deleteButton.className = "secondary-btn";
            deleteButton.textContent = "Delete";

            deleteButton.addEventListener("click", () => {
                openDeleteModal("file", [name]);
            });

            actions.append(downloadButton, deleteButton);
            actionCell.append(actions);
        });

        updateSelection("file");

    } catch (error) {
        showToast(error.message, true);
    }
}

// Update selected upload file
function updateFolderSelectedFile(file) {
    chosenFolderFile = file;
    folderSelectedFile.textContent = file ? file.name : "";
}

// Browse for a file
folderFileInput.addEventListener("change", () => {
    updateFolderSelectedFile(folderFileInput.files[0] || null);
});

// Drag and drop
folderUploadZone.addEventListener("dragover", event => {
    event.preventDefault();
    folderUploadZone.classList.add("drag-over");
});

folderUploadZone.addEventListener("dragleave", () => {
    folderUploadZone.classList.remove("drag-over");
});

folderUploadZone.addEventListener("drop", event => {
    event.preventDefault();
    folderUploadZone.classList.remove("drag-over");

    if (event.dataTransfer.files.length > 0) {
        updateFolderSelectedFile(event.dataTransfer.files[0]);
    }
});

// Upload file inside folder
folderUploadForm.addEventListener("submit", async event => {
    event.preventDefault();

    if (busy || !currentFolder) return;

    if (!chosenFolderFile) {
        showToast("Please select a file first.", true);
        return;
    }

    if (chosenFolderFile.size > 50 * 1024 * 1024) {
        showToast("File must be 50 MB or smaller.", true);
        return;
    }

    busy = true;
    const folder = currentFolder;

    try {
        const data = new FormData();
        data.append("file", chosenFolderFile);

        const response = await fetch(
            "/api/files/upload?folder=" +
            encodeURIComponent(folder),
            {
                method: "POST",
                body: data
            }
        );

        if (!response.ok) {
            throw new Error(await response.text());
        }

        folderFileInput.value = "";
        updateFolderSelectedFile(null);

        await loadFolderFiles();
        showToast("File uploaded successfully!");

    } catch (error) {
        showToast(error.message, true);

    } finally {
        busy = false;
        updateSelection("file");
    }
});

// Create a new folder
document.getElementById("createFolderForm")
    .addEventListener("submit", async event => {
        event.preventDefault();
        if (busy) return;

        const input = document.getElementById("folderName");
        const name = input.value.trim();

        if (!name) return;

        busy = true;

        try {
            const response = await fetch(
                "/api/folders?name=" + encodeURIComponent(name),
                { method: "POST" }
            );

            if (!response.ok) {
                throw new Error(await response.text());
            }

            input.value = "";
            await loadFolders();
            showToast("Folder created successfully!");

        } catch (error) {
            showToast(error.message, true);

        } finally {
            busy = false;
            updateSelection("folder");
        }
    });

// Select all folders or files
function setupSelectAll(id, type) {
    document.getElementById(id)
        .addEventListener("change", event => {
            const selected = type === "folder"
                ? selectedFolders : selectedFiles;

            const list = type === "folder"
                ? folderList : folderFileList;

            list.querySelectorAll("." + type + "-checkbox")
                .forEach(box => {
                    box.checked = event.target.checked;

                    if (box.checked) {
                        selected.add(box.dataset.name);
                    } else {
                        selected.delete(box.dataset.name);
                    }
                });

            updateSelection(type);
        });
}

setupSelectAll("selectAllFolders", "folder");
setupSelectAll("selectAllFiles", "file");

// Open rename modal
function openRenameModal(name) {
    if (busy) return;

    renameTarget = name;

    document.getElementById("newFolderName").value = name;
    document.getElementById("renameModal").hidden = false;
    document.getElementById("newFolderName").focus();
}

// Close rename modal
function closeRenameModal() {
    if (busy) return;

    document.getElementById("renameModal").hidden = true;
    renameTarget = null;
}

document.getElementById("cancelRename")
    .addEventListener("click", closeRenameModal);

// Rename a folder
document.getElementById("renameFolderForm")
    .addEventListener("submit", async event => {
        event.preventDefault();

        if (!renameTarget || busy) return;

        const newName = document.getElementById("newFolderName")
            .value.trim();

        if (!newName) return;

        busy = true;
        const oldName = renameTarget;

        try {
            const response = await fetch(
                "/api/folders/" + encodeURIComponent(oldName) +
                "?newName=" + encodeURIComponent(newName),
                { method: "PUT" }
            );

            if (!response.ok) {
                throw new Error(await response.text());
            }

            selectedFolders.delete(oldName);

            document.getElementById("renameModal").hidden = true;
            renameTarget = null;

            await loadFolders();
            showToast("Folder renamed successfully!");

        } catch (error) {
            showToast(error.message, true);

        } finally {
            busy = false;
            updateSelection("folder");
        }
    });

// Open delete confirmation
function openDeleteModal(type, names) {
    if (busy || names.length === 0) return;

    deleteType = type;
    deleteTargets = [...names];

    const label = type === "folder" ? "folders" : "files";

    document.getElementById("deleteMessage").textContent =
        "Delete " + names.length + " selected " + label +
        "? This cannot be undone." +
        (type === "folder"
            ? " Only empty folders can be deleted."
            : "");

    document.getElementById("deleteModal").hidden = false;
    document.getElementById("cancelDelete").focus();
}

// Close delete confirmation
function closeDeleteModal() {
    if (busy) return;

    document.getElementById("deleteModal").hidden = true;
    deleteTargets = [];
    deleteType = null;
}

document.getElementById("cancelDelete")
    .addEventListener("click", closeDeleteModal);

// Delete selected folders
document.getElementById("deleteSelectedButton")
    .addEventListener("click", () => {
        openDeleteModal("folder", [...selectedFolders]);
    });

// Delete selected files inside folder
document.getElementById("deleteSelectedFiles")
    .addEventListener("click", () => {
        openDeleteModal("file", [...selectedFiles]);
    });

// Delete folders or files
document.getElementById("confirmDelete")
    .addEventListener("click", async () => {
        if (busy || deleteTargets.length === 0) return;

        busy = true;

        const type = deleteType;
        const names = [...deleteTargets];
        const selected = type === "folder"
            ? selectedFolders : selectedFiles;

        const folder = currentFolder;
        let deleted = 0;
        const failed = [];

        const confirmButton =
            document.getElementById("confirmDelete");

        confirmButton.disabled = true;
        confirmButton.textContent = "Deleting...";

        try {
            for (const name of names) {
                try {
                    const url = type === "folder"
                        ? "/api/folders/" + encodeURIComponent(name)
                        : "/api/files/" + encodeURIComponent(name) +
                          "?folder=" + encodeURIComponent(folder);

                    const response = await fetch(url, {
                        method: "DELETE"
                    });

                    if (!response.ok) {
                        throw new Error(await response.text());
                    }

                    selected.delete(name);
                    deleted++;

                } catch (error) {
                    failed.push(name);
                }
            }

            document.getElementById("deleteModal").hidden = true;
            deleteTargets = [];
            deleteType = null;

            if (type === "folder") {
                await loadFolders();
            } else {
                await loadFolderFiles();
            }

            showToast(
                deleted + " deleted" +
                (failed.length
                    ? ", " + failed.length + " failed."
                    : " successfully!"),
                failed.length > 0
            );

        } finally {
            busy = false;
            confirmButton.disabled = false;
            confirmButton.textContent = "Delete";
            updateSelection(type);
        }
    });

// Refresh the current view
document.getElementById("refreshButton")
    .addEventListener("click", () => {
        if (currentFolder) {
            loadFolderFiles();
        } else {
            loadFolders();
        }
    });

// Close modals with Escape
document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !busy) {
        if (!document.getElementById("renameModal").hidden) {
            closeRenameModal();
        }

        if (!document.getElementById("deleteModal").hidden) {
            closeDeleteModal();
        }
    }
});

// Load folders when page opens
loadFolders();
