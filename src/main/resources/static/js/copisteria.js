document.addEventListener("DOMContentLoaded", function () {
    const fileInput = document.querySelector("[data-file-input]");
    const fileList = document.querySelector("[data-file-list]");
    const fileHint = document.querySelector("[data-file-hint]");
    const jobTypeSelect = document.querySelector("[data-job-type-select]");
    const helperCard = document.querySelector("[data-job-type-helper]");
    const helperLabel = document.querySelector("[data-job-type-label]");
    const helperDescription = document.querySelector("[data-job-type-description]");
    const uploadTitle = document.querySelector("[data-upload-dropzone-title]");
    const uploadDescription = document.querySelector("[data-upload-dropzone-description]");
    const printConfigSections = document.querySelectorAll("[data-print-config-section], [data-print-config-item]");
    const priceEstimator = document.querySelector("[data-price-estimator]");

    if (jobTypeSelect && helperCard && helperLabel && helperDescription && uploadTitle && uploadDescription) {
        initTipoTrabajoBehavior(jobTypeSelect, helperCard, helperLabel, helperDescription, uploadTitle, uploadDescription, printConfigSections);
    }

    if (priceEstimator && jobTypeSelect && fileInput && fileList && fileHint) {
        initPriceEstimator({
            jobTypeSelect: jobTypeSelect,
            fileInput: fileInput,
            fileList: fileList,
            fileHint: fileHint,
            priceEstimator: priceEstimator
        });
    }
});

function initTipoTrabajoBehavior(select, helperCard, helperLabel, helperDescription, uploadTitle, uploadDescription, printConfigSections) {
    const defaultLabel = helperLabel.textContent;
    const defaultDescription = helperDescription.textContent;
    const defaultUploadTitle = uploadTitle.textContent;
    const defaultUploadDescription = uploadDescription.textContent;

    updateTipoTrabajoState();
    select.addEventListener("change", updateTipoTrabajoState);

    function updateTipoTrabajoState() {
        const selectedOption = select.options[select.selectedIndex];
        const hasSelectedOption = Boolean(selectedOption && selectedOption.value);
        const requiresPrintConfiguration = hasSelectedOption && selectedOption.dataset.requiresPrint === "true";

        helperCard.classList.toggle("is-accent", hasSelectedOption);
        helperLabel.textContent = hasSelectedOption ? selectedOption.dataset.uploadLabel : defaultLabel;
        helperDescription.textContent = hasSelectedOption ? selectedOption.dataset.uploadDescription : defaultDescription;
        uploadTitle.textContent = hasSelectedOption ? selectedOption.dataset.uploadLabel : defaultUploadTitle;
        uploadDescription.textContent = hasSelectedOption ? selectedOption.dataset.uploadDescription : defaultUploadDescription;

        printConfigSections.forEach(function (section) {
            section.hidden = !requiresPrintConfiguration;

            section.querySelectorAll("input, select, textarea").forEach(function (field) {
                field.disabled = !requiresPrintConfiguration;
            });
        });
    }
}

