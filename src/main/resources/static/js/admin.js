function switchTab(button, tab) {
    document.querySelectorAll(".admin-buttons button").forEach(btn => btn.classList.remove("active"));
    button.classList.add("active");

    const tableContainer = document.querySelector(".table-container");
    tableContainer.innerHTML = '<div class="admin-loading">Cargando datos...</div>';

    const apiEndpoint = tab === "users" ? "/admin/api/users" : "/admin/api/products";

    fetch(apiEndpoint)
        .then(response => response.json())
        .then(data => {
            if (data.length === 0) {
                tableContainer.innerHTML = `<p class="no-data">No hay ${tab === "users" ? "usuarios" : "productos"}.</p>`;
                return;
            }

            if (tab === "users") {
                renderUsersTable(tableContainer, data);
                return;
            }

            renderProductsTable(tableContainer, data);
        })
        .catch(error => {
            console.error("Error:", error);
            tableContainer.innerHTML = '<p class="no-data">Error cargando datos.</p>';
        });
}

function renderUsersTable(tableContainer, data) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";

    tableContainer.innerHTML = `
        <table class="admin-table admin-users-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Email</th>
                    <th>Telefono</th>
                    <th>Rol</th>
                    <th>Creado</th>
                    <th>Cambiar rol</th>
                </tr>
            </thead>
            <tbody>
                ${data.map(user => `
                    <tr>
                        <td class="admin-cell-id">${user.id}</td>
                        <td class="admin-cell-name" title="${user.firstName || ""} ${user.lastName || ""}">
                            ${user.firstName || ""} ${user.lastName || ""}
                        </td>
                        <td class="admin-cell-email" title="${user.email}">
                            ${user.email}
                        </td>
                        <td class="admin-cell-phone">${user.phone || ""}</td>
                        <td class="admin-role-cell"><span class="admin-role-badge">${user.rol === "ROLE_USER" ? "Usuario" : "Admin"}</span></td>
                        <td class="admin-date-cell">${formatAdminDate(user.createdAt)}</td>
                        <td class="admin-action-cell">
                            <form action="/admin/update-role/${user.id}" method="post" class="admin-role-form" data-current-role="${user.rol}">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <div class="admin-role-dropdown" data-role-dropdown>
                                    <select name="newRole" class="admin-role-select admin-role-native" data-user-name="${user.firstName || ""} ${user.lastName || ""}" aria-label="Cambiar rol">
                                        <option value="ROLE_USER" ${user.rol === "ROLE_USER" ? "selected" : ""}>Usuario</option>
                                        <option value="ROLE_ADMIN" ${user.rol === "ROLE_ADMIN" ? "selected" : ""}>Admin</option>
                                    </select>
                                    <button class="admin-role-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                                        <span data-role-trigger-label>${user.rol === "ROLE_USER" ? "Usuario" : "Admin"}</span>
                                        <span class="admin-role-trigger-icon" aria-hidden="true"></span>
                                    </button>
                                    <div class="admin-role-menu" role="listbox">
                                        <button class="admin-role-option" type="button" role="option" data-role-value="ROLE_USER">
                                            <span class="admin-role-option-dot"></span>
                                            Usuario
                                        </button>
                                        <button class="admin-role-option" type="button" role="option" data-role-value="ROLE_ADMIN">
                                            <span class="admin-role-option-dot admin-role-option-dot-admin"></span>
                                            Admin
                                        </button>
                                    </div>
                                </div>
                            </form>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;

    initRoleChangeConfirmation();
    initRoleDropdowns();
}

