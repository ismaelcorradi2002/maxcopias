document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-copisteria-wizard]");
    const fileInput = document.querySelector("[data-file-input]");
    const fileList = document.querySelector("[data-file-list]");
    const fileHint = document.querySelector("[data-file-hint]");
    const filesError = document.querySelector("[data-files-error]");
    const priceEstimator = document.querySelector("[data-price-estimator]");

    if (!form || !priceEstimator || !fileInput || !fileList || !fileHint) {
        return;
    }

    const summary = createSummaryController(form, fileInput);
    const pricePreview = createPricePreviewController(form, priceEstimator, fileInput, fileList, fileHint);
    const wizard = createWizardController(form, fileInput, fileHint, filesError);

    wizard.onChange(function () {
        pricePreview.updateEstimate();
        summary.update(pricePreview.getState());
    });

    pricePreview.onChange(function () {
        summary.update(pricePreview.getState());
    });

    pricePreview.initialize();
    summary.update(pricePreview.getState());
    preventDoubleSubmit(form);
});

function preventDoubleSubmit(form) {
    if (!form) {
        return;
    }

    form.addEventListener("submit", function (event) {
        if (form.dataset.submitting === "true") {
            event.preventDefault();
            return;
        }

        form.dataset.submitting = "true";
        const submitButtons = Array.from(form.querySelectorAll("button[type='submit']"));
        submitButtons.forEach(function (button) {
            button.disabled = true;
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.textContent || "";
            }
            button.textContent = "Guardando pedido...";
        });
    });
}