function initPriceEstimator(options) {
    const jobTypeSelect = options.jobTypeSelect;
    const fileInput = options.fileInput;
    const fileList = options.fileList;
    const fileHint = options.fileHint;
    const priceEstimator = options.priceEstimator;
    const previewUrl = priceEstimator.dataset.previewUrl;
    const totalElement = priceEstimator.querySelector("[data-price-total]");
    const pagesElement = priceEstimator.querySelector("[data-price-pages]");
    const breakdownElement = priceEstimator.querySelector("[data-price-breakdown]");
    const noteElement = priceEstimator.querySelector("[data-price-note]");
    const copiesInput = document.querySelector("#copies");
    const observationsInput = document.querySelector("#observations");
    const colorInputs = document.querySelectorAll("input[name='colorMode']");
    const printSideInputs = document.querySelectorAll("input[name='printSide']");
    const paperSizeInputs = document.querySelectorAll("input[name='paperSize']");
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const state = {
        fileCount: 0,
        pageCount: 0,
        pending: false,
        requestId: 0
    };

    if (!totalElement || !pagesElement || !breakdownElement || !noteElement) {
        return;
    }

    fileHint.dataset.defaultText = fileHint.textContent;

    const watchedFields = [
        jobTypeSelect,
        copiesInput,
        observationsInput,
        ...Array.from(colorInputs),
        ...Array.from(printSideInputs),
        ...Array.from(paperSizeInputs)
    ].filter(Boolean);

    watchedFields.forEach(function (field) {
        field.addEventListener("change", updateEstimate);
        field.addEventListener("input", updateEstimate);
    });

    fileInput.addEventListener("change", function () {
        const selection = renderSelectedFiles(fileInput, fileList, fileHint);

        if (!selection.valid) {
            state.fileCount = 0;
            state.pageCount = 0;
            state.pending = false;
            updateEstimate(selection.message);
            return;
        }

        state.fileCount = selection.fileCount;
        state.pageCount = selection.fileCount;
        updateEstimate();

        if (!selection.fileCount) {
            return;
        }

        state.pending = true;
        updateEstimate("Calculando paginas PDF y ajustando el precio orientativo...");
        requestPreview();
    });

    updateEstimate();

    function updateEstimate(customNote) {
        const jobType = jobTypeSelect.value;

        if (!jobType) {
            totalElement.textContent = "0,00 â‚¬";
            pagesElement.textContent = pageCountLabel(state.pageCount);
            breakdownElement.textContent = "Selecciona un servicio para ver el precio orientativo del pedido.";
            noteElement.textContent = customNote || "El importe se calcula automaticamente al combinar archivos y configuracion.";
            return;
        }

        const estimate = calculateEstimate({
            jobType: jobType,
            fileCount: state.fileCount,
            pageCount: state.pageCount,
            copies: normalizeCopies(copiesInput && copiesInput.value),
            colorMode: getCheckedValue(colorInputs) || "BLACK_AND_WHITE",
            printSide: getCheckedValue(printSideInputs) || "ONE_SIDED",
            paperSize: getCheckedValue(paperSizeInputs) || "A4",
            urgentRequested: Boolean(observationsInput && /(urgente|express)/i.test(observationsInput.value || ""))
        });

        totalElement.textContent = formatEuro(estimate.total);
        pagesElement.textContent = pageCountLabel(state.pageCount);
        breakdownElement.textContent = estimate.breakdown;
        noteElement.textContent = customNote || estimate.note;
    }

    function requestPreview() {
        state.requestId += 1;
        const currentRequestId = state.requestId;

        const formData = new FormData();
        appendIfValue(formData, "jobType", jobTypeSelect.value);
        appendIfValue(formData, "copies", copiesInput ? copiesInput.value : "");
        appendIfValue(formData, "colorMode", getCheckedValue(colorInputs));
        appendIfValue(formData, "printSide", getCheckedValue(printSideInputs));
        appendIfValue(formData, "paperSize", getCheckedValue(paperSizeInputs));
        appendIfValue(formData, "observations", observationsInput ? observationsInput.value : "");

        Array.from(fileInput.files || []).forEach(function (file) {
            formData.append("files", file);
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
                state.pending = false;
                fileHint.textContent = buildDetectedFileHint(fileInput.files || [], state.pageCount);
                updateEstimate(data.note);
            })
            .catch(function (error) {
                if (currentRequestId !== state.requestId) {
                    return;
                }

                state.pending = false;
                state.pageCount = 0;
                updateEstimate(error.message);
            });
    }
}

function renderSelectedFiles(fileInput, fileList, fileHint) {
    const files = Array.from(fileInput.files || []);
    fileList.innerHTML = "";

    if (!files.length) {
        fileHint.classList.remove("is-error");
        fileHint.textContent = fileHint.dataset.defaultText || fileHint.textContent;
        return {
            valid: true,
            fileCount: 0
        };
    }

    const invalidFiles = files.filter(function (file) {
        return !isAllowedFile(file.name);
    });

    if (invalidFiles.length) {
        fileInput.value = "";
        fileHint.classList.add("is-error");
        fileHint.textContent = "No se admiten archivos Word en este formulario. Sube PDF, JPG o PNG. Para documentos, exporta antes a PDF.";
        return {
            valid: false,
            fileCount: 0,
            message: "No se admiten archivos Word en este formulario. Sube PDF, JPG o PNG. Para documentos, exporta antes a PDF."
        };
    }

    fileHint.classList.remove("is-error");
    fileHint.textContent = buildDetectedFileHint(files, 0);

    files.forEach(function (file) {
        const listItem = document.createElement("li");
        const name = document.createElement("strong");
        const size = document.createElement("span");

        name.textContent = file.name;
        size.textContent = formatFileSize(file.size);

        listItem.appendChild(name);
        listItem.appendChild(size);
        fileList.appendChild(listItem);
    });

    return {
        valid: true,
        fileCount: files.length
    };
}

function calculateEstimate(input) {
    switch (input.jobType) {
        case "IMPRESION":
            return calculatePrintLikeEstimate(input, 0.06, 0.45, "impresion");
        case "FOTOCOPIAS":
            return calculatePrintLikeEstimate(input, 0.05, 0.18, "fotocopias");
        case "PUBLICIDAD_IMPRENTA":
            return calculateCampaignEstimate(input);
        case "ENCUADERNACION":
            return calculateSimpleEstimate(input, 3.5, 1.1, "Base de encuadernacion + acabado por archivo", "encuadernacion");
        case "PLASTIFICADO":
            return calculateSimpleEstimate(input, 1.8, 0.9, "Plastificado calculado por documento adjunto", "plastificado");
        case "DISENO_GRAFICO":
            return calculateSimpleEstimate(input, 25, 4.5, "Base de diseno + material o referencias adjuntas", "diseno grafico");
        case "PERSONALIZACION":
            return calculateSimpleEstimate(input, 9.9, 3.5, "Personalizacion orientativa segun unidades o artes adjuntas", "personalizacion");
        case "SERVICIOS_ADICIONALES":
            return calculateSimpleEstimate(input, 0.5, 0.75, "Servicio adicional calculado por documento o gestion", "servicios adicionales");
        default:
            return calculateSimpleEstimate(input, 12, 2.5, "Referencia base para encargos especiales", "encargo especial");
    }
}

