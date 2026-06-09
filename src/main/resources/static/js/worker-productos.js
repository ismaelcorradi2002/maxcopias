document.addEventListener("DOMContentLoaded", function () {
    const table = document.getElementById("worker-products-table");
    const searchInput = document.getElementById("worker-product-search");
    const categorySelect = document.getElementById("worker-product-category");

    if (!table || !searchInput || !categorySelect) {
        return;
    }

    const rows = Array.from(table.querySelectorAll("tbody tr"));
    const wrapper = table.closest(".worker-table-wrap");

    function normalize(value) {
        return String(value || "")
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim();
    }

    function paint() {
        const query = normalize(searchInput.value);
        const category = normalize(categorySelect.value);
        let visibleRows = 0;

        rows.forEach(function (row) {
            const rowText = normalize(row.textContent);
            const rowCategories = normalize(row.dataset.categories || "");
            const matchesQuery = !query || rowText.includes(query);
            const matchesCategory = !category || rowCategories.includes(category);
            const isVisible = matchesQuery && matchesCategory;

            row.hidden = !isVisible;
            if (isVisible) {
                visibleRows += 1;
            }
        });

        wrapper?.querySelector(".worker-no-results")?.remove();

        if (visibleRows === 0 && wrapper) {
            const empty = document.createElement("p");
            empty.className = "worker-no-results";
            empty.textContent = "No se han encontrado resultados con esos filtros.";
            wrapper.appendChild(empty);
        }
    }

    searchInput.addEventListener("input", paint);
    categorySelect.addEventListener("change", paint);
    paint();
});