function createWizardController(form, fileInput, fileHint, filesError) {
    const stepElements = Array.from(form.querySelectorAll("[data-step-id]"));
    const progressItems = Array.from(document.querySelectorAll("[data-progress-step]"));
    const nextButtons = Array.from(form.querySelectorAll("[data-step-next]"));
    const prevButtons = Array.from(form.querySelectorAll("[data-step-prev]"));
    const copiesInput = form.querySelector("#copies");
    const deliveryAddress = form.querySelector("#deliveryAddress");
    const deliveryAddressBlock = form.querySelector("[data-delivery-address-block]");
    const deliverySummary = form.querySelector("[data-delivery-summary]");
    const deliveryFields = {
        street: form.querySelector("#deliveryStreet"),
        number: form.querySelector("#deliveryNumber"),
        unit: form.querySelector("#deliveryUnit"),
        postalCode: form.querySelector("#deliveryPostalCode"),
        city: form.querySelector("#deliveryCity"),
        province: form.querySelector("#deliveryProvince"),
        contactPhone: form.querySelector("#deliveryContactPhone"),
        instructions: form.querySelector("#deliveryInstructions")
    };
    const deliveryFieldErrors = {
        street: form.querySelector("[data-delivery-field-error='street']"),
        number: form.querySelector("[data-delivery-field-error='number']"),
        postalCode: form.querySelector("[data-delivery-field-error='postalCode']"),
        city: form.querySelector("[data-delivery-field-error='city']"),
        province: form.querySelector("[data-delivery-field-error='province']"),
        contactPhone: form.querySelector("[data-delivery-field-error='contactPhone']")
    };
    const quantityButtons = Array.from(form.querySelectorAll("[data-quantity-action]"));
    const listeners = [];
    let currentIndex = 0;

    function getSelectedValue(name) {
        const checked = form.querySelector("input[name='" + name + "']:checked");
        return checked ? checked.value : "";
    }

    function getSequence() {
        const jobType = getSelectedValue("jobType");
        if (jobType === "IMPRESION" || jobType === "FOTOCOPIAS") {
            return ["jobType", "colorMode", "paperSize", "copies", "printSide", "paperType", "bindingType", "extras", "delivery", "files", "review"];
        }

        return ["jobType", "extras", "delivery", "files", "review"];
    }

    function getCurrentStepId() {
        const sequence = getSequence();
        return sequence[currentIndex] || "jobType";
    }

    function findStepIndex(stepId) {
        return getSequence().indexOf(stepId);
    }

    function getStepElement(stepId) {
        return stepElements.find(function (element) {
            return element.dataset.stepId === stepId;
        });
    }

    function validateFilesStep() {
        const hasFiles = Boolean(fileInput.files && fileInput.files.length);
        const isValid = hasFiles && !fileHint.classList.contains("is-error");

        if (filesError) {
            filesError.hidden = isValid;
        }

        return isValid;
    }

    function selectedDeliveryMethod() {
        return getSelectedValue("deliveryMethod") || "HOME_DELIVERY";
    }

    function deliveryCost() {
        return selectedDeliveryMethod() === "HOME_DELIVERY" ? "4,95 €" : "0,00 €";
    }

    function updateDeliverySummary() {
        if (deliverySummary) {
            deliverySummary.textContent = "Entrega simulada · coste estimado " + deliveryCost();
        }
    }

    function parseStructuredDeliveryAddress() {
        const value = deliveryAddress ? deliveryAddress.value : "";
        if (!value) {
            return;
        }

        const mappings = [
            ["street", "Calle"],
            ["number", "Número"],
            ["unit", "Piso / puerta / bloque"],
            ["postalCode", "Código postal"],
            ["city", "Ciudad"],
            ["province", "Provincia"],
            ["contactPhone", "Teléfono de contacto"],
            ["instructions", "Indicaciones"]
        ];

        mappings.forEach(function (entry) {
            const match = value.match(new RegExp(entry[1] + ":\\s*(.*)"));
            if (match && deliveryFields[entry[0]] && !deliveryFields[entry[0]].value) {
                deliveryFields[entry[0]].value = match[1].trim();
            }
        });
    }

    function composeDeliveryAddress() {
        if (!deliveryAddress) {
            return "";
        }

        if (selectedDeliveryMethod() !== "HOME_DELIVERY") {
            deliveryAddress.value = "";
            return "";
        }

        const lines = [
            "Calle: " + (deliveryFields.street?.value.trim() || ""),
            "Número: " + (deliveryFields.number?.value.trim() || ""),
            "Piso / puerta / bloque: " + (deliveryFields.unit?.value.trim() || ""),
            "Código postal: " + (deliveryFields.postalCode?.value.trim() || ""),
            "Ciudad: " + (deliveryFields.city?.value.trim() || ""),
            "Provincia: " + (deliveryFields.province?.value.trim() || ""),
            "Teléfono de contacto: " + (deliveryFields.contactPhone?.value.trim() || ""),
            "Indicaciones: " + (deliveryFields.instructions?.value.trim() || "")
        ];

        deliveryAddress.value = lines.join("\n");
        return deliveryAddress.value;
    }

    function setDeliveryFieldState(fieldName, isValid) {
        const field = deliveryFields[fieldName];
        const error = deliveryFieldErrors[fieldName];

        if (field) {
            field.classList.toggle("is-invalid", !isValid);
        }
        if (error) {
            error.hidden = isValid;
        }
    }

    function updateDeliveryVisibility() {
        const showAddress = selectedDeliveryMethod() === "HOME_DELIVERY";
        if (deliveryAddressBlock) {
            deliveryAddressBlock.hidden = !showAddress;
        }
        if (showAddress) {
            if (deliveryFields.city && !deliveryFields.city.value.trim()) {
                deliveryFields.city.value = "";
            }
            if (deliveryFields.province && !deliveryFields.province.value.trim()) {
                deliveryFields.province.value = "Madrid";
            }
        }
        updateDeliverySummary();
        if (!showAddress) {
            Object.keys(deliveryFieldErrors).forEach(function (fieldName) {
                setDeliveryFieldState(fieldName, true);
            });
            if (deliveryAddress) {
                deliveryAddress.value = "";
            }
        }
    }

    function validateDeliveryStep() {
        const requiresAddress = selectedDeliveryMethod() === "HOME_DELIVERY";
        if (!requiresAddress) {
            composeDeliveryAddress();
            return true;
        }

        const street = deliveryFields.street?.value.trim() || "";
        const number = deliveryFields.number?.value.trim() || "";
        const postalCode = deliveryFields.postalCode?.value.trim() || "";
        const city = deliveryFields.city?.value.trim() || "";
        const province = deliveryFields.province?.value.trim() || "";
        const contactPhone = (deliveryFields.contactPhone?.value || "").replace(/\s+/g, "");

        const validity = {
            street: street.length > 0,
            number: number.length > 0,
            postalCode: /^\d{5}$/.test(postalCode),
            city: city.length > 0 && normalizeDeliveryCity(city) === "torrejon de ardoz",
            province: province.length > 0,
            contactPhone: /^\d{9}$/.test(contactPhone)
        };

        Object.keys(validity).forEach(function (fieldName) {
            setDeliveryFieldState(fieldName, validity[fieldName]);
        });

        if (deliveryFields.contactPhone) {
            deliveryFields.contactPhone.value = contactPhone;
        }

        composeDeliveryAddress();
        return Object.values(validity).every(Boolean);
    }

    function normalizeDeliveryCity(value) {
        return String(value || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim();
    }

    function validateStep(stepId) {
        switch (stepId) {
            case "jobType":
                return Boolean(getSelectedValue("jobType"));
            case "colorMode":
                return Boolean(getSelectedValue("colorMode"));
            case "paperSize":
                return Boolean(getSelectedValue("paperSize"));
            case "copies":
                return Boolean(copiesInput && Number.parseInt(copiesInput.value, 10) > 0);
            case "printSide":
                return Boolean(getSelectedValue("printSide"));
            case "paperType":
                return Boolean(getSelectedValue("paperType"));
            case "bindingType":
                return Boolean(getSelectedValue("bindingType"));
            case "delivery":
                return validateDeliveryStep();
            case "files":
                return validateFilesStep();
            case "extras":
            case "review":
                return true;
            default:
                return true;
        }
    }

    function canNavigateTo(stepId) {
        const targetIndex = findStepIndex(stepId);

        if (targetIndex === -1) {
            return false;
        }

        if (targetIndex <= currentIndex) {
            return true;
        }

        const sequence = getSequence();
        for (let index = 0; index < targetIndex; index += 1) {
            if (!validateStep(sequence[index])) {
                return false;
            }
        }

        return true;
    }

    function updateProgress() {
        const sequence = getSequence();

        progressItems.forEach(function (item) {
            const stepId = item.dataset.progressStep;
            const stepIndex = sequence.indexOf(stepId);
            const isVisible = stepIndex !== -1;

            item.hidden = !isVisible;
            item.disabled = !isVisible;

            const stepNumber = item.querySelector("span");
            if (stepNumber && isVisible) {
                stepNumber.textContent = String(stepIndex + 1);
            }

            item.classList.toggle("is-active", isVisible && stepIndex === currentIndex);
            item.classList.toggle("is-completed", isVisible && stepIndex < currentIndex);
            item.classList.toggle("is-clickable", isVisible && canNavigateTo(stepId));
        });
    }

    function updateSteps() {
        const currentStepId = getCurrentStepId();
        const currentSequence = getSequence();

        stepElements.forEach(function (element) {
            const stepId = element.dataset.stepId;
            const isVisibleInFlow = currentSequence.includes(stepId);
            const isCurrent = currentStepId === stepId;

            element.hidden = !isVisibleInFlow;
            element.classList.toggle("is-active", isCurrent && isVisibleInFlow);
            element.classList.toggle("is-complete", isVisibleInFlow && currentSequence.indexOf(stepId) < currentIndex);
        });

        updateDeliveryVisibility();
        updateProgress();
    }

    function focusCurrentStepField(stepId) {
        const stepElement = getStepElement(stepId);
        if (!stepElement) {
            return;
        }

        const candidate = stepElement.querySelector("input:not([type='hidden']), textarea, select, button");
        if (candidate) {
            candidate.focus();
        }
    }

    function notify() {
        listeners.forEach(function (listener) {
            listener();
        });
    }

    function goToStep(stepId) {
        if (!canNavigateTo(stepId)) {
            focusCurrentStepField(getCurrentStepId());
            return;
        }

        const stepIndex = findStepIndex(stepId);
        if (stepIndex === -1) {
            return;
        }

        currentIndex = stepIndex;
        updateSteps();
        focusCurrentStepField(stepId);
        notify();
    }

    function goNext() {
        const currentStepId = getCurrentStepId();

        if (!validateStep(currentStepId)) {
            focusCurrentStepField(currentStepId);
            return;
        }

        const sequence = getSequence();
        if (currentIndex < sequence.length - 1) {
            currentIndex += 1;
            updateSteps();
            focusCurrentStepField(getCurrentStepId());
            notify();
        }
    }

    function goPrev() {
        if (currentIndex > 0) {
            currentIndex -= 1;
            updateSteps();
            focusCurrentStepField(getCurrentStepId());
            notify();
        }
    }

    function normalizeSequencePosition() {
        const sequence = getSequence();

        if (currentIndex > sequence.length - 1) {
            currentIndex = sequence.length - 1;
        }

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        updateSteps();
        notify();
    }

    function attachEvents() {
        nextButtons.forEach(function (button) {
            button.addEventListener("click", goNext);
        });

        prevButtons.forEach(function (button) {
            button.addEventListener("click", goPrev);
        });

        quantityButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                if (!copiesInput) {
                    return;
                }

                const min = Number.parseInt(copiesInput.min || "1", 10);
                const max = Number.parseInt(copiesInput.max || "5000", 10);
                const currentValue = Number.parseInt(copiesInput.value || String(min), 10);
                const safeValue = Number.isFinite(currentValue) ? currentValue : min;
                const delta = this.dataset.quantityAction === "increase" ? 1 : -1;
                const nextValue = Math.min(max, Math.max(min, safeValue + delta));

                copiesInput.value = String(nextValue);
                copiesInput.dispatchEvent(new Event("input", { bubbles: true }));
                copiesInput.dispatchEvent(new Event("change", { bubbles: true }));
            });
        });

        progressItems.forEach(function (item) {
            item.addEventListener("click", function () {
                goToStep(item.dataset.progressStep);
            });
        });

        form.querySelectorAll("input[name='jobType'], input[name='colorMode'], input[name='paperSize'], input[name='printSide'], input[name='paperType'], input[name='bindingType'], input[name='deliveryMethod']")
            .forEach(function (input) {
                input.addEventListener("change", function () {
                    if (input.name === "jobType") {
                        if (filesError) {
                            filesError.hidden = true;
                        }
                        normalizeSequencePosition();
                    } else {
                        updateSteps();
                        notify();
                    }
                });
            });

        form.querySelectorAll("input[name='plastificado'], input[name='urgente'], input[name='escaneado']")
            .forEach(function (input) {
                input.addEventListener("change", function () {
                    updateSteps();
                    notify();
                });
            });

        if (copiesInput) {
            copiesInput.addEventListener("input", function () {
                updateSteps();
                notify();
            });
            copiesInput.addEventListener("change", function () {
                updateSteps();
                notify();
            });
        }

        const observations = form.querySelector("#observations");
        if (observations) {
            observations.addEventListener("input", notify);
            observations.addEventListener("change", notify);
        }

        Object.keys(deliveryFields).forEach(function (fieldName) {
            const field = deliveryFields[fieldName];
            if (!field) {
                return;
            }

            field.addEventListener("input", function () {
                composeDeliveryAddress();
                if (deliveryFieldErrors[fieldName]) {
                    setDeliveryFieldState(fieldName, true);
                }
                updateSteps();
                notify();
            });
            field.addEventListener("change", function () {
                composeDeliveryAddress();
                updateSteps();
                notify();
            });
        });

        fileInput.addEventListener("change", function () {
            if (filesError) {
                filesError.hidden = true;
            }
            updateSteps();
            notify();
        });
    }

    parseStructuredDeliveryAddress();
    composeDeliveryAddress();
    attachEvents();
    normalizeSequencePosition();

    return {
        onChange: function (listener) {
            listeners.push(listener);
        }
    };
}

