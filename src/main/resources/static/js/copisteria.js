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
});

function createWizardController(form, fileInput, fileHint, filesError) {
    const stepElements = Array.from(form.querySelectorAll("[data-step-id]"));
    const progressItems = Array.from(document.querySelectorAll("[data-progress-step]"));
    const nextButtons = Array.from(form.querySelectorAll("[data-step-next]"));
    const prevButtons = Array.from(form.querySelectorAll("[data-step-prev]"));
    const copiesInput = form.querySelector("#copies");
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
            return ["jobType", "colorMode", "paperSize", "copies", "printSide", "paperType", "bindingType", "extras", "files", "review"];
        }

        return ["jobType", "extras", "files", "review"];
    }

    function getCurrentSequence() {
        return getSequence();
    }

    function getCurrentStepId() {
        const sequence = getCurrentSequence();
        return sequence[currentIndex] || "jobType";
    }

    function findStepIndex(stepId) {
        return getCurrentSequence().indexOf(stepId);
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

        const sequence = getCurrentSequence();
        for (let index = 0; index < targetIndex; index += 1) {
            if (!validateStep(sequence[index])) {
                return false;
            }
        }

        return true;
    }

    function updateProgress() {
        const sequence = getCurrentSequence();

        progressItems.forEach(function (item) {
            const stepId = item.dataset.progressStep;
            const stepIndex = sequence.indexOf(stepId);
            const isVisible = stepIndex !== -1;

            item.hidden = !isVisible;
            item.disabled = !isVisible;
            item.classList.toggle("is-active", isVisible && stepIndex === currentIndex);
            item.classList.toggle("is-completed", isVisible && stepIndex < currentIndex);
            item.classList.toggle("is-clickable", isVisible && canNavigateTo(stepId));
        });
    }

    function updateSteps() {
        const currentStepId = getCurrentStepId();
        const currentSequence = getCurrentSequence();

        stepElements.forEach(function (element) {
            const stepId = element.dataset.stepId;
            const isVisibleInFlow = currentSequence.includes(stepId);
            const isCurrent = currentStepId === stepId;

            element.hidden = !isVisibleInFlow;
            element.classList.toggle("is-active", isCurrent && isVisibleInFlow);
            element.classList.toggle("is-complete", isVisibleInFlow && currentSequence.indexOf(stepId) < currentIndex);
        });

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

        const sequence = getCurrentSequence();
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
        const sequence = getCurrentSequence();

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

        form.querySelectorAll("input[name='jobType'], input[name='colorMode'], input[name='paperSize'], input[name='printSide'], input[name='paperType'], input[name='bindingType']")
            .forEach(function (input) {
                input.addEventListener("change", function () {
                    if (input.name === "jobType") {
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

        fileInput.addEventListener("change", function () {
            if (filesError) {
                filesError.hidden = true;
            }
            updateSteps();
            notify();
        });
    }

    attachEvents();
    normalizeSequencePosition();

    return {
        onChange: function (listener) {
            listeners.push(listener);
        }
    };
}

function createSummaryController(form, fileInput) {
    const nodes = {
        jobType: document.querySelector("[data-summary-job-type]"),
        colorMode: document.querySelector("[data-summary-color-mode]"),
        paperSize: document.querySelector("[data-summary-paper-size]"),
        copies: document.querySelector("[data-summary-copies]"),
        printSide: document.querySelector("[data-summary-print-side]"),
        paperType: document.querySelector("[data-summary-paper-type]"),
        bindingType: document.querySelector("[data-summary-binding-type]"),
        extras: document.querySelector("[data-summary-extras]"),
        files: document.querySelector("[data-summary-files]"),
        pages: document.querySelector("[data-summary-pages]"),
        total: document.querySelector("[data-summary-total]"),
        note: document.querySelector("[data-summary-note]"),
        priceLines: document.querySelector("[data-summary-price-lines]")
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

    function update(state) {
        const jobType = selectedJobType();
        const isPrintFlow = jobType === "IMPRESION" || jobType === "FOTOCOPIAS";
        const copiesValue = form.querySelector("#copies")?.value || "";
        const extras = getSelectedExtras();

        nodes.jobType.textContent = labelForRadio("jobType") === "No aplica" ? "Sin seleccionar" : labelForRadio("jobType");
        nodes.colorMode.textContent = isPrintFlow ? labelForRadio("colorMode") : "No aplica";
        nodes.paperSize.textContent = isPrintFlow ? labelForRadio("paperSize") : "No aplica";
        nodes.copies.textContent = isPrintFlow ? (copiesValue || "Pendiente") : "No aplica";
        nodes.printSide.textContent = isPrintFlow ? labelForRadio("printSide") : "No aplica";
        nodes.paperType.textContent = isPrintFlow ? labelForRadio("paperType") : "No aplica";
        nodes.bindingType.textContent = isPrintFlow ? labelForRadio("bindingType") : "No aplica";
        nodes.extras.textContent = extras.length ? extras.join(", ") : "Sin extras";
        nodes.files.textContent = selectedFilesLabel();
        nodes.pages.textContent = state.pageCountLabel;
        nodes.total.textContent = state.formattedTotal;
        nodes.note.textContent = state.note;
        renderPriceLines(nodes.priceLines, state.lines);
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
        note: "El importe se calcula automaticamente al combinar configuracion, archivos y extras.",
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
                updateEstimate(data.note);
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
            note: "El importe se calcula automaticamente al combinar configuracion, archivos y extras.",
            lines: []
        };
    }

    switch (input.jobType) {
        case "IMPRESION":
            return calculatePrintLikeEstimate(input, 0.06, 0.45, "impresion");
        case "FOTOCOPIAS":
            return calculatePrintLikeEstimate(input, 0.05, 0.18, "fotocopias");
        case "PUBLICIDAD_IMPRENTA":
            return calculateCampaignEstimate(input);
        case "DISENO_GRAFICO":
            return calculateQuoteStyleEstimate(input, 25, "diseno grafico");
        case "OTRO":
            return calculateQuoteStyleEstimate(input, 12, "encargo especial");
        default:
            return calculateQuoteStyleEstimate(input, 12, "encargo especial");
    }
}

function calculatePrintLikeEstimate(input, bwUnitPrice, colorUnitPrice, label) {
    const copies = normalizeCopies(input.copies);
    const pages = Math.max(input.pageCount, 1);
    const colorMode = input.colorMode || "BLACK_AND_WHITE";
    const paperSize = input.paperSize || "A4";
    const printSide = input.printSide || "ONE_SIDED";
    const paperType = input.paperType || "NORMAL";
    const bindingType = input.bindingType || "SIN_ENCUADERNACION";
    const baseUnit = colorMode === "COLOR" ? colorUnitPrice : bwUnitPrice;
    const basePrint = roundPrice(baseUnit * pages * copies);
    const sizeExtra = roundPrice(basePrint * (sizeMultiplier(paperSize) - 1));
    const withSize = basePrint + sizeExtra;
    const sideExtra = roundPrice(withSize * (sideMultiplier(printSide) - 1));
    const withSides = withSize + sideExtra;
    const paperExtra = roundPrice(withSides * (paperTypeMultiplier(paperType) - 1));
    const plastificadoExtra = input.plastificado ? roundPrice(1.8 * Math.max(input.fileCount, 1)) : 0;
    const urgenteExtra = input.urgente ? 2 : 0;
    const escaneadoExtra = input.escaneado ? roundPrice(0.5 * pages) : 0;
    const bindingExtra = bindingPrice(bindingType);

    const lines = [
        {
            concept: (input.jobType === "FOTOCOPIAS" ? "Fotocopias " : "Impresion ") + colorLabel(colorMode),
            detail: pages + " pagina(s) x " + copies + " copia(s)",
            amount: basePrint
        }
    ];

    addLineIfPositive(lines, "Formato " + paperSize, "Ajuste por tamano del papel", sizeExtra);
    addLineIfPositive(lines, printSideLabel(printSide), "Configuracion de caras del pedido", sideExtra);
    addLineIfPositive(lines, "Papel " + paperTypeLabel(paperType), "Acabado seleccionado", paperExtra);
    addLineIfPositive(lines, "Encuadernacion " + bindingLabel(bindingType), "Acabado adicional", bindingExtra);
    addLineIfPositive(lines, "Plastificado", "Proteccion del documento", plastificadoExtra);
    addLineIfPositive(lines, "Servicio urgente", "Prioridad de preparacion", urgenteExtra);
    addLineIfPositive(lines, "Escaneado", pages + " pagina(s) a digitalizar", escaneadoExtra);

    const total = roundPrice(lines.reduce(function (accumulator, line) {
        return accumulator + line.amount;
    }, 0));

    const parts = [
        pageReference(input.pageCount, input.fileCount) + " x " + copies + " copia(s)",
        colorLabel(colorMode),
        paperSize,
        printSideLabel(printSide),
        paperTypeLabel(paperType)
    ];

    if (bindingType !== "SIN_ENCUADERNACION") {
        parts.push(bindingLabel(bindingType));
    }

    appendExtras(parts, input);

    return {
        total: total,
        breakdown: parts.join(" • "),
        note: buildNote(input.fileCount, label),
        lines: lines
    };
}

function calculateCampaignEstimate(input) {
    const copies = normalizeCopies(input.copies);
    const pages = Math.max(input.pageCount, 1);
    const colorMode = input.colorMode || "COLOR";
    const paperSize = input.paperSize || "A4";
    const paperType = input.paperType || "NORMAL";
    const production = roundPrice(
        (colorMode === "COLOR" ? 0.22 : 0.12)
        * campaignSizeMultiplier(paperSize)
        * paperTypeMultiplier(paperType)
        * pages
        * copies
    );
    const plastificadoExtra = input.plastificado ? roundPrice(1.8 * Math.max(input.fileCount, 1)) : 0;
    const urgenteExtra = input.urgente ? 2 : 0;
    const escaneadoExtra = input.escaneado ? roundPrice(0.5 * pages) : 0;

    const lines = [
        {
            concept: "Base de publicidad e imprenta",
            detail: "Preparacion del encargo",
            amount: 19
        },
        {
            concept: "Produccion " + colorLabel(colorMode),
            detail: pages + " pagina(s) x " + copies + " unidad(es) • " + paperSize + " • " + paperTypeLabel(paperType),
            amount: production
        }
    ];

    addLineIfPositive(lines, "Plastificado", "Proteccion del documento", plastificadoExtra);
    addLineIfPositive(lines, "Servicio urgente", "Prioridad de preparacion", urgenteExtra);
    addLineIfPositive(lines, "Escaneado", pages + " pagina(s) a digitalizar", escaneadoExtra);

    const total = roundPrice(lines.reduce(function (accumulator, line) {
        return accumulator + line.amount;
    }, 0));

    const parts = [
        "Base de imprenta",
        pageReference(input.pageCount, input.fileCount) + " x " + copies + " unidad(es)",
        colorLabel(colorMode),
        paperSize,
        paperTypeLabel(paperType)
    ];

    appendExtras(parts, input);

    return {
        total: total,
        breakdown: parts.join(" • "),
        note: buildNote(input.fileCount, "publicidad e imprenta"),
        lines: lines
    };
}

function calculateQuoteStyleEstimate(input, basePrice, label) {
    const additionalFiles = roundPrice((Math.max(input.fileCount, 1) - 1) * 2.5);
    const plastificadoExtra = input.plastificado ? roundPrice(1.8 * Math.max(input.fileCount, 1)) : 0;
    const urgenteExtra = input.urgente ? 2 : 0;
    const escaneadoExtra = input.escaneado ? roundPrice(0.5 * Math.max(input.pageCount, 1)) : 0;

    const lines = [
        {
            concept: "Base de " + label,
            detail: fileReference(input.fileCount),
            amount: basePrice
        }
    ];

    addLineIfPositive(lines, "Archivos adicionales", Math.max(input.fileCount - 1, 0) + " archivo(s)", additionalFiles);
    addLineIfPositive(lines, "Plastificado", "Proteccion del documento", plastificadoExtra);
    addLineIfPositive(lines, "Servicio urgente", "Prioridad de preparacion", urgenteExtra);
    addLineIfPositive(lines, "Escaneado", Math.max(input.pageCount, 1) + " pagina(s) a digitalizar", escaneadoExtra);

    const total = roundPrice(lines.reduce(function (accumulator, line) {
        return accumulator + line.amount;
    }, 0));

    const parts = [
        "Base de " + label,
        fileReference(input.fileCount)
    ];

    appendExtras(parts, input);

    return {
        total: total,
        breakdown: parts.join(" • "),
        note: buildNote(input.fileCount, label),
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
        fileHint.textContent = "Formato no valido. Sube PDF, DOC, DOCX, JPG o PNG.";
        return {
            valid: false,
            fileCount: 0,
            message: "Formato no valido. Sube PDF, DOC, DOCX, JPG o PNG."
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
        item.textContent = "Completa la configuracion y sube el archivo para ver el desglose.";
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

function campaignSizeMultiplier(paperSize) {
    switch (paperSize) {
        case "A5":
            return 0.78;
        case "A3":
            return 1.4;
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
            ? "1 pagina detectada en " + Math.max(fileCount, 1) + " archivo"
            : pageCount + " paginas detectadas en " + Math.max(fileCount, 1) + " archivo(s)";
    }

    return "1 pagina estimada base";
}

function fileReference(fileCount) {
    return fileCount > 0 ? fileCount + " archivo(s)" : "1 archivo de referencia";
}

function pageCountLabel(pageCount) {
    if (pageCount === 0) {
        return "0 paginas detectadas";
    }

    return pageCount === 1 ? "1 pagina detectada" : pageCount + " paginas detectadas";
}

function buildNote(fileCount, label) {
    if (fileCount > 0) {
        return "Precio orientativo calculado con el servicio, las paginas detectadas y la configuracion elegida. El importe final puede ajustarse al revisar acabados especiales.";
    }

    return "Configura el pedido y sube tus archivos para afinar mejor el importe orientativo de " + label + ".";
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
            return "Sin encuadernacion";
    }
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
        return "Formatos permitidos: PDF, DOC, DOCX, JPG, PNG | Maximo 20 MB por archivo.";
    }

    if (pageCount > 0) {
        return safeFiles.length
            + " archivo(s) seleccionado(s) | "
            + pageCountLabel(pageCount)
            + " | Tamano total aproximado: "
            + totalSizeInMb.toFixed(2)
            + " MB";
    }

    return safeFiles.length
        + " archivo(s) seleccionado(s) | Tamano total aproximado: "
        + totalSizeInMb.toFixed(2)
        + " MB";
}

function isAllowedFile(filename) {
    const extension = filename.split(".").pop().toLowerCase();
    return ["pdf", "doc", "docx", "jpg", "jpeg", "png"].includes(extension);
}

function appendIfValue(formData, name, value) {
    if (value !== null && value !== undefined && String(value).trim() !== "") {
        formData.append(name, value);
    }
}
