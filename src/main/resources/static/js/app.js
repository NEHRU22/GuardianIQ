
"use strict";

const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const loginTab = document.getElementById("loginTab");
const registerTab = document.getElementById("registerTab");
const messageBox = document.getElementById("message");

function showMessage(message, type = "error") {
    messageBox.textContent = message;
    messageBox.className = `message ${type}`;
}

function clearMessage() {
    messageBox.textContent = "";
    messageBox.className = "message";
}

function showLoginForm() {
    loginForm.hidden = false;
    registerForm.hidden = true;

    loginTab.classList.add("active");
    registerTab.classList.remove("active");

    clearMessage();
}

function showRegisterForm() {
    loginForm.hidden = true;
    registerForm.hidden = false;

    loginTab.classList.remove("active");
    registerTab.classList.add("active");

    clearMessage();
}

loginTab.addEventListener("click", showLoginForm);
registerTab.addEventListener("click", showRegisterForm);

async function apiRequest(url, method, data) {
    const response = await fetch(url, {
        method,
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        body: data ? JSON.stringify(data) : undefined
    });

    const responseText = await response.text();
    let result = {};

    if (responseText) {
        try {
            result = JSON.parse(responseText);
        } catch {
            result = { message: responseText };
        }
    }

    if (!response.ok) {
        const errorMessage =
            result.detail ||
            result.message ||
            result.error ||
            (Array.isArray(result.errors)
                ? result.errors.join(", ")
                : null) ||
            `Request failed with status ${response.status}`;

        throw new Error(errorMessage);
    }

    return result;
}

function setFormBusy(form, busy) {
    const submitButton = form.querySelector('button[type="submit"]');

    if (!submitButton) {
        return;
    }

    submitButton.disabled = busy;

    if (busy) {
        submitButton.dataset.originalText = submitButton.textContent;
        submitButton.textContent = "Please wait...";
    } else {
        submitButton.textContent =
            submitButton.dataset.originalText || submitButton.textContent;
    }
}

// Registration
registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearMessage();
    setFormBusy(registerForm, true);

    const data = {
        fullName: document.getElementById("fullName").value.trim(),
        email: document.getElementById("registerEmail").value.trim(),
        password: document.getElementById("registerPassword").value,
        dateOfBirth: document.getElementById("dateOfBirth").value,
        parentEmail: document.getElementById("parentEmail").value.trim(),
        parentReportsConsent:
            document.getElementById("parentReportsConsent").checked,
        guardianConsentGiven:
            document.getElementById("guardianConsentGiven").checked
    };

    try {
        const result = await apiRequest(
            "/api/auth/register",
            "POST",
            data
        );

        showLoginForm();

        document.getElementById("loginEmail").value = data.email;
        document.getElementById("loginPassword").value = "";

        showMessage(
            result.message || "Registration successful. Please log in.",
            "success"
        );

        registerForm.reset();
    } catch (error) {
        showMessage(error.message || "Registration failed.");
    } finally {
        setFormBusy(registerForm, false);
    }
});

// Login

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearMessage();
    setFormBusy(loginForm, true);

    const data = {
        email: document.getElementById("loginEmail").value.trim(),
        password: document.getElementById("loginPassword").value
    };

    try {
        // Step 1: Authenticate the user.
        await apiRequest(
            "/api/auth/login",
            "POST",
            data
        );

        // Step 2: Verify that the login session is valid.
        await apiRequest(
            "/api/auth/me",
            "GET"
        );

        // Step 3: Redirect only after successful verification.
        window.location.replace("/dashboard.html");

    } catch (error) {
        showMessage(
            error.message || "Login failed. Please check your credentials."
        );
    } finally {
        setFormBusy(loginForm, false);
    }
});


// Set the newest allowable date of birth.
// The backend remains responsible for enforcing the age requirement.
const dateOfBirthInput = document.getElementById("dateOfBirth");

if (dateOfBirthInput) {
    
const dateOfBirthInput = document.getElementById("dateOfBirth");

if (dateOfBirthInput) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // The user must be younger than 20 years old.
    const cutoff = new Date(
        today.getFullYear() - 20,
        today.getMonth(),
        today.getDate()
    );

    // Earliest eligible birth date: one day after the 20th birthday cutoff.
    const earliestEligibleDob = new Date(cutoff);
    earliestEligibleDob.setDate(earliestEligibleDob.getDate() + 1);

    // Date of birth must be in the past.
    const latestEligibleDob = new Date(today);
    latestEligibleDob.setDate(latestEligibleDob.getDate() - 1);

    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");

        return `${year}-${month}-${day}`;
    }

    dateOfBirthInput.min = formatDate(earliestEligibleDob);
    dateOfBirthInput.max = formatDate(latestEligibleDob);
}

}

showLoginForm();