function calculatePrintLikeEstimate(input, bwUnitPrice, colorUnitPrice, label) {
    const unitPrice = (input.colorMode === "COLOR" ? colorUnitPrice : bwUnitPrice)
        * sizeMultiplier(input.paperSize)
        * sideMultiplier(input.printSide);
    const pages = Math.max(input.pageCount, 1);
    const total = applyUrgentSupplement(unitPrice * pages * input.copies, input.urgentRequested);
    const breakdown = pageReference(input.pageCount, input.fileCount)
        + " x "
        + input.copies
        + " copia(s) â€¢ "
        + colorLabel(input.colorMode)
        + " â€¢ "
        + input.paperSize
        + " â€¢ "
        + printSideLabel(input.printSide)
        + (input.urgentRequested ? " + suplemento urgente" : "");

    return {
        total: total,
        breakdown: breakdown,
        note: buildNote(input.fileCount, label)
    };
}

function calculateCampaignEstimate(input) {
    const unitPrice = (input.colorMode === "COLOR" ? 0.22 : 0.12)
        * (input.paperSize === "A3" ? 1.4 : 1)
        * (input.printSide === "DOUBLE_SIDED" ? 1.3 : 1);
    const pages = Math.max(input.pageCount, 1);
    const total = applyUrgentSupplement(19 + (unitPrice * pages * input.copies), input.urgentRequested);
    const breakdown = "Base de imprenta + tirada estimada de "
        + pageReference(input.pageCount, input.fileCount)
        + " x "
        + input.copies
        + " unidad(es) â€¢ "
        + colorLabel(input.colorMode)
        + " â€¢ "
        + input.paperSize
        + (input.urgentRequested ? " + suplemento urgente" : "");

    return {
        total: total,
        breakdown: breakdown,
        note: buildNote(input.fileCount, "publicidad e imprenta")
    };
}

function calculateSimpleEstimate(input, basePrice, extraPerFile, breakdownPrefix, label) {
    const pricedFiles = Math.max(input.fileCount, 1);
    const total = applyUrgentSupplement(basePrice + (extraPerFile * (pricedFiles - 1)), input.urgentRequested);

    return {
        total: total,
        breakdown: breakdownPrefix + " â€¢ " + fileReference(input.fileCount) + (input.urgentRequested ? " + suplemento urgente" : ""),
        note: buildNote(input.fileCount, label)
    };
}

function getCheckedValue(elements) {
    const checked = Array.from(elements || []).find(function (element) {
        return element.checked;
    });

    return checked ? checked.value : "";
}

function normalizeCopies(value) {
    const parsedValue = parseInt(value, 10);
    return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : 1;
}

function sizeMultiplier(paperSize) {
    return paperSize === "A3" ? 1.85 : 1;
}

function sideMultiplier(printSide) {
    return printSide === "DOUBLE_SIDED" ? 1.8 : 1;
}

function fileReference(fileCount) {
    return fileCount > 0 ? fileCount + " archivo(s)" : "1 archivo de referencia";
}

function pageReference(pageCount, fileCount) {
    if (pageCount > 0) {
        return pageCount === 1
            ? "1 pagina detectada en " + Math.max(fileCount, 1) + " archivo"
            : pageCount + " paginas detectadas en " + Math.max(fileCount, 1) + " archivo(s)";
    }

    return "1 pagina estimada base";
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

    return "Precio orientativo base para " + label + ". Sube tus archivos en PDF, JPG o PNG para afinar mejor el importe antes de guardar el pedido.";
}

function colorLabel(colorMode) {
    return colorMode === "COLOR" ? "Color" : "Blanco y negro";
}

function printSideLabel(printSide) {
    return printSide === "DOUBLE_SIDED" ? "Doble cara" : "Una cara";
}

function applyUrgentSupplement(total, urgentRequested) {
    return urgentRequested ? total + 2 : total;
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
        return "Formatos permitidos: PDF, JPG, PNG | Maximo 15 MB por archivo. Para documentos, sube PDF para calcular bien las paginas.";
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
    return ["pdf", "jpg", "jpeg", "png"].includes(extension);
}

function appendIfValue(formData, name, value) {
    if (value !== null && value !== undefined && String(value).trim() !== "") {
        formData.append(name, value);
    }
}