function createSummaryController(form, fileInput) {
    function queryAll(selector) {
        return Array.from(document.querySelectorAll(selector));
    }

    function setText(nodes, value) {
        nodes.forEach(function (node) {
            if (node) {
                node.textContent = value;
            }
        });
    }

    const nodes = {
        jobType: queryAll("[data-summary-job-type]"),
        colorMode: queryAll("[data-summary-color-mode]"),
        paperSize: queryAll("[data-summary-paper-size]"),
        copies: queryAll("[data-summary-copies]"),
        printSide: queryAll("[data-summary-print-side]"),
        paperType: queryAll("[data-summary-paper-type]"),
        bindingType: queryAll("[data-summary-binding-type]"),
        extras: queryAll("[data-summary-extras]"),
        deliveryMethod: queryAll("[data-summary-delivery-method]"),
        deliveryAddress: queryAll("[data-summary-delivery-address]"),
        files: queryAll("[data-summary-files]"),
        pages: queryAll("[data-summary-pages]"),
        total: queryAll("[data-summary-total]"),
        note: queryAll("[data-summary-note]"),
        priceLines: queryAll("[data-summary-price-lines]")
    };

    function labelForRadio(name) {
        const checked = form.querySelector("input[name='" + name + "']:checked");
        if (!checked) {
            return "No aplica";
        }

        const textNode = checked.closest("label")?.querySelector("strong");
        return textNode ? textNode.textContent.trim() : checked.value;
    }

    function selectedJobType() {
        const checked = form.querySelector("input[name='jobType']:checked");
        return checked ? checked.value : "";
    }

    function getSelectedExtras() {
        const extras = [];

        if (form.querySelector("input[name='plastificado']")?.checked) {
            extras.push("Plastificado");
        }
        if (form.querySelector("input[name='urgente']")?.checked) {
            extras.push("Urgente");
        }
        if (form.querySelector("input[name='escaneado']")?.checked) {
            extras.push("Escaneado");
        }

        return extras;
    }

    function selectedFilesLabel() {
        const files = Array.from(fileInput.files || []);

        if (!files.length) {
            return "Sin archivo";
        }

        if (files.length === 1) {
            return files[0].name;
        }

        return files[0].name + " y " + (files.length - 1) + " archivo(s) mas";
    }

    function deliveryMethodLabel() {
        return getCheckedValue(form, "deliveryMethod") === "HOME_DELIVERY"
            ? "Envío a domicilio"
            : "Envío pendiente de confirmar";
    }

    function deliveryAddressLabel() {
        const street = form.querySelector("#deliveryStreet")?.value.trim() || "";
        const number = form.querySelector("#deliveryNumber")?.value.trim() || "";
        const unit = form.querySelector("#deliveryUnit")?.value.trim() || "";
        const postalCode = form.querySelector("#deliveryPostalCode")?.value.trim() || "";
        const city = form.querySelector("#deliveryCity")?.value.trim() || "";
        const province = form.querySelector("#deliveryProvince")?.value.trim() || "";

        const primary = [street, number].filter(Boolean).join(", ");
        const secondary = [unit, postalCode, city, province].filter(Boolean).join(" · ");
        return [primary, secondary].filter(Boolean).join(" | ");
    }

    function update(state) {
        const jobType = selectedJobType();
        const isPrintFlow = jobType === "IMPRESION" || jobType === "FOTOCOPIAS";
        const copiesValue = form.querySelector("#copies")?.value || "";
        const extras = getSelectedExtras();

        setText(nodes.jobType, labelForRadio("jobType") === "No aplica" ? "Sin seleccionar" : labelForRadio("jobType"));
        setText(nodes.colorMode, isPrintFlow ? labelForRadio("colorMode") : "No aplica");
        setText(nodes.paperSize, isPrintFlow ? labelForRadio("paperSize") : "No aplica");
        setText(nodes.copies, isPrintFlow ? (copiesValue || "Pendiente") : "No aplica");
        setText(nodes.printSide, isPrintFlow ? labelForRadio("printSide") : "No aplica");
        setText(nodes.paperType, isPrintFlow ? labelForRadio("paperType") : "No aplica");
        setText(nodes.bindingType, isPrintFlow ? labelForRadio("bindingType") : "No aplica");
        setText(nodes.extras, extras.length ? extras.join(", ") : "Sin extras");
        setText(nodes.deliveryMethod, deliveryMethodLabel());
        setText(
            nodes.deliveryAddress,
            getCheckedValue(form, "deliveryMethod") === "HOME_DELIVERY"
                ? (deliveryAddressLabel() || "Pendiente")
                : "No aplica"
        );
        setText(nodes.files, selectedFilesLabel());
        setText(nodes.pages, state.pageCountLabel);
        setText(nodes.total, state.formattedTotal);
        setText(nodes.note, state.note);

        nodes.priceLines.forEach(function (node) {
            renderPriceLines(node, state.lines);
        });
    }

    return {
        update: update
    };
}

