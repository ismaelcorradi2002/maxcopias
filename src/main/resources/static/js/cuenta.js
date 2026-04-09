const NAME_REGEX = /^[\p{L}]+(?:[ -][\p{L}]+)*$/u;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

document.addEventListener("DOMContentLoaded", function () {
    const registerForm = document.querySelector('[data-live-validation="register"]');
    const loginForm = document.querySelector('[data-live-validation="login"]');

    if (registerForm) {
        initRegisterValidation(registerForm);
    }

    if (loginForm) {
        initLoginValidation(loginForm);
    }
});

function initRegisterValidation(form) {
    const fields = {
        firstName: form.querySelector('[data-live-validate="firstName"]'),
        lastName: form.querySelector('[data-live-validate="lastName"]'),
        email: form.querySelector('[data-live-validate="email"]'),
        phone: form.querySelector('[data-live-validate="phone"]'),
        password: form.querySelector('[data-live-validate="password"]'),
        confirmPassword: form.querySelector('[data-live-validate="confirmPassword"]')
    };

    const submitButton = form.querySelector('button[type="submit"]');
    const emailCheckUrl = form.dataset.emailCheckUrl;
    const strengthNodes = {
        wrapper: form.querySelector("[data-password-strength]"),
        bar: form.querySelector("[data-password-strength-bar]"),
        text: form.querySelector("[data-password-strength-text]")
    };

    let emailRequestId = 0;
    let emailCheckTimer = null;
    let lastCheckedEmail = "";
    let lastCheckedResult = null;

    const validators = {
        firstName: function () {
            return validateNameField(fields.firstName, "Introduce tu nombre.", 80, "El nombre solo puede contener letras, espacios y guiones.");
        },
        lastName: function () {
            return validateNameField(fields.lastName, "Introduce tus apellidos.", 120, "Los apellidos solo pueden contener letras, espacios y guiones.");
        },
        email: function (options) {
            return validateEmailField(fields.email, emailCheckUrl, options);
        },
        phone: function () {
            const value = fields.phone.value.trim();

            if (!value) {
                return setFieldState(fields.phone, "Introduce tu telefono.", "error");
            }

            if (!/^\d{9}$/.test(value)) {
                return setFieldState(fields.phone, "El telefono debe tener exactamente 9 digitos.", "error");
            }

            return setFieldState(fields.phone, "Telefono valido.", "success");
        },
        password: function () {
            const value = fields.password.value;
            const strength = evaluatePasswordStrength(value);

            updatePasswordStrength(strengthNodes, strength);

            if (!value) {
                return setFieldState(fields.password, "Introduce una contrasena.", "error");
            }

            if (value.length > 72) {
                return setFieldState(fields.password, "La contrasena no puede superar 72 caracteres.", "error");
            }

            if (!strength.rules.minLength) {
                return setFieldState(fields.password, "La contrasena debe tener al menos 6 caracteres.", "error");
            }

            if (!strength.rules.uppercase) {
                return setFieldState(fields.password, "Incluye al menos una letra mayuscula.", "error");
            }

            if (!strength.rules.numberOrSymbol) {
                return setFieldState(fields.password, "Incluye al menos un numero o simbolo.", "error");
            }

            return setFieldState(fields.password, "Contrasena segura.", "success");
        },
        confirmPassword: function () {
            const passwordValue = fields.password.value;
            const confirmValue = fields.confirmPassword.value;

            if (!confirmValue) {
                return setFieldState(fields.confirmPassword, "Confirma tu contrasena.", "error");
            }

            if (confirmValue !== passwordValue) {
                return setFieldState(fields.confirmPassword, "Las contrasenas no coinciden.", "error");
            }

            return setFieldState(fields.confirmPassword, "Las contrasenas coinciden.", "success");
        }
    };

    fields.firstName.addEventListener("input", function () {
        fields.firstName.value = sanitizeNameValue(fields.firstName.value);
    });

    fields.lastName.addEventListener("input", function () {
        fields.lastName.value = sanitizeNameValue(fields.lastName.value);
    });

    fields.phone.addEventListener("input", function () {
        fields.phone.value = sanitizePhoneValue(fields.phone.value);
    });

    Object.keys(fields).forEach(function (key) {
        const field = fields[key];

        field.addEventListener("input", function () {
            validators[key]({ immediate: false });

            if (key === "password") {
                validators.confirmPassword();
            }

            updateSubmitState();
        });

        field.addEventListener("blur", function () {
            validators[key]({ immediate: true });

            if (key === "password") {
                validators.confirmPassword();
            }

            updateSubmitState();
        });
    });

    form.addEventListener("submit", function (event) {
        let hasPendingValidation = false;
        let hasErrors = false;

        Object.keys(validators).forEach(function (key) {
            const result = validators[key]({ immediate: true });

            if (result === null) {
                hasPendingValidation = true;
                return;
            }

            if (!result.valid) {
                hasErrors = true;
            }
        });

        if (hasPendingValidation || hasErrors) {
            event.preventDefault();
            updateSubmitState();
        }
    });

    updatePasswordStrength(strengthNodes, evaluatePasswordStrength(fields.password.value));

    if (Object.values(fields).some(function (field) { return field.value.trim() !== ""; })) {
        Object.keys(validators).forEach(function (key) {
            validators[key]({ immediate: true });
        });
    }

    updateSubmitState();

    function validateEmailField(field, url, options) {
        const value = field.value.trim();
        const normalizedValue = value.toLowerCase();
        const immediate = Boolean(options && options.immediate);
        const currentState = field.dataset.validationState || "";

        window.clearTimeout(emailCheckTimer);

        if (!value) {
            emailRequestId++;
            lastCheckedEmail = "";
            lastCheckedResult = null;
            return setFieldState(field, "Introduce un email.", "error");
        }

        if (!isValidEmail(value)) {
            emailRequestId++;
            lastCheckedEmail = "";
            lastCheckedResult = null;
            return setFieldState(field, "Introduce un email valido.", "error");
        }

        if (!url) {
            return setFieldState(field, "Email valido.", "success");
        }

        if (lastCheckedEmail === normalizedValue && lastCheckedResult !== null && (currentState === "success" || currentState === "error")) {
            return { valid: lastCheckedResult };
        }

        setFieldState(field, "Comprobando si el email esta disponible...", "pending");
        const requestId = ++emailRequestId;

        const runCheck = function () {
            checkEmailAvailability(url, value, requestId)
                .then(function (result) {
                    if (result.requestId !== emailRequestId) {
                        return;
                    }

                    lastCheckedEmail = normalizedValue;
                    lastCheckedResult = result.available;
                    setFieldState(field, result.message, result.available ? "success" : "error");
                    updateSubmitState();
                })
                .catch(function () {
                    if (requestId !== emailRequestId) {
                        return;
                    }

                    lastCheckedEmail = "";
                    lastCheckedResult = null;
                    setFieldState(field, "No se ha podido comprobar el email ahora mismo.", "error");
                    updateSubmitState();
                });
        };

        if (immediate) {
            runCheck();
        } else {
            emailCheckTimer = window.setTimeout(runCheck, 320);
        }

        return null;
    }

    function updateSubmitState() {
        const states = Object.values(fields).map(function (field) {
            return field.dataset.validationState || "";
        });
        const hasPendingValidation = states.includes("pending");
        const hasErrors = states.includes("error");
        const hasEmptyRequiredFields = Object.values(fields).some(function (field) {
            return field.value.trim() === "";
        });

        if (submitButton) {
            submitButton.disabled = hasPendingValidation || hasErrors || hasEmptyRequiredFields;
            submitButton.classList.toggle("is-disabled", submitButton.disabled);
        }
    }
}

