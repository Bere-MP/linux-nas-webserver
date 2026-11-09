const fileList = document.getElementById("fileList");
const uploadForm = document.getElementById("uploadForm");
const fileInput = document.getElementById("fileInput");
const uploadZone = document.getElementById("uploadZone");
const selectedFile = document.getElementById("selectedFile");
const toast = document.getElementById("toast");
const deleteModal = document.getElementById("deleteModal");

let pendingDelete = null;
let chosenFile = null;
let toastTimeout;

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

// Show selected file
function updateSelectedFile(file) {
    chosenFile = file;
    selectedFile.textContent = file ? file.name : "";
}

// Select a file
fileInput.addEventListener("change", () => {
    updateSelectedFile(fileInput.files[0] || null);
});

// Drag and drop
uploadZone.addEventListener("dragover", event => {
    event.preventDefault();
    uploadZone.classList.add("drag-over");
});

uploadZone.addEventListener("dragleave", () => {
    uploadZone.classList.remove("drag-over");
});

uploadZone.addEventListener("drop", event => {
    event.preventDefault();
    uploadZone.classList.remove("drag-over");

    if (event.dataTransfer.files.length) {
        updateSelectedFile(event.dataTransfer.files[0]);
    }
});

// Upload file
uploadForm.addEventListener("submit", async event => {
    event.preventDefault();

    if (!chosenFile) {
        showToast("Please select a file first.", true);
        return;
    }

    if (chosenFile.size > 50 * 1024 * 1024) {
        showToast("File must be 50 MB or smaller.", true);
        return;
    }

    const data = new FormData();
    data.append("file", chosenFile);

    try {
        const response = await fetch("/api/files/upload", {
            method: "POST",
            body: data
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        fileInput.value = "";
        updateSelectedFile(null);

        await loadFiles();
        showToast("File uploaded successfully!");

    } catch (error) {
        showToast(error.message, true);
    }
});

// Load stored files
async function loadFiles() {
    fileList.replaceChildren();

    try {
        const response = await fetch("/api/files");

        if (!response.ok) {
            throw new Error("Unable to load files.");
        }

        const files = await response.json();

        if (files.length === 0) {
            const row = fileList.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 2;
            cell.textContent = "No files uploaded yet.";
            return;
        }

        files.forEach(filename => {
            const row = fileList.insertRow();

            const nameCell = row.insertCell();
            const nameWrapper = document.createElement("div");
            nameWrapper.className = "file-name";

            const icon = document.createElement("span");
            icon.className = "file-icon";
            icon.textContent = "📄";

            const name = document.createElement("span");
            name.textContent = filename;

            nameWrapper.append(icon, name);
            nameCell.append(nameWrapper);

            const actionCell = row.insertCell();
            const actions = document.createElement("div");
            actions.className = "action-buttons";

            const download = document.createElement("button");
            download.className = "secondary-btn";
            download.textContent = "Download";

            download.addEventListener("click", () => {
                window.location.href =
                    "/api/files/download/" +
                    encodeURIComponent(filename);
            });

            const remove = document.createElement("button");
            remove.className = "secondary-btn";
            remove.textContent = "Delete";

            remove.addEventListener("click", () => {
                openDeleteModal(filename);
            });

            actions.append(download, remove);
            actionCell.append(actions);
        });

    } catch (error) {
        showToast(error.message, true);
    }
}

// Open delete confirmation
function openDeleteModal(filename) {
    pendingDelete = filename;

    document.getElementById("deleteMessage").textContent =
        'Are you sure you want to delete "' + filename + '"?';

    deleteModal.hidden = false;
    document.getElementById("cancelDelete").focus();
}

// Close delete confirmation
function closeDeleteModal() {
    deleteModal.hidden = true;
    pendingDelete = null;
}

document.getElementById("cancelDelete")
    .addEventListener("click", closeDeleteModal);

// Confirm file deletion
document.getElementById("confirmDelete")
    .addEventListener("click", async () => {
        if (!pendingDelete) return;

        const filename = pendingDelete;

        try {
            const response = await fetch(
                "/api/files/" + encodeURIComponent(filename),
                { method: "DELETE" }
            );

            if (!response.ok) {
                throw new Error(await response.text());
            }

            closeDeleteModal();
            await loadFiles();
            showToast("File deleted successfully!");

        } catch (error) {
            showToast(error.message, true);
        }
    });

// Close modal with Escape
document.addEventListener("keydown", event => {
    if (event.key === "Escape" && !deleteModal.hidden) {
        closeDeleteModal();
    }
});

// Refresh file list
document.getElementById("refreshButton")
    .addEventListener("click", loadFiles);

loadFiles();