function createPricePreviewController(form, priceEstimator, fileInput, fileList, fileHint) {
    const previewUrl = priceEstimator.dataset.previewUrl;
    const totalElement = priceEstimator.querySelector("[data-price-total]");
    const pagesElement = priceEstimator.querySelector("[data-price-pages]");
    const breakdownElement = priceEstimator.querySelector("[data-price-breakdown]");
    const noteElement = priceEstimator.querySelector("[data-price-note]");
    const linesElement = priceEstimator.querySelector("[data-price-lines]");
    const estimatedPriceInput = form.querySelector("[data-estimated-price-input]");
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const listeners = [];
    const state = {
        fileCount: 0,
        pageCount: 0,
        requestId: 0,
        lines: [],
        breakdown: "Selecciona un servicio para ver el precio orientativo del pedido.",
        note: "El importe se calcula automáticamente al combinar configuración, archivos y extras.",
        formattedTotal: formatEuro(0)
    };

    fileHint.dataset.defaultText = fileHint.textContent;

    function initialize() {
        bindEvents();
        updateEstimate();
    }

    function bindEvents() {
        fileInput.addEventListener("change", function () {
            const selection = renderSelectedFiles(fileInput, fileList, fileHint);

            if (!selection.valid) {
                state.fileCount = 0;
                state.pageCount = 0;
                updateEstimate(selection.message);
                return;
            }

            state.fileCount = selection.fileCount;
            state.pageCount = selection.fileCount;
            updateEstimate();

            if (selection.fileCount) {
                requestPreview();
            }
        });

        form.querySelectorAll("input, textarea, select").forEach(function (field) {
            if (field === fileInput) {
                return;
            }

            field.addEventListener("change", function () {
                updateEstimate();
            });

            if (field.tagName === "TEXTAREA" || field.type === "number") {
                field.addEventListener("input", function () {
                    updateEstimate();
                });
            }
        });
    }

    function requestPreview() {
        state.requestId += 1;
        const currentRequestId = state.requestId;
        const formData = new FormData();

        appendIfValue(formData, "jobType", getCheckedValue(form, "jobType"));
        appendIfValue(formData, "colorMode", getCheckedValue(form, "colorMode"));
        appendIfValue(formData, "paperSize", getCheckedValue(form, "paperSize"));
        appendIfValue(formData, "copies", form.querySelector("#copies")?.value || "");
        appendIfValue(formData, "printSide", getCheckedValue(form, "printSide"));
        appendIfValue(formData, "paperType", getCheckedValue(form, "paperType"));
        appendIfValue(formData, "bindingType", getCheckedValue(form, "bindingType"));
        appendIfValue(formData, "deliveryMethod", getCheckedValue(form, "deliveryMethod"));
        appendIfValue(formData, "observations", form.querySelector("#observations")?.value || "");

        if (form.querySelector("input[name='plastificado']")?.checked) {
            formData.append("plastificado", "true");
        }
        if (form.querySelector("input[name='urgente']")?.checked) {
            formData.append("urgente", "true");
        }
        if (form.querySelector("input[name='escaneado']")?.checked) {
            formData.append("escaneado", "true");
        }

        Array.from(fileInput.files || []).forEach(function (file) {
            formData.append("archivo", file);
        });

        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(previewUrl, {
            method: "POST",
            headers: headers,
            body: formData
        })
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (data) {
                        throw new Error(data.message || "No se ha podido calcular el precio orientativo.");
                    });
                }

                return response.json();
            })
            .then(function (data) {
                if (currentRequestId !== state.requestId) {
                    return;
                }

                state.fileCount = data.fileCount || 0;
                state.pageCount = data.pageCount || 0;
                fileHint.textContent = buildDetectedFileHint(fileInput.files || [], state.pageCount);
                updateEstimate();
            })
            .catch(function (error) {
                if (currentRequestId !== state.requestId) {
                    return;
                }

                state.pageCount = 0;
                updateEstimate(error.message);
            });
    }

    function updateEstimate(customNote) {
        const estimate = calculateEstimate({
            jobType: getCheckedValue(form, "jobType"),
            colorMode: getCheckedValue(form, "colorMode"),
            paperSize: getCheckedValue(form, "paperSize"),
            copies: form.querySelector("#copies")?.value || "",
            printSide: getCheckedValue(form, "printSide"),
            paperType: getCheckedValue(form, "paperType"),
            bindingType: getCheckedValue(form, "bindingType"),
            deliveryMethod: getCheckedValue(form, "deliveryMethod") || "HOME_DELIVERY",
            plastificado: Boolean(form.querySelector("input[name='plastificado']")?.checked),
            urgente: Boolean(form.querySelector("input[name='urgente']")?.checked),
            escaneado: Boolean(form.querySelector("input[name='escaneado']")?.checked),
            fileCount: state.fileCount,
            pageCount: state.pageCount
        });

        state.lines = estimate.lines;
        state.breakdown = estimate.breakdown;
        state.note = customNote || estimate.note;
        state.formattedTotal = formatEuro(estimate.total);

        totalElement.textContent = state.formattedTotal;
        pagesElement.textContent = pageCountLabel(state.pageCount);
        breakdownElement.textContent = estimate.breakdown;
        noteElement.textContent = state.note;
        renderPriceLines(linesElement, estimate.lines);

        if (estimatedPriceInput) {
            estimatedPriceInput.value = String(estimate.total).replace(".", ",");
        }

        notify();
    }

    function notify() {
        listeners.forEach(function (listener) {
            listener();
        });
    }

    return {
        initialize: initialize,
        onChange: function (listener) {
            listeners.push(listener);
        },
        getState: function () {
            return {
                fileCount: state.fileCount,
                pageCount: state.pageCount,
                pageCountLabel: pageCountLabel(state.pageCount),
                lines: state.lines,
                breakdown: state.breakdown,
                note: state.note,
                formattedTotal: state.formattedTotal
            };
        },
        updateEstimate: updateEstimate
    };
}

