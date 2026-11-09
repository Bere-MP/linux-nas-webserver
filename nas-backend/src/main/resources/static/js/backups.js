const backupList = document.getElementById("backupList");
const createBackupButton = document.getElementById("createBackupButton");
const restoreModal = document.getElementById("restoreModal");
const deleteModal = document.getElementById("deleteModal");
const toast = document.getElementById("toast");

const selectAllBackups = document.getElementById("selectAllBackups");
const deleteSelectedButton = document.getElementById("deleteSelectedButton");
const selectedCount = document.getElementById("selectedCount");

let selectedBackup = null;
let bulkDelete = false;
let toastTimeout;
let busy = false;

const selectedBackups = new Set();

// Show notifications
function showToast(message, isError = false) {
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");

    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        toast.classList.remove("show");
    }, 3500);
}

// Update selected backup count
function updateSelection() {
    const checkboxes = [
        ...backupList.querySelectorAll(".backup-checkbox")
    ];

    const total = checkboxes.length;
    const checked = checkboxes.filter(box => box.checked).length;

    selectedCount.textContent =
        selectedBackups.size + " backups selected";

    deleteSelectedButton.disabled =
        selectedBackups.size === 0 || busy;

    selectAllBackups.checked = total > 0 && checked === total;
    selectAllBackups.indeterminate =
        checked > 0 && checked < total;
}

// Load available backups
async function loadBackups() {
    try {
        const response = await fetch("/api/backups");

        if (!response.ok) {
            throw new Error("Unable to load backups.");
        }

        const backups = await response.json();

        backupList.replaceChildren();

        // Remove selections that no longer exist
        for (const name of selectedBackups) {
            if (!backups.includes(name)) {
                selectedBackups.delete(name);
            }
        }

        if (backups.length === 0) {
            const row = backupList.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 3;
            cell.textContent = "No backups created yet.";
            updateSelection();
            return;
        }

        backups.forEach(filename => {
            const row = backupList.insertRow();

            // Selection checkbox
            const checkboxCell = row.insertCell();
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.className = "backup-checkbox";
            checkbox.checked = selectedBackups.has(filename);
            checkbox.setAttribute(
                "aria-label", "Select " + filename
            );

            checkbox.addEventListener("change", () => {
                if (checkbox.checked) {
                    selectedBackups.add(filename);
                } else {
                    selectedBackups.delete(filename);
                }

                updateSelection();
            });

            checkboxCell.append(checkbox);

            // Backup name
            const nameCell = row.insertCell();
            const wrapper = document.createElement("div");
            wrapper.className = "file-name";

            const icon = document.createElement("span");
            icon.className = "file-icon";
            icon.textContent = "💾";

            const name = document.createElement("span");
            name.textContent = filename;

            wrapper.append(icon, name);
            nameCell.append(wrapper);

            // Action buttons
            const actionCell = row.insertCell();
            const actions = document.createElement("div");
            actions.className = "action-buttons";
			
			// Download backup ZIP
			const downloadButton = document.createElement("button");
			downloadButton.className = "secondary-btn";
			downloadButton.textContent = "Download";

			downloadButton.addEventListener("click", () => {
			    window.location.href =
			        "/api/backups/" +
			        encodeURIComponent(filename) +
			        "/download";
			});

            const restoreButton = document.createElement("button");
            restoreButton.className = "secondary-btn";
            restoreButton.textContent = "Restore";

            restoreButton.addEventListener("click", () => {
                openRestoreModal(filename);
            });

            const deleteButton = document.createElement("button");
            deleteButton.className = "secondary-btn";
            deleteButton.textContent = "Delete";

            deleteButton.addEventListener("click", () => {
                openDeleteModal(filename);
            });

            actions.append(downloadButton, restoreButton, deleteButton);
            actionCell.append(actions);
        });

        updateSelection();

    } catch (error) {
        showToast(error.message, true);
    }
}

// Select or deselect all backups
selectAllBackups.addEventListener("change", () => {
    const checkboxes =
        backupList.querySelectorAll(".backup-checkbox");

    checkboxes.forEach(checkbox => {
        checkbox.checked = selectAllBackups.checked;

        const filename = checkbox.closest("tr")
            .querySelector(".file-name span:last-child").textContent;

        if (checkbox.checked) {
            selectedBackups.add(filename);
        } else {
            selectedBackups.delete(filename);
        }
    });

    updateSelection();
});