function formatAdminDate(value) {
    if (!value) {
        return "";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("es-ES", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date).replace(",", "");
}

function renderProductsTable(tableContainer, data) {
    tableContainer.innerHTML = `
        <div class="admin-product-toolbar">
            <button class="button button-primary admin-new-product-btn" type="button">
                Crear un nuevo producto
            </button>
            <label class="admin-product-search" for="admin-product-search">
                <span>Buscar producto</span>
                <input id="admin-product-search" type="search" placeholder="Buscar por ID, nombre o categoria" autocomplete="off">
            </label>
            <label class="admin-product-filter" for="admin-category-filter">
                <span>Filtrar categoria</span>
                <select id="admin-category-filter">
                    <option value="">Todas las categorias</option>
                    ${getProductCategories(data).map(category => `<option value="${category}">${category}</option>`).join("")}
                </select>
            </label>
        </div>
        <div class="admin-products-table-region" data-products-table-region></div>
    `;

    const tableRegion = tableContainer.querySelector("[data-products-table-region]");
    const searchInput = tableContainer.querySelector("#admin-product-search");
    const categoryFilter = tableContainer.querySelector("#admin-category-filter");

    function paintProducts(products) {
        if (products.length === 0) {
            tableRegion.innerHTML = '<p class="no-data">No se han encontrado productos con esa busqueda.</p>';
            return;
        }

        tableRegion.innerHTML = `
            <table class="admin-table admin-products-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Precio</th>
                    <th>Stock</th>
                    <th>Categorias</th>
                    <th>Accion</th>
                    <th>Eliminar</th>
                </tr>
            </thead>
            <tbody>
                ${products.map(product => `
                    <tr>
                        <td>${product.id}</td>
                        <td>${product.nombre}</td>
                        <td>${product.precio}</td>
                        <td>${product.stock}</td>
                        <td class="admin-product-categories">${product.categorias ? product.categorias.map(cat => cat.nombre).join(", ") : ""}</td>
                        <td>
                            <button class="button button-small button-outline" type="button" onclick="window.location.href='/editarstock/${product.id}'">
                                Editar
                            </button>
                        </td>
                        <td>
                            <button class="button button-small button-outline admin-delete-btn" type="button" data-product-id="${product.id}" title="Eliminar producto">
                                Eliminar
                            </button>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
        `;
    }

    paintProducts(data);

    function applyProductFilters() {
        const query = normalizeAdminSearch(searchInput.value);
        const selectedCategory = normalizeAdminSearch(categoryFilter.value);

        if (!query && !selectedCategory) {
            paintProducts(data);
            return;
        }

        const filteredProducts = data.filter(product => {
            const categories = product.categorias ? product.categorias.map(cat => cat.nombre).join(" ") : "";
            const searchableText = normalizeAdminSearch(`${product.id} ${product.nombre} ${categories}`);
            const matchesSearch = !query || searchableText.includes(query);
            const matchesCategory = !selectedCategory || (product.categorias || []).some(cat => normalizeAdminSearch(cat.nombre) === selectedCategory);

            return matchesSearch && matchesCategory;
        });

        paintProducts(filteredProducts);
    }

    searchInput.addEventListener("input", applyProductFilters);
    categoryFilter.addEventListener("change", applyProductFilters);
}

function normalizeAdminSearch(value) {
    return String(value || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}

function getProductCategories(products) {
    const categories = new Set();

    products.forEach(product => {
        (product.categorias || []).forEach(category => {
            if (category.nombre) {
                categories.add(category.nombre);
            }
        });
    });

    return Array.from(categories).sort((first, second) => first.localeCompare(second, "es"));
}

document.addEventListener("DOMContentLoaded", function() {
    const activeAdminTab = document.querySelector(".admin-buttons .active");
    if (activeAdminTab) {
        switchTab(activeAdminTab, "users");
    }
});

function initRoleChangeConfirmation() {
    const modal = document.querySelector("[data-role-confirm-modal]");
    const message = document.querySelector("[data-role-confirm-message]");
    const acceptButton = document.querySelector("[data-role-confirm-accept]");
    const cancelButtons = document.querySelectorAll("[data-role-confirm-cancel]");
    let pendingSelect = null;
    let pendingForm = null;
    let previousValue = null;

    if (!modal || !message || !acceptButton) {
        return;
    }

    function closeModal(restorePreviousValue) {
        modal.hidden = true;
        document.body.classList.remove("admin-modal-open");

        if (restorePreviousValue && pendingSelect) {
            pendingSelect.value = previousValue;
            syncRoleDropdown(pendingSelect);
        }

        pendingSelect = null;
        pendingForm = null;
        previousValue = null;
    }

    function openModal(select, form, userName, newRoleText) {
        pendingSelect = select;
        pendingForm = form;
        previousValue = select.dataset.previousValue || form.dataset.currentRole;
        message.textContent = `Vas a cambiar el rol de ${userName} a ${newRoleText}. Esta accion modificara sus permisos de acceso.`;
        modal.hidden = false;
        document.body.classList.add("admin-modal-open");
        acceptButton.focus();
    }

    cancelButtons.forEach(button => {
        button.addEventListener("click", function () {
            closeModal(true);
        });
    });

    acceptButton.addEventListener("click", function () {
        if (pendingForm) {
            pendingForm.submit();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !modal.hidden) {
            closeModal(true);
        }
    });

    document.querySelectorAll(".admin-role-select").forEach(select => {
        select.addEventListener("focus", function () {
            this.dataset.previousValue = this.value;
        });

        select.addEventListener("change", function () {
            const form = this.closest(".admin-role-form");
            const newRoleText = this.options[this.selectedIndex].text;
            const userName = (this.dataset.userName || "este usuario").trim();

            if (this.value === (this.dataset.previousValue || form.dataset.currentRole)) {
                return;
            }

            openModal(this, form, userName || "este usuario", newRoleText);
        });
    });
}

function initRoleDropdowns() {
    document.querySelectorAll("[data-role-dropdown]").forEach(dropdown => {
        const select = dropdown.querySelector(".admin-role-native");
        const trigger = dropdown.querySelector(".admin-role-trigger");
        const options = dropdown.querySelectorAll(".admin-role-option");

        if (!select || !trigger || options.length === 0) {
            return;
        }

        syncRoleDropdown(select);

        trigger.addEventListener("click", function () {
            const isOpen = dropdown.classList.toggle("is-open");
            dropdown.closest(".table-container")?.classList.toggle("has-open-role-dropdown", isOpen);
            trigger.setAttribute("aria-expanded", String(isOpen));
            closeOtherRoleDropdowns(dropdown);
        });

        options.forEach(option => {
            option.addEventListener("click", function () {
                const nextValue = this.dataset.roleValue;

                if (!nextValue || nextValue === select.value) {
                    dropdown.classList.remove("is-open");
                    dropdown.closest(".table-container")?.classList.remove("has-open-role-dropdown");
                    trigger.setAttribute("aria-expanded", "false");
                    return;
                }

                select.dataset.previousValue = select.value;
                select.value = nextValue;
                syncRoleDropdown(select);
                dropdown.classList.remove("is-open");
                dropdown.closest(".table-container")?.classList.remove("has-open-role-dropdown");
                trigger.setAttribute("aria-expanded", "false");
                select.dispatchEvent(new Event("change", { bubbles: true }));
            });
        });
    });
}

function syncRoleDropdown(select) {
    const dropdown = select.closest("[data-role-dropdown]");

    if (!dropdown) {
        return;
    }

    const selectedOption = select.options[select.selectedIndex];
    const label = dropdown.querySelector("[data-role-trigger-label]");
    const options = dropdown.querySelectorAll(".admin-role-option");

    if (label && selectedOption) {
        label.textContent = selectedOption.text;
    }

    options.forEach(option => {
        const isSelected = option.dataset.roleValue === select.value;
        option.classList.toggle("is-selected", isSelected);
        option.setAttribute("aria-selected", String(isSelected));
    });
}

function closeOtherRoleDropdowns(activeDropdown) {
    document.querySelectorAll("[data-role-dropdown].is-open").forEach(dropdown => {
        if (dropdown === activeDropdown) {
            return;
        }

        dropdown.classList.remove("is-open");
        dropdown.closest(".table-container")?.classList.remove("has-open-role-dropdown");
        dropdown.querySelector(".admin-role-trigger")?.setAttribute("aria-expanded", "false");
    });
}

document.addEventListener("click", function (event) {
    if (event.target.closest("[data-role-dropdown]")) {
        return;
    }

    // Delete product handler
    if (event.target.matches(".admin-delete-btn")) {
        event.preventDefault();
        const button = event.target;
        const productId = button.dataset.productId;
        const productName = button.closest("tr").querySelector("td:nth-child(2)").textContent.trim();
        
        if (confirm(`¿Estás seguro que quieres eliminar el producto "${productName}"? Esta acción no se puede deshacer.`)) {
            deleteProduct(productId);
        }
        return;
    }

    // New product button
    if (event.target.matches(".admin-new-product-btn")) {
        window.location.href = '/admin/crear-producto';
    }

    closeOtherRoleDropdowns(null);
});

function deleteProduct(productId) {
    fetch(`/admin/delete-producto/${productId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success === 'true') {
            // Refresh products
            const activeTabButton = document.querySelector('.admin-buttons .active');
            const tabName = activeTabButton ? activeTabButton.dataset.tab || 'products' : 'products';
            switchTab(activeTabButton, tabName);
            alert(data.message);
        } else {
            alert('Error: ' + data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al eliminar producto. Revisa la consola.');
    });
}