function calculateEstimate(input) {
    if (!input.jobType) {
        return {
            total: 0,
            breakdown: "Selecciona un servicio para ver el precio orientativo del pedido.",
            note: "El importe se calcula automáticamente al combinar configuración, archivos y extras.",
            lines: []
        };
    }

    switch (input.jobType) {
        case "IMPRESION":
            return calculatePrintLikeEstimate(input, 0.06, 0.45);
        case "FOTOCOPIAS":
            return calculatePrintLikeEstimate(input, 0.05, 0.18);
        default:
            return calculateQuoteStyleEstimate(input, 12, "encargo especial");
    }
}

function calculatePrintLikeEstimate(input, bwUnitPrice, colorUnitPrice) {
    const copies = normalizeCopies(input.copies);
    const pages = Math.max(input.pageCount, 1);
    const colorMode = input.colorMode || "BLACK_AND_WHITE";
    const paperSize = input.paperSize || "A4";
    const printSide = input.printSide || "ONE_SIDED";
    const paperType = input.paperType || "NORMAL";
    const bindingType = input.bindingType || "SIN_ENCUADERNACION";
    const deliveryMethod = input.deliveryMethod || "HOME_DELIVERY";
    const baseUnit = colorMode === "COLOR" ? colorUnitPrice : bwUnitPrice;
    const basePrint = roundPrice(baseUnit * pages * copies);
    const sizeExtra = roundPrice(basePrint * (sizeMultiplier(paperSize) - 1));
    const withSize = basePrint + sizeExtra;
    const sideExtra = roundPrice(withSize * (sideMultiplier(printSide) - 1));
    const withSides = withSize + sideExtra;
    const paperExtra = roundPrice(withSides * (paperTypeMultiplier(paperType) - 1));
    const deliveryExtra = deliveryMethod === "HOME_DELIVERY" ? 4.95 : 0;
    const plastificadoExtra = input.plastificado ? roundPrice(1.8 * Math.max(input.fileCount, 1)) : 0;
    const urgenteExtra = input.urgente ? (deliveryMethod === "HOME_DELIVERY" ? 4 : 2) : 0;
    const escaneadoExtra = input.escaneado ? roundPrice(0.5 * pages) : 0;
    const bindingExtra = bindingPrice(bindingType);

    const lines = [
        {
            concept: "Impresión " + colorLabel(colorMode),
            detail: pages + " página(s) x " + copies + " copia(s)",
            amount: basePrint
        }
    ];

    addLineIfPositive(lines, "Formato " + paperSize, "Ajuste por tamaño del papel", sizeExtra);
    addLineIfPositive(lines, printSideLabel(printSide), "Configuración de caras del pedido", sideExtra);
    addLineIfPositive(lines, "Papel " + paperTypeLabel(paperType), "Acabado seleccionado", paperExtra);
    addLineIfPositive(lines, "Encuadernación " + bindingLabel(bindingType), "Acabado adicional", bindingExtra);
    addLineIfPositive(lines, "Entrega " + deliveryLabel(deliveryMethod), deliveryDetail(deliveryMethod), deliveryExtra);
    addLineIfPositive(lines, "Plastificado", "Protección del documento", plastificadoExtra);
    addLineIfPositive(lines, "Servicio urgente", urgentDetail(deliveryMethod), urgenteExtra);
    addLineIfPositive(lines, "Escaneado", pages + " página(s) a digitalizar", escaneadoExtra);

    const total = roundPrice(lines.reduce(function (accumulator, line) {
        return accumulator + line.amount;
    }, 0));

    const parts = [
        pageReference(input.pageCount, input.fileCount) + " x " + copies + " copia(s)",
        colorLabel(colorMode),
        paperSize,
        printSideLabel(printSide),
        paperTypeLabel(paperType),
        deliveryLabel(deliveryMethod)
    ];

    if (bindingType !== "SIN_ENCUADERNACION") {
        parts.push(bindingLabel(bindingType));
    }

    appendExtras(parts, input);

    return {
        total: total,
        breakdown: parts.join(" · "),
        note: buildNote(input.fileCount, "impresión", input),
        lines: lines
    };
}