// Create a backup
createBackupButton.addEventListener("click", async () => {
    if (busy) return;

    busy = true;
    createBackupButton.disabled = true;
    createBackupButton.textContent = "Creating...";

    try {
        const response = await fetch("/api/backups", {
            method: "POST"
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const filename = await response.text();

        await loadBackups();
        showToast("Backup created: " + filename);

    } catch (error) {
        showToast(error.message, true);

    } finally {
        busy = false;
        createBackupButton.disabled = false;
        createBackupButton.textContent = "+ Create Backup";
        updateSelection();
    }
});

// Open restore confirmation
function openRestoreModal(filename) {
    if (busy) return;

    selectedBackup = filename;

    document.getElementById("restoreMessage").textContent =
        'Restore "' + filename +
        '"? Your file and folder storage must be empty first.';

    restoreModal.hidden = false;
    document.getElementById("cancelRestore").focus();
}

// Close restore confirmation
function closeRestoreModal() {
    restoreModal.hidden = true;
    selectedBackup = null;
}

document.getElementById("cancelRestore")
    .addEventListener("click", closeRestoreModal);

// Restore a backup
document.getElementById("confirmRestore")
    .addEventListener("click", async () => {
        if (!selectedBackup || busy) return;

        busy = true;
        const filename = selectedBackup;

        try {
            const response = await fetch(
                "/api/backups/" +
                encodeURIComponent(filename) +
                "/restore",
                { method: "POST" }
            );

            if (!response.ok) {
                throw new Error(await response.text());
            }

            closeRestoreModal();
            showToast("Backup restored successfully!");

        } catch (error) {
            showToast(error.message, true);

        } finally {
            busy = false;
            updateSelection();
        }
    });

// Open delete confirmation
function openDeleteModal(filename = null) {
    if (busy) return;

    bulkDelete = filename === null;
    selectedBackup = filename;

    const message = bulkDelete
        ? "Are you sure you want to delete " +
          selectedBackups.size +
          " selected backups? This cannot be undone."
        : 'Are you sure you want to delete "' +
          filename + '"? This cannot be undone.';

    document.getElementById("deleteMessage").textContent = message;

    deleteModal.hidden = false;
    document.getElementById("cancelDelete").focus();
}

// Close delete confirmation
function closeDeleteModal() {
    if (busy) return;

    deleteModal.hidden = true;
    selectedBackup = null;
    bulkDelete = false;
}

document.getElementById("cancelDelete")
    .addEventListener("click", closeDeleteModal);

// Open bulk delete confirmation
deleteSelectedButton.addEventListener("click", () => {
    if (selectedBackups.size > 0) {
        openDeleteModal();
    }
});

// Delete selected backups
document.getElementById("confirmDelete")
    .addEventListener("click", async () => {
        if (busy) return;

        const filenames = bulkDelete
            ? [...selectedBackups]
            : [selectedBackup];

        if (!filenames.length || !filenames[0]) return;

        busy = true;
        const confirmButton =
            document.getElementById("confirmDelete");

        confirmButton.disabled = true;
        confirmButton.textContent = "Deleting...";
        updateSelection();

        let deleted = 0;
        const failed = [];

        try {
            for (const filename of filenames) {
                try {
                    const response = await fetch(
                        "/api/backups/" +
                        encodeURIComponent(filename),
                        { method: "DELETE" }
                    );

                    if (!response.ok) {
                        throw new Error(await response.text());
                    }

                    selectedBackups.delete(filename);
                    deleted++;

                } catch (error) {
                    failed.push(filename + ": " + error.message);
                }
            }

            deleteModal.hidden = true;
            selectedBackup = null;
            bulkDelete = false;

            await loadBackups();

            if (failed.length === 0) {
                showToast(deleted + " backup(s) deleted successfully!");
            } else {
                showToast(
                    deleted + " deleted, " +
                    failed.length + " failed. " +
                    failed.join("; "),
                    true
                );
            }

        } finally {
            busy = false;
            confirmButton.disabled = false;
            confirmButton.textContent = "Delete Backup";
            updateSelection();
        }
    });

// Close popups with Escape
document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !busy) {
        if (!restoreModal.hidden) {
            closeRestoreModal();
        }

        if (!deleteModal.hidden) {
            closeDeleteModal();
        }
    }
});

// Refresh backup list
document.getElementById("refreshButton")
    .addEventListener("click", loadBackups);

loadBackups();