function initLoginValidation(form) {
    const emailField = form.querySelector('[data-live-validate="loginEmail"]');
    const passwordField = form.querySelector('[data-live-validate="loginPassword"]');
    const submitButton = form.querySelector('button[type="submit"]');

    function validateEmail() {
        const value = emailField.value.trim();

        if (!value) {
            return setFieldState(emailField, "Introduce tu email.", "error");
        }

        if (!isValidEmail(value)) {
            return setFieldState(emailField, "Introduce un email valido.", "error");
        }

        return setFieldState(emailField, "Email correcto.", "success");
    }

    function validatePassword() {
        const value = passwordField.value;

        if (!value) {
            return setFieldState(passwordField, "Introduce tu contrasena.", "error");
        }

        return setFieldState(passwordField, "Contrasena introducida.", "success");
    }

    [emailField, passwordField].forEach(function (field) {
        field.addEventListener("input", updateSubmitState);
        field.addEventListener("blur", updateSubmitState);
    });

    form.addEventListener("submit", function (event) {
        const emailValid = validateEmail().valid;
        const passwordValid = validatePassword().valid;

        if (!emailValid || !passwordValid) {
            event.preventDefault();
            updateSubmitState();
        }
    });

    if (emailField.value || passwordField.value) {
        updateSubmitState();
    }

    function updateSubmitState() {
        const emailValid = validateEmail().valid;
        const passwordValid = validatePassword().valid;

        if (submitButton) {
            submitButton.disabled = !emailValid || !passwordValid;
            submitButton.classList.toggle("is-disabled", submitButton.disabled);
        }
    }
}