function calculateQuoteStyleEstimate(input, basePrice, label) {
    const additionalFiles = roundPrice((Math.max(input.fileCount, 1) - 1) * 2.5);
    const deliveryMethod = input.deliveryMethod || "HOME_DELIVERY";
    const deliveryExtra = deliveryMethod === "HOME_DELIVERY" ? 4.95 : 0;
    const plastificadoExtra = input.plastificado ? roundPrice(1.8 * Math.max(input.fileCount, 1)) : 0;
    const urgenteExtra = input.urgente ? (deliveryMethod === "HOME_DELIVERY" ? 4 : 2) : 0;
    const escaneadoExtra = input.escaneado ? roundPrice(0.5 * Math.max(input.pageCount, 1)) : 0;

    const lines = [
        {
            concept: "Base de " + label,
            detail: fileReference(input.fileCount),
            amount: basePrice
        }
    ];

    addLineIfPositive(lines, "Archivos adicionales", Math.max(input.fileCount - 1, 0) + " archivo(s)", additionalFiles);
    addLineIfPositive(lines, "Entrega " + deliveryLabel(deliveryMethod), deliveryDetail(deliveryMethod), deliveryExtra);
    addLineIfPositive(lines, "Plastificado", "Protección del documento", plastificadoExtra);
    addLineIfPositive(lines, "Servicio urgente", urgentDetail(deliveryMethod), urgenteExtra);
    addLineIfPositive(lines, "Escaneado", Math.max(input.pageCount, 1) + " página(s) a digitalizar", escaneadoExtra);

    const total = roundPrice(lines.reduce(function (accumulator, line) {
        return accumulator + line.amount;
    }, 0));

    const parts = [
        "Base de " + label,
        fileReference(input.fileCount),
        deliveryLabel(deliveryMethod)
    ];

    appendExtras(parts, input);

    return {
        total: total,
        breakdown: parts.join(" · "),
        note: buildNote(input.fileCount, label, input),
        lines: lines
    };
}

