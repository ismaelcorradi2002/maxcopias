const WORKER_PAGE_SIZE = 5;

document.addEventListener("DOMContentLoaded", function () {
    initWorkerTables();
});

function initWorkerTables() {
    document.querySelectorAll("[data-worker-table]").forEach(table => {
        const key = table.dataset.workerTable;
        const isPaginated = table.dataset.workerPaginated === "true";
        const state = {
            visibleCount: isPaginated ? WORKER_PAGE_SIZE : Number.MAX_SAFE_INTEGER
        };

        const controls = getWorkerControls(key);
        const inputs = [
            controls.searchInput,
            controls.statusInput,
            controls.dateInput,
            controls.categoryInput
        ];

        inputs.forEach(input => {
            if (!input) {
                return;
            }

            input.addEventListener("input", () => {
                state.visibleCount = WORKER_PAGE_SIZE;
                renderWorkerTable(table, controls, state);
            });
            input.addEventListener("change", () => {
                state.visibleCount = WORKER_PAGE_SIZE;
                renderWorkerTable(table, controls, state);
            });
        });

        renderWorkerTable(table, controls, state, isPaginated);
    });
}

function getWorkerControls(key) {
    return {
        searchInput: document.querySelector(`[data-worker-search="${key}"]`),
        statusInput: document.querySelector(`[data-worker-status="${key}"]`),
        dateInput: document.querySelector(`[data-worker-date="${key}"]`),
        categoryInput: document.querySelector(`[data-worker-category="${key}"]`)
    };
}

function renderWorkerTable(table, controls, state, isPaginated = true) {
    const rows = Array.from(table.querySelectorAll("tbody tr"));
    const filteredRows = rows.filter(row => rowMatchesWorkerFilters(row, controls));
    const visibleRows = isPaginated ? filteredRows.slice(0, state.visibleCount) : filteredRows;

    rows.forEach(row => {
        row.hidden = true;
    });

    visibleRows.forEach(row => {
        row.hidden = false;
    });

    renderWorkerPagination(table, filteredRows.length, visibleRows.length, state, controls, isPaginated);
}

function rowMatchesWorkerFilters(row, controls) {
    const query = normalizeWorkerSearch(controls.searchInput?.value || "");
    const status = controls.statusInput?.value || "";
    const date = controls.dateInput?.value || "";
    const category = normalizeWorkerSearch(controls.categoryInput?.value || "");

    const rowText = normalizeWorkerSearch(row.textContent);
    const rowStatus = row.dataset.status || "";
    const rowDate = row.dataset.date || "";
    const rowCategories = normalizeWorkerSearch(row.dataset.categories || "");

    const matchesSearch = !query || rowText.includes(query);
    const matchesStatus = !status || rowStatus === status;
    const matchesDate = !date || rowDate === date;
    const matchesCategory = !category || rowCategories.split(/\s+/).includes(category) || rowCategories.includes(category);

    return matchesSearch && matchesStatus && matchesDate && matchesCategory;
}

function renderWorkerPagination(table, totalCount, visibleCount, state, controls, isPaginated) {
    const wrapper = table.closest(".worker-table-wrap");

    if (!wrapper) {
        return;
    }

    wrapper.querySelector(".worker-load-more")?.remove();
    wrapper.querySelector(".worker-no-results")?.remove();

    if (totalCount === 0) {
        const empty = document.createElement("p");
        empty.className = "worker-no-results";
        empty.textContent = "No se han encontrado resultados con esos filtros.";
        wrapper.appendChild(empty);
        return;
    }

    if (!isPaginated) {
        return;
    }

    if (totalCount <= WORKER_PAGE_SIZE) {
        return;
    }

    const controlsBox = document.createElement("div");
    controlsBox.className = "worker-load-more";

    if (state.visibleCount > WORKER_PAGE_SIZE) {
        const lessButton = document.createElement("button");
        lessButton.type = "button";
        lessButton.className = "button button-outline worker-load-more-button";
        lessButton.textContent = "Ver menos";
        lessButton.addEventListener("click", () => {
            state.visibleCount = Math.max(WORKER_PAGE_SIZE, state.visibleCount - WORKER_PAGE_SIZE);
            renderWorkerTable(table, controls, state, isPaginated);
        });
        controlsBox.appendChild(lessButton);
    }

    if (visibleCount < totalCount) {
        const moreButton = document.createElement("button");
        moreButton.type = "button";
        moreButton.className = "button button-outline worker-load-more-button";
        moreButton.textContent = "Ver mas";
        moreButton.addEventListener("click", () => {
            state.visibleCount += WORKER_PAGE_SIZE;
            renderWorkerTable(table, controls, state, isPaginated);
        });
        controlsBox.appendChild(moreButton);
    }

    const counter = document.createElement("span");
    counter.textContent = `${visibleCount} de ${totalCount}`;
    controlsBox.appendChild(counter);
    wrapper.appendChild(controlsBox);
}

function normalizeWorkerSearch(value) {
    return String(value || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}
