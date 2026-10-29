document.addEventListener("DOMContentLoaded", () => {
    // ==================== ELEMENTS ====================
    const chatForm = document.getElementById("chatForm");
    const messageInput = document.getElementById("messageInput");
    const sendButton = document.getElementById("sendButton");
    const logoutButton = document.getElementById("logoutButton");
    const newConversationButton =
        document.getElementById("newConversationButton");
    const messagesContainer =
        document.getElementById("messagesContainer");
    const historyContainer =
        document.getElementById("conversationHistory");
    const welcomeScreen =
        document.getElementById("welcomeScreen");

    const pdfQuestionsButton =
        document.getElementById("pdfQuestionsButton");
    const pdfQuestionsModal =
        document.getElementById("pdfQuestionsModal");
    const closePdfModal =
        document.getElementById("closePdfModal");
    const pdfUploadForm =
        document.getElementById("pdfUploadForm");
    const pdfFileInput =
        document.getElementById("pdfFile");
    const uploadPdfButton =
        document.getElementById("uploadPdfButton");
    const pdfUploadStatus =
        document.getElementById("pdfUploadStatus");
    const pdfQuestionInstructions =
        document.getElementById("pdfQuestionInstructions");

    // ==================== APPLICATION STATE ====================
    let activeConversationId = null;
    let activePdfDocumentId = null;
    let activePdfFilename = null;
    let currentUserEmail = null;

    let isSendingMessage = false;
    let isLoadingConversation = false;
    let isLoggingOut = false;
    let pdfCloseTimer = null;

    const MAX_PDF_SIZE_BYTES = 10 * 1024 * 1024;

    // ==================== PDF SELECTION STORAGE ====================
    function pdfStorageKey(email) {
        return "ai-personal-assistant.activePdf." +
            email.trim().toLowerCase();
    }

    function restoreActivePdf() {
        if (!currentUserEmail) {
            return;
        }

        try {
            const savedPdf = localStorage.getItem(
                pdfStorageKey(currentUserEmail)
            );

            if (!savedPdf) {
                activePdfDocumentId = null;
                activePdfFilename = null;
                return;
            }

            const parsed = JSON.parse(savedPdf);

            activePdfDocumentId = parsed.documentId || null;
            activePdfFilename = parsed.filename || null;
        } catch (error) {
            console.warn(
                "Could not restore selected PDF:",
                error
            );

            activePdfDocumentId = null;
            activePdfFilename = null;
        }

        updatePdfInstructions();
        updateChatPlaceholder();
    }

    function saveActivePdf(documentId, filename) {
        activePdfDocumentId = documentId;
        activePdfFilename = filename || "Uploaded PDF";

        if (currentUserEmail) {
            try {
                localStorage.setItem(
                    pdfStorageKey(currentUserEmail),
                    JSON.stringify({
                        documentId: activePdfDocumentId,
                        filename: activePdfFilename
                    })
                );
            } catch (error) {
                console.warn(
                    "Could not save PDF selection:",
                    error
                );
            }
        }

        updatePdfInstructions();
        updateChatPlaceholder();
    }

    function clearActivePdf() {
        activePdfDocumentId = null;
        activePdfFilename = null;

        if (currentUserEmail) {
            try {
                localStorage.removeItem(
                    pdfStorageKey(currentUserEmail)
                );
            } catch (error) {
                console.warn(
                    "Could not clear saved PDF selection:",
                    error
                );
            }
        }

        updatePdfInstructions();
        updateChatPlaceholder();
    }

    function updatePdfInstructions() {
        if (!pdfQuestionInstructions) {
            return;
        }

        if (activePdfDocumentId) {
            pdfQuestionInstructions.textContent =
                `Selected PDF: ${activePdfFilename || "Uploaded PDF"}. ` +
                "Questions sent through AI Chat will use this document.";
        } else {
            pdfQuestionInstructions.textContent =
                "Upload a PDF to select it for question answering in AI Chat.";
        }
    }

    function updateChatPlaceholder() {
        if (!messageInput) {
            return;
        }

        messageInput.placeholder = activePdfDocumentId
            ? `Ask a question about ${activePdfFilename || "your PDF"}...`
            : "Ask me anything...";
    }

    // ==================== API HELPER ====================
    async function apiRequest(url, options = {}) {
        const response = await fetch(url, {
            ...options,
            credentials: "same-origin",
            headers: {
                ...(options.headers || {})
            }
        });

        if (response.status === 401 || response.status === 403) {
            window.location.replace("/");
            throw new Error(
                "Your session has expired. Please log in again."
            );
        }

        const contentType =
            response.headers.get("content-type") || "";

        let data = {};

        if (contentType.includes("json")) {
            try {
                data = await response.json();
            } catch (error) {
                data = {};
            }
        } else {
            const responseText = await response.text();

            data = responseText
                ? { message: responseText }
                : {};
        }

        if (!response.ok) {
            throw new Error(
                data.detail ||
                data.error ||
                data.message ||
                data.title ||
                `Request failed with status ${response.status}.`
            );
        }

        return data;
    }

    // ==================== USER PROFILE ====================
    async function loadUserProfile() {
        try {
            const user = await apiRequest("/api/auth/me");

            const fullName = user.fullName || "User";
            const email = user.email || "";

            currentUserEmail = email;
            restoreActivePdf();

            const initials = fullName
                .trim()
                .split(/\s+/)
                .filter(Boolean)
                .slice(0, 2)
                .map(part => part.charAt(0).toUpperCase())
                .join("");

            document.querySelectorAll(
                "#profileName, #userName, [data-profile-name], .profile-name"
            ).forEach(element => {
                element.textContent = fullName;
            });

            document.querySelectorAll(
                "#profileEmail, #userEmail, [data-profile-email], .profile-email"
            ).forEach(element => {
                element.textContent = email;
            });

            document.querySelectorAll(
                "#profileInitials, #userInitials, [data-profile-initials]"
            ).forEach(element => {
                element.textContent = initials || "U";
            });
        } catch (error) {
            console.error(
                "Profile loading failed:",
                error
            );
        }
    }

    // ==================== CHAT MESSAGE DISPLAY ====================
    function addChatMessage(role, content) {
        if (!messagesContainer) {
            console.error(
                "Missing #messagesContainer in dashboard.html."
            );

            return null;
        }

        const row = document.createElement("div");

        row.classList.add(
            "transcript-message",
            `transcript-message--${role}`
        );

        const bubble = document.createElement("div");

        bubble.className = "transcript-message__bubble";
        bubble.textContent = content || "";

        row.appendChild(bubble);
        messagesContainer.appendChild(row);

        messagesContainer.scrollTop =
            messagesContainer.scrollHeight;

        return bubble;
    }

    function clearMessages() {
        if (messagesContainer) {
            messagesContainer.replaceChildren();
        }
    }

    function hideWelcomeScreen() {
        if (welcomeScreen) {
            welcomeScreen.style.display = "none";
        }
    }

    function showWelcomeScreen() {
        if (welcomeScreen) {
            welcomeScreen.style.display = "";
        }
    }

    // ==================== CONVERSATION HISTORY ====================
    function updateActiveHistoryItem() {
        if (!historyContainer) {
            return;
        }

        historyContainer
            .querySelectorAll("[data-conversation-id]")
            .forEach(button => {
                const buttonId =
                    Number(button.dataset.conversationId);

                const isActive =
                    activeConversationId !== null &&
                    buttonId === Number(activeConversationId);

                button.classList.toggle("is-active", isActive);

                if (isActive) {
                    button.setAttribute("aria-current", "true");
                } else {
                    button.removeAttribute("aria-current");
                }
            });
    }

    function showHistoryMessage(message) {
        if (!historyContainer) {
            return;
        }

        historyContainer.replaceChildren();

        const paragraph = document.createElement("p");

        paragraph.className = "conversation-history-empty";
        paragraph.textContent = message;

        historyContainer.appendChild(paragraph);
    }

    function renderConversationHistory(conversations) {
        if (!historyContainer) {
            return;
        }

        historyContainer.replaceChildren();

        if (!Array.isArray(conversations) ||
                conversations.length === 0) {
            showHistoryMessage("No saved conversations yet.");
            return;
        }

        conversations.forEach(conversation => {
            const button = document.createElement("button");

            button.type = "button";
            button.className = "conversation-history-item";
            button.dataset.conversationId =
                String(conversation.id);
            button.title =
                conversation.title || "Conversation";

            const icon = document.createElement("span");

            icon.className = "conversation-history-icon";
            icon.textContent = "◷";
            icon.setAttribute("aria-hidden", "true");

            const title = document.createElement("span");

            title.className = "conversation-history-title";
            title.textContent =
                conversation.title || "New conversation";

            button.append(icon, title);

            button.addEventListener("click", () => {
                openConversation(conversation.id);
            });

            historyContainer.appendChild(button);
        });

        updateActiveHistoryItem();
    }

    async function loadConversationHistory() {
        if (!historyContainer) {
            return;
        }

        try {
            const conversations = await apiRequest(
                "/api/chat/conversations"
            );

            renderConversationHistory(conversations);
        } catch (error) {
            console.error(
                "Could not load conversation history:",
                error
            );

            showHistoryMessage(
                "Could not load saved chats. Refresh to try again."
            );
        }
    }

    async function openConversation(conversationId) {
        if (isSendingMessage ||
                isLoadingConversation ||
                isLoggingOut) {
            return;
        }

        isLoadingConversation = true;

        if (messageInput) {
            messageInput.disabled = true;
        }

        if (sendButton) {
            sendButton.disabled = true;
        }

        try {
            const messages = await apiRequest(
                `/api/chat/conversations/${encodeURIComponent(conversationId)}/messages`
            );

            if (!Array.isArray(messages)) {
                throw new Error(
                    "The server returned an invalid message history."
                );
            }

            clearMessages();

            activeConversationId = Number(conversationId);

            messages.forEach(message => {
                const role =
                    String(message.role || "").toLowerCase();

                if (role === "user" || role === "assistant") {
                    addChatMessage(role, message.content);
                }
            });

            if (messages.length > 0) {
                hideWelcomeScreen();
            } else {
                showWelcomeScreen();
            }

            updateActiveHistoryItem();
        } catch (error) {
            console.error(
                "Could not open conversation:",
                error
            );

            alert(
                error.message ||
                "Could not load this conversation."
            );
        } finally {
            isLoadingConversation = false;

            if (messageInput) {
                messageInput.disabled = false;
                messageInput.focus();
            }

            if (sendButton) {
                sendButton.disabled = false;
            }
        }
    }

    // ==================== SUGGESTED PROMPTS ====================
    document.querySelectorAll("[data-prompt]").forEach(element => {
        element.addEventListener("click", () => {
            if (!messageInput ||
                    isSendingMessage ||
                    isLoadingConversation ||
                    isLoggingOut) {
                return;
            }

            messageInput.value =
                element.getAttribute("data-prompt") || "";

            messageInput.focus();
        });
    });

    // ==================== NEW CONVERSATION ====================
    if (newConversationButton) {
        newConversationButton.addEventListener("click", event => {
            event.preventDefault();

            if (isSendingMessage ||
                    isLoadingConversation ||
                    isLoggingOut) {
                return;
            }

            // Start a new conversation in the interface.
            // Existing conversation messages remain saved in MySQL.
            activeConversationId = null;

            // Do not carry the previous PDF into the new conversation.
            clearActivePdf();

            clearMessages();
            showWelcomeScreen();
            updateActiveHistoryItem();

            if (messageInput) {
                messageInput.value = "";
                messageInput.placeholder = "Ask me anything...";
                messageInput.focus();
            }

            console.log(
                "[Chat] New conversation started without PDF context."
            );
        });
    }

    // ==================== SEND MESSAGE TO AI ====================
    if (chatForm &&
            messageInput &&
            sendButton &&
            messagesContainer) {

        chatForm.addEventListener("submit", async event => {
            event.preventDefault();

            if (isSendingMessage ||
                    isLoadingConversation ||
                    isLoggingOut) {
                return;
            }

            const message = messageInput.value.trim();

            if (!message) {
                messageInput.focus();
                return;
            }

            isSendingMessage = true;

            sendButton.disabled = true;
            messageInput.disabled = true;
            messageInput.value = "";

            hideWelcomeScreen();

            addChatMessage("user", message);

            const assistantBubble =
                addChatMessage("assistant", "Thinking...");

            try {
                const requestBody = {
                    conversationId: activeConversationId,
                    message: message,
                    documentId: activePdfDocumentId
                };

                const data = await apiRequest(
                    "/api/chat/message",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify(requestBody)
                    }
                );

                activeConversationId = data.conversationId;

                if (assistantBubble) {
                    assistantBubble.textContent =
                        data.assistantMessage ||
                        "The AI returned an empty response.";
                }

                // Refresh history after the response is saved.
                await loadConversationHistory();

                updateActiveHistoryItem();
            } catch (error) {
                console.error(
                    "Chat request failed:",
                    error
                );

                if (assistantBubble) {
                    assistantBubble.textContent =
                        error.message ||
                        "Unable to contact the AI service. Please try again.";
                }
            } finally {
                isSendingMessage = false;

                sendButton.disabled = false;
                messageInput.disabled = false;

                updateChatPlaceholder();
                messageInput.focus();
            }
        });
    } else {
        console.error(
            "Chat setup failed. Check chatForm, messageInput, " +
            "sendButton, and messagesContainer in dashboard.html."
        );
    }

    // ==================== PDF MODAL OPEN/CLOSE ====================
    function openPdfModal() {
        if (!pdfQuestionsModal) {
            return;
        }

        if (pdfCloseTimer !== null) {
            window.clearTimeout(pdfCloseTimer);
            pdfCloseTimer = null;
        }

        pdfQuestionsModal.hidden = false;

        if (pdfUploadStatus) {
            pdfUploadStatus.textContent = "";
        }

        if (pdfFileInput) {
            pdfFileInput.value = "";
        }

        updatePdfInstructions();

        if (closePdfModal) {
            closePdfModal.focus();
        }
    }

    function closePdfQuestionsModal() {
        if (!pdfQuestionsModal) {
            return;
        }

        pdfQuestionsModal.hidden = true;

        if (pdfQuestionsButton) {
            pdfQuestionsButton.focus();
        }
    }

    if (pdfQuestionsButton && pdfQuestionsModal) {
        pdfQuestionsButton.addEventListener("click", event => {
            event.preventDefault();
            openPdfModal();
        });

        if (closePdfModal) {
            closePdfModal.addEventListener("click", event => {
                event.preventDefault();
                closePdfQuestionsModal();
            });
        }

        pdfQuestionsModal.addEventListener("click", event => {
            if (event.target === pdfQuestionsModal) {
                closePdfQuestionsModal();
            }
        });

        document.addEventListener("keydown", event => {
            if (event.key === "Escape" &&
                    !pdfQuestionsModal.hidden) {
                closePdfQuestionsModal();
            }
        });
    }

    // ==================== PDF FILE VALIDATION ====================
    if (pdfFileInput && pdfUploadStatus) {
        pdfFileInput.addEventListener("change", () => {
            const file =
                pdfFileInput.files &&
                pdfFileInput.files[0];

            if (!file) {
                pdfUploadStatus.textContent = "";
                return;
            }

            if (!file.name.toLowerCase().endsWith(".pdf")) {
                pdfUploadStatus.textContent =
                    "Please select a PDF file.";

                pdfFileInput.value = "";
                return;
            }

            if (file.size > MAX_PDF_SIZE_BYTES) {
                pdfUploadStatus.textContent =
                    "The PDF must be 10 MB or smaller.";

                pdfFileInput.value = "";
                return;
            }

            pdfUploadStatus.textContent =
                `Selected file: ${file.name}`;
        });
    }

    // ==================== PDF UPLOAD ====================
    if (pdfUploadForm &&
            pdfFileInput &&
            uploadPdfButton &&
            pdfUploadStatus) {

        pdfUploadForm.addEventListener("submit", async event => {
            // Prevent the browser from refreshing the dashboard.
            event.preventDefault();

            if (uploadPdfButton.disabled) {
                return;
            }

            const file =
                pdfFileInput.files &&
                pdfFileInput.files[0];

            if (!file) {
                pdfUploadStatus.textContent =
                    "Please select a PDF file first.";
                return;
            }

            if (!file.name.toLowerCase().endsWith(".pdf")) {
                pdfUploadStatus.textContent =
                    "Only PDF files are supported.";
                return;
            }

            if (file.size > MAX_PDF_SIZE_BYTES) {
                pdfUploadStatus.textContent =
                    "The PDF must be 10 MB or smaller.";
                return;
            }

            const formData = new FormData();

            formData.append("file", file);

            const originalButtonText =
                uploadPdfButton.textContent;

            uploadPdfButton.disabled = true;
            uploadPdfButton.textContent = "Uploading...";

            pdfUploadStatus.textContent =
                "Uploading and indexing your PDF. Please wait.";

            try {
                const result = await apiRequest(
                    "/api/pdfs/upload",
                    {
                        method: "POST",
                        body: formData
                    }
                );

                if (!result || !result.documentId) {
                    throw new Error(
                        "The server did not return a PDF document ID."
                    );
                }

                // Save the selected PDF so chat can retrieve it.
                saveActivePdf(
                    String(result.documentId),
                    file.name
                );

                const chunkCount = Number(result.chunksStored);

                const chunksMessage = Number.isFinite(chunkCount)
                    ? `${chunkCount} text chunks indexed.`
                    : "Your PDF was indexed.";

                pdfUploadStatus.textContent =
                    `PDF indexed successfully. ${chunksMessage}`;

                console.log("[PDF] Upload succeeded:", {
                    documentId: result.documentId,
                    chunksStored: result.chunksStored
                });

                if (pdfCloseTimer !== null) {
                    window.clearTimeout(pdfCloseTimer);
                }

                // Return to the existing chat, not a new conversation.
                pdfCloseTimer = window.setTimeout(() => {
                    pdfCloseTimer = null;

                    closePdfQuestionsModal();
                    updateChatPlaceholder();

                    if (messageInput) {
                        messageInput.focus();
                    }
                }, 1500);
            } catch (error) {
                console.error(
                    "[PDF] Upload failed:",
                    error
                );

                pdfUploadStatus.textContent =
                    error.message ||
                    "Upload failed. Please try again.";
            } finally {
                uploadPdfButton.disabled = false;

                uploadPdfButton.textContent =
                    originalButtonText || "Upload PDF";
            }
        });
    } else {
        console.warn(
            "PDF upload was not initialized. Check the PDF element IDs in dashboard.html."
        );
    }

    // ==================== LOGOUT ====================
    if (logoutButton) {
        logoutButton.addEventListener("click", async event => {
            event.preventDefault();

            if (isLoggingOut) {
                return;
            }

            isLoggingOut = true;
            logoutButton.disabled = true;

            try {
                await apiRequest(
                    "/api/auth/logout",
                    { method: "POST" }
                );

                window.location.replace("/");
            } catch (error) {
                console.error(
                    "Logout failed:",
                    error
                );

                isLoggingOut = false;
                logoutButton.disabled = false;

                alert("Logout failed. Please try again.");
            }
        });
    }

    // ==================== INITIAL PAGE SETUP ====================
    loadUserProfile();
    loadConversationHistory();
});