function renderSelectedFiles(fileInput, fileList, fileHint) {
    const files = Array.from(fileInput.files || []);
    fileList.innerHTML = "";

    if (!files.length) {
        fileHint.classList.remove("is-error");
        fileHint.textContent = fileHint.dataset.defaultText || fileHint.textContent;
        return { valid: true, fileCount: 0 };
    }

    const invalidFiles = files.filter(function (file) {
        return !isAllowedFile(file.name);
    });

    if (invalidFiles.length) {
        fileInput.value = "";
        fileHint.classList.add("is-error");
        fileHint.textContent = "Formato no válido. Sube PDF, DOC, DOCX, JPG, PNG o WEBP.";
        return {
            valid: false,
            fileCount: 0,
            message: "Formato no válido. Sube PDF, DOC, DOCX, JPG, PNG o WEBP."
        };
    }

    fileHint.classList.remove("is-error");
    fileHint.textContent = buildDetectedFileHint(files, 0);

    files.forEach(function (file) {
        const item = document.createElement("li");
        const copy = document.createElement("div");
        const name = document.createElement("strong");
        const size = document.createElement("span");

        name.textContent = file.name;
        size.textContent = formatFileSize(file.size);
        copy.appendChild(name);
        copy.appendChild(size);
        item.appendChild(copy);
        fileList.appendChild(item);
    });

    return { valid: true, fileCount: files.length };
}

function renderPriceLines(container, lines) {
    if (!container) {
        return;
    }

    container.innerHTML = "";

    if (!lines || !lines.length) {
        const item = document.createElement("li");
        item.className = "price-line-empty";
        item.textContent = "Completa la configuración y sube el archivo para ver el desglose.";
        container.appendChild(item);
        return;
    }

    lines.forEach(function (line) {
        const item = document.createElement("li");
        const copy = document.createElement("div");
        const concept = document.createElement("strong");
        const detail = document.createElement("small");
        const amount = document.createElement("span");

        concept.textContent = line.concept;
        detail.textContent = line.detail;
        amount.textContent = formatEuro(line.amount);

        copy.appendChild(concept);
        copy.appendChild(detail);
        item.appendChild(copy);
        item.appendChild(amount);
        container.appendChild(item);
    });
}

function getCheckedValue(container, name) {
    const checked = container.querySelector("input[name='" + name + "']:checked");
    return checked ? checked.value : "";
}

function normalizeCopies(value) {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
}

function sizeMultiplier(paperSize) {
    switch (paperSize) {
        case "A5":
            return 0.72;
        case "A3":
            return 1.85;
        default:
            return 1;
    }
}

function sideMultiplier(printSide) {
    return printSide === "DOUBLE_SIDED" ? 1.8 : 1;
}

function paperTypeMultiplier(paperType) {
    switch (paperType) {
        case "SATINADO":
            return 1.35;
        case "CARTULINA":
            return 1.65;
        default:
            return 1;
    }
}