function validateNameField(field, emptyMessage, maxLength, invalidMessage) {
    const value = field.value.trim();

    if (!value) {
        return setFieldState(field, emptyMessage, "error");
    }

    if (value.length > maxLength) {
        return setFieldState(field, "No puede superar " + maxLength + " caracteres.", "error");
    }

    if (!NAME_REGEX.test(value)) {
        return setFieldState(field, invalidMessage, "error");
    }

    return setFieldState(field, "Correcto.", "success");
}

function evaluatePasswordStrength(value) {
    const trimmedValue = value || "";
    const rules = {
        minLength: trimmedValue.length >= 6,
        uppercase: /[A-Z]/.test(trimmedValue),
        numberOrSymbol: /(?:\d|[^A-Za-z\d])/.test(trimmedValue)
    };

    const score = Object.values(rules).filter(Boolean).length;

    if (!trimmedValue) {
        return {
            score: 0,
            width: 0,
            tone: "weak",
            label: "Debil",
            rules: rules
        };
    }

    if (score <= 1) {
        return {
            score: score,
            width: score === 0 ? 0 : 40,
            tone: "weak",
            label: "Debil",
            rules: rules
        };
    }

    if (score === 2) {
        return {
            score: score,
            width: 72,
            tone: "medium",
            label: "Media",
            rules: rules
        };
    }

    return {
        score: score,
        width: 100,
        tone: "strong",
        label: "Segura",
        rules: rules
    };
}

function updatePasswordStrength(nodes, strength) {
    if (!nodes.wrapper || !nodes.bar || !nodes.text) {
        return;
    }

    nodes.wrapper.dataset.strength = strength.tone;
    nodes.bar.style.width = strength.width + "%";
    nodes.text.textContent = strength.label;
}

function sanitizeNameValue(value) {
    return value
        .replace(/[^\p{L}\s-]/gu, "")
        .replace(/\s{2,}/g, " ")
        .replace(/-{2,}/g, "-")
        .replace(/^\s+/g, "");
}

function sanitizePhoneValue(value) {
    return value.replace(/\D/g, "").slice(0, 9);
}

function setFieldState(field, message, state) {
    const feedback = getFeedbackNode(field);
    feedback.textContent = message || "";
    feedback.classList.remove("is-error", "is-success", "is-pending");
    field.classList.remove("is-invalid", "is-valid");
    field.dataset.validationState = state || "";
    field.setAttribute("aria-invalid", state === "error" ? "true" : "false");
    hideServerError(field);

    if (state === "error") {
        feedback.classList.add("is-error");
        field.classList.add("is-invalid");
        return { valid: false };
    }

    if (state === "success") {
        feedback.classList.add("is-success");
        field.classList.add("is-valid");
        return { valid: true };
    }

    if (state === "pending") {
        feedback.classList.add("is-pending");
    }

    return null;
}

function hideServerError(field) {
    const key = field.dataset.liveValidate;
    const serverError = document.querySelector('[data-server-error="' + key + '"]');

    if (serverError) {
        serverError.hidden = true;
    }
}

function getFeedbackNode(field) {
    const key = field.dataset.liveValidate;
    return document.querySelector('[data-live-feedback="' + key + '"]');
}

function isValidEmail(value) {
    return EMAIL_REGEX.test(value);
}

function checkEmailAvailability(url, email, requestId) {
    return fetch(url + "?email=" + encodeURIComponent(email), {
        headers: {
            "X-Requested-With": "XMLHttpRequest"
        }
    }).then(function (response) {
        if (!response.ok) {
            throw new Error("Email check failed");
        }

        return response.json();
    }).then(function (data) {
        return {
            requestId: requestId,
            available: Boolean(data.available),
            message: data.message || "No se ha podido comprobar el email."
        };
    });
}