function bindingPrice(bindingType) {
    switch (bindingType) {
        case "ESPIRAL":
            return 3.5;
        case "TAPA_DURA":
            return 7.5;
        case "GRAPADO":
            return 0.6;
        default:
            return 0;
    }
}

function addLineIfPositive(lines, concept, detail, amount) {
    if (!amount || amount <= 0) {
        return;
    }

    lines.push({
        concept: concept,
        detail: detail,
        amount: roundPrice(amount)
    });
}

function appendExtras(parts, input) {
    if (input.plastificado) {
        parts.push("Plastificado");
    }
    if (input.urgente) {
        parts.push("Urgente");
    }
    if (input.escaneado) {
        parts.push("Escaneado");
    }
}

function pageReference(pageCount, fileCount) {
    if (pageCount > 0) {
        return pageCount === 1
            ? "1 página detectada en " + Math.max(fileCount, 1) + " archivo"
            : pageCount + " páginas detectadas en " + Math.max(fileCount, 1) + " archivo(s)";
    }

    return "1 página estimada base";
}

function fileReference(fileCount) {
    return fileCount > 0 ? fileCount + " archivo(s)" : "1 archivo de referencia";
}

function pageCountLabel(pageCount) {
    if (pageCount === 0) {
        return "0 páginas detectadas";
    }

    return pageCount === 1 ? "1 página detectada" : pageCount + " páginas detectadas";
}

function buildNote(fileCount, label, input) {
    const urgentCopy = input && input.urgente
        ? " Con urgente, la entrega estimada es de " + urgentEta(input.deliveryMethod || "HOME_DELIVERY") + "."
        : "";

    if (fileCount > 0) {
        return "Precio orientativo calculado con el servicio, las páginas detectadas y la configuración elegida." + urgentCopy + " El importe final puede ajustarse al revisar acabados especiales.";
    }

    return "Configura el pedido y sube tus archivos para afinar mejor el importe orientativo de " + label + "." + urgentCopy;
}

function colorLabel(colorMode) {
    return colorMode === "COLOR" ? "color" : "blanco y negro";
}

function printSideLabel(printSide) {
    return printSide === "DOUBLE_SIDED" ? "Doble cara" : "Una cara";
}

function paperTypeLabel(paperType) {
    switch (paperType) {
        case "SATINADO":
            return "Satinado";
        case "CARTULINA":
            return "Cartulina";
        default:
            return "Normal";
    }
}

function bindingLabel(bindingType) {
    switch (bindingType) {
        case "ESPIRAL":
            return "Espiral";
        case "TAPA_DURA":
            return "Tapa dura";
        case "GRAPADO":
            return "Grapado";
        default:
        return "Sin encuadernación";
    }
}

function deliveryLabel(deliveryMethod) {
    return deliveryMethod === "HOME_DELIVERY" ? "Envío a domicilio" : "Envío pendiente de confirmar";
}

function deliveryDetail(deliveryMethod) {
    return deliveryMethod === "HOME_DELIVERY" ? "Suplemento de envío al domicilio" : "Sin suplemento de entrega";
}

function urgentDetail(deliveryMethod) {
    return deliveryMethod === "HOME_DELIVERY"
        ? "Preparación prioritaria con entrega estimada en 20 minutos"
        : "Preparación prioritaria para envío a domicilio";
}

function urgentEta(deliveryMethod) {
    return deliveryMethod === "HOME_DELIVERY" ? "20 minutos a domicilio" : "10 minutos en tienda";
}

function roundPrice(amount) {
    return Math.round((amount + Number.EPSILON) * 100) / 100;
}

function formatEuro(amount) {
    return new Intl.NumberFormat("es-ES", {
        style: "currency",
        currency: "EUR"
    }).format(amount || 0);
}

function formatFileSize(sizeInBytes) {
    const sizeInMb = sizeInBytes / (1024 * 1024);
    return sizeInMb < 1
        ? (sizeInBytes / 1024).toFixed(1) + " KB"
        : sizeInMb.toFixed(2) + " MB";
}

function buildDetectedFileHint(files, pageCount) {
    const safeFiles = Array.from(files || []);
    const totalSizeInMb = safeFiles.reduce(function (accumulator, file) {
        return accumulator + file.size;
    }, 0) / (1024 * 1024);

    if (!safeFiles.length) {
        return "Formatos permitidos: PDF, DOC, DOCX, JPG, PNG | Máximo 20 MB por archivo.";
    }

    if (pageCount > 0) {
        return safeFiles.length
            + " archivo(s) seleccionado(s) | "
            + pageCountLabel(pageCount)
            + " | Tamaño total aproximado: "
            + totalSizeInMb.toFixed(2)
            + " MB";
    }

    return safeFiles.length
        + " archivo(s) seleccionado(s) | Tamaño total aproximado: "
        + totalSizeInMb.toFixed(2)
        + " MB";
}

function isAllowedFile(filename) {
    const extension = filename.split(".").pop().toLowerCase();
    return ["pdf", "doc", "docx", "jpg", "jpeg", "png", "webp"].includes(extension);
}

function appendIfValue(formData, name, value) {
    if (value !== null && value !== undefined && String(value).trim() !== "") {
        formData.append(name, value);
    }
}
