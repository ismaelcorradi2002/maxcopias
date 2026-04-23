const ADMIN_PAGE_SIZE = 5;

function switchTab(button, tab) {
    document.querySelectorAll(".admin-buttons button").forEach(btn => btn.classList.remove("active"));
    button.classList.add("active");

    const tableContainer = document.querySelector(".table-container");
    tableContainer.classList.toggle("is-users-tab", tab === "users");
    tableContainer.innerHTML = '<div class="admin-loading">Cargando datos...</div>';

    const apiEndpoint = tab === "users" ? "/admin/api/users" : tab === "products" ? "/admin/api/products" : tab === "orders" ? "/admin/api/pedidos" : "/admin/api/categorias";

    fetch(apiEndpoint)
        .then(response => response.json())
        .then(data => {
            if (!Array.isArray(data)) {
                console.error("Respuesta inesperada de la API:", data);
                tableContainer.innerHTML = '<p class="no-data">Error al cargar datos. Revisa la consola para más detalles.</p>';
                return;
            }

            if (data.length === 0) {
                tableContainer.innerHTML = `<p class="no-data">No hay ${tab === "users" ? "usuarios" : tab === "products" ? "productos" : tab === "orders" ? "pedidos" : "categorías"}.</p>`;
                return;
            }

            if (tab === "users") {
                renderUsersTable(tableContainer, data);
                return;
            }
            if (tab === "categories") {
                renderCategoriesTable(tableContainer, data);
                return;
            }
            if (tab === "orders") {
                renderOrdersTable(tableContainer, data);
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
    let visibleCount = ADMIN_PAGE_SIZE;
    let currentUsers = data;

    tableContainer.innerHTML = `
        <div class="admin-product-toolbar admin-user-toolbar">
            <label class="admin-product-search admin-user-search" for="admin-user-search">
                <span>Buscar usuario</span>
                <input id="admin-user-search" type="search" placeholder="Buscar por nombre, correo o telefono" autocomplete="off">
            </label>
        </div>
        <div class="admin-users-table-region" data-users-table-region></div>
    `;

    const tableRegion = tableContainer.querySelector("[data-users-table-region]");
    const searchInput = tableContainer.querySelector("#admin-user-search");

    function paintUsers(users) {
        const visibleUsers = users.slice(0, visibleCount);

        if (users.length === 0) {
            tableRegion.innerHTML = '<p class="no-data">No se han encontrado usuarios con esa busqueda.</p>';
            return;
        }

        tableRegion.innerHTML = `
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
                    <th>Eliminar</th>
                </tr>
            </thead>
            <tbody>
                ${visibleUsers.map(user => `
                    <tr>
                        <td class="admin-cell-id">${user.id}</td>
                        <td class="admin-cell-name" title="${user.firstName || ""} ${user.lastName || ""}">
                            ${user.firstName || ""} ${user.lastName || ""}
                        </td>
                        <td class="admin-cell-email" title="${user.email}">
                            ${user.email}
                        </td>
                        <td class="admin-cell-phone">${user.phone || ""}</td>
                        <td class="admin-role-cell"><span class="admin-role-badge">${formatRoleName(user.rol)}</span></td>
                        <td class="admin-date-cell">${formatAdminDate(user.createdAt)}</td>
                        <td class="admin-action-cell">
                            <form action="/admin/update-role/${user.id}" method="post" class="admin-role-form" data-current-role="${user.rol}">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <div class="admin-role-dropdown" data-role-dropdown>
                                    <select name="newRole" class="admin-role-select admin-role-native" data-user-name="${user.firstName || ""} ${user.lastName || ""}" aria-label="Cambiar rol">
                                        <option value="ROLE_USER" ${user.rol === "ROLE_USER" ? "selected" : ""}>Usuario</option>
                                        <option value="ROLE_WORKER" ${user.rol === "ROLE_WORKER" ? "selected" : ""}>Trabajador</option>
                                        <option value="ROLE_ADMIN" ${user.rol === "ROLE_ADMIN" ? "selected" : ""}>Admin</option>
                                    </select>
                                    <button class="admin-role-trigger" type="button" aria-haspopup="listbox" aria-expanded="false">
                                        <span data-role-trigger-label>${formatRoleName(user.rol)}</span>
                                        <span class="admin-role-trigger-icon" aria-hidden="true"></span>
                                    </button>
                                    <div class="admin-role-menu" role="listbox">
                                        <button class="admin-role-option" type="button" role="option" data-role-value="ROLE_USER">
                                            <span class="admin-role-option-dot"></span>
                                            Usuario
                                        </button>
                                        <button class="admin-role-option" type="button" role="option" data-role-value="ROLE_WORKER">
                                            <span class="admin-role-option-dot admin-role-option-dot-worker"></span>
                                            Trabajador
                                        </button>
                                        <button class="admin-role-option" type="button" role="option" data-role-value="ROLE_ADMIN">
                                            <span class="admin-role-option-dot admin-role-option-dot-admin"></span>
                                            Admin
                                        </button>
                                    </div>
                                </div>
                            </form>
                        </td>
                        <td class="admin-action-cell">
                            <button class="button button-small button-outline admin-delete-btn" type="button" data-user-id="${user.id}" data-user-name="${user.firstName || ""} ${user.lastName || ""}" title="Eliminar usuario">
                                Eliminar
                            </button>
                        </td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
        ${renderListPaginationControls(visibleUsers.length, users.length, "usuarios")}
        `;

        initRoleChangeConfirmation();
        initRoleDropdowns();
        tableRegion.querySelector("[data-load-more]")?.addEventListener("click", function () {
            visibleCount += ADMIN_PAGE_SIZE;
            paintUsers(users);
        });
        tableRegion.querySelector("[data-load-less]")?.addEventListener("click", function () {
            visibleCount = Math.max(ADMIN_PAGE_SIZE, visibleCount - ADMIN_PAGE_SIZE);
            paintUsers(users);
        });
    }

    paintUsers(currentUsers);

    searchInput.addEventListener("input", function () {
        const query = normalizeAdminSearch(this.value);
        visibleCount = ADMIN_PAGE_SIZE;

        if (!query) {
            currentUsers = data;
            paintUsers(currentUsers);
            return;
        }

        currentUsers = data.filter(user => {
            const searchableText = normalizeAdminSearch(`${user.firstName || ""} ${user.lastName || ""} ${user.email || ""} ${user.phone || ""}`);
            return searchableText.includes(query);
        });

        paintUsers(currentUsers);
    });
}

function formatAdminDate(value) {
    if (!value) {
        return "-";
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

function renderCategoriesTable(tableContainer, data) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
    let visibleCount = ADMIN_PAGE_SIZE;
    let currentCategories = data;

    tableContainer.innerHTML = `
        <div class="admin-product-toolbar">
            <button class="button button-primary admin-new-category-btn" type="button" onclick="window.location.href='/admin/crear-categoria'">
                Crear nueva categoría
            </button>
            <label class="admin-product-search" for="admin-category-search">
                <span>Buscar categoría</span>
                <input id="admin-category-search" type="search" placeholder="Buscar por ID o nombre" autocomplete="off">
            </label>
        </div>
        <div class="admin-categories-table-region" data-categories-table-region></div>
    `;

    const tableRegion = tableContainer.querySelector("[data-categories-table-region]");
    const searchInput = tableContainer.querySelector("#admin-category-search");

    function paintCategories(categories) {
        const visibleCategories = categories.slice(0, visibleCount);

        if (categories.length === 0) {
            tableRegion.innerHTML = '<p class="no-data">No se han encontrado categorías con esa búsqueda.</p>';
            return;
        }

        tableRegion.innerHTML = `
            <table class="admin-table admin-categories-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Nombre</th>
                        <th>Descripción</th>
                        <th># Productos</th>
                        <th>Eliminar</th>
                    </tr>
                </thead>
                <tbody>
                    ${visibleCategories.map(category => `
                        <tr>
                            <td>${category.id}</td>
                            <td>${category.nombre}</td>
                            <td class="admin-category-description" title="${category.descripcion || ''}">${category.descripcion || '-'}</td>
                            <td>${category.productos ? category.productos.length : 0}</td>
                            <td>
                                <button class="button button-small button-outline admin-delete-btn" type="button" data-category-id="${category.id}" data-category-name="${category.nombre}" title="Eliminar categoría">
                                    Eliminar
                                </button>
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
            ${renderListPaginationControls(visibleCategories.length, categories.length, "categorias")}
        `;

        tableRegion.querySelector("[data-load-more]")?.addEventListener("click", function () {
            visibleCount += ADMIN_PAGE_SIZE;
            paintCategories(categories);
        });
        tableRegion.querySelector("[data-load-less]")?.addEventListener("click", function () {
            visibleCount = Math.max(ADMIN_PAGE_SIZE, visibleCount - ADMIN_PAGE_SIZE);
            paintCategories(categories);
        });
    }

    paintCategories(currentCategories);

    function applyCategoryFilters() {
        const query = normalizeAdminSearch(searchInput.value);
        visibleCount = ADMIN_PAGE_SIZE;

        if (!query) {
            currentCategories = data;
            paintCategories(currentCategories);
            return;
        }
        currentCategories = data.filter(cat =>
            normalizeAdminSearch(`${cat.id} ${cat.nombre}`).includes(query)
        );
        paintCategories(currentCategories);
    }

    searchInput.addEventListener("input", applyCategoryFilters);

}

function renderProductsTable(tableContainer, data) {
    let visibleCount = ADMIN_PAGE_SIZE;
    let currentProducts = data;

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
                <select id="admin-category-filter" class="admin-form-select">
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
    initAdminFormSelectDropdowns();

    function paintProducts(products) {
        const visibleProducts = products.slice(0, visibleCount);

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
                ${visibleProducts.map(product => `
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
        ${renderListPaginationControls(visibleProducts.length, products.length, "productos")}
        `;

        tableRegion.querySelector("[data-load-more]")?.addEventListener("click", function () {
            visibleCount += ADMIN_PAGE_SIZE;
            paintProducts(products);
        });
        tableRegion.querySelector("[data-load-less]")?.addEventListener("click", function () {
            visibleCount = Math.max(ADMIN_PAGE_SIZE, visibleCount - ADMIN_PAGE_SIZE);
            paintProducts(products);
        });
    }

    paintProducts(currentProducts);

    function applyProductFilters() {
        const query = normalizeAdminSearch(searchInput.value);
        const selectedCategory = normalizeAdminSearch(categoryFilter.value);
        visibleCount = ADMIN_PAGE_SIZE;

        if (!query && !selectedCategory) {
            currentProducts = data;
            paintProducts(currentProducts);
            return;
        }

        currentProducts = data.filter(product => {
            const categories = product.categorias ? product.categorias.map(cat => cat.nombre).join(" ") : "";
            const searchableText = normalizeAdminSearch(`${product.id} ${product.nombre} ${categories}`);
            const matchesSearch = !query || searchableText.includes(query);
            const matchesCategory = !selectedCategory || (product.categorias || []).some(cat => normalizeAdminSearch(cat.nombre) === selectedCategory);

            return matchesSearch && matchesCategory;
        });

        paintProducts(currentProducts);
    }

    searchInput.addEventListener("input", applyProductFilters);
    categoryFilter.addEventListener("change", applyProductFilters);
}

function renderOrdersTable(tableContainer, data) {
    let visibleCount = ADMIN_PAGE_SIZE;
    let currentOrders = data;

    function def(value) {
        return value != null && value !== "" ? value : "-";
    }

    tableContainer.innerHTML = `
        <div class="admin-product-toolbar">
            <label class="admin-product-search" for="admin-order-search">
                <span>Buscar pedido</span>
                <input id="admin-order-search" type="search" placeholder="Buscar por cliente, email o tipo" autocomplete="off">
            </label>
            <label class="admin-product-filter" for="admin-order-type-filter">
                <span>Filtrar tipo</span>
                <select id="admin-order-type-filter" class="admin-form-select">
                    <option value="">Todos los tipos</option>
                    <option value="copistería">Copistería</option>
                    <option value="tienda">Tienda</option>
                </select>
            </label>
        </div>
        <div class="admin-orders-table-region" data-orders-table-region></div>
    `;

    const tableRegion = tableContainer.querySelector("[data-orders-table-region]");
    const searchInput = tableContainer.querySelector("#admin-order-search");
    const typeFilter = tableContainer.querySelector("#admin-order-type-filter");
    initAdminFormSelectDropdowns();

    function paintOrders(orders) {
        const visibleOrders = orders.slice(0, visibleCount);

        if (orders.length === 0) {
            tableRegion.innerHTML = '<p class="no-data">No se han encontrado pedidos con esa búsqueda.</p>';
            return;
        }

        tableRegion.innerHTML = `
            <table class="admin-table admin-orders-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Tipo</th>
                    <th>Cliente</th>
                    <th>Email</th>
                    <th>Teléfono</th>
                    <th>Estado</th>
                    <th>Fecha</th>
                    <th>Total</th>
                    <th>Trabajo</th>
                    <th>Copias</th>
                    <th>Color</th>
                    <th>Tamaño</th>
                    <th>Caras</th>
                    <th>Papel</th>
                    <th>Encuadernación</th>
                    <th>Extras</th>
                    <th>Archivo</th>
                    <th>Código recoger</th>
                    <th>Resumen productos</th>
                    <th>Usuario</th>
                </tr>
            </thead>
            <tbody>
                ${visibleOrders.map(order => `
                    <tr>
                        <td>${def(order.id)}</td>
                        <td>${def(order.tipo)}</td>
                        <td class="admin-cell-name">${def(order.cliente)}</td>
                        <td class="admin-cell-email">${def(order.email)}</td>
                        <td class="admin-cell-phone">${def(order.telefono)}</td>
                        <td><span class="admin-role-badge">${def(order.estado)}</span></td>
                        <td class="admin-date-cell">${formatAdminDate(order.fechaCreacion)}</td>
                        <td>${order.total != null ? order.total + " EUR" : "-"}</td>
                        <td>${def(order.trabajo)}</td>
                        <td>${def(order.copias)}</td>
                        <td>${def(order.color)}</td>
                        <td>${def(order.tamano)}</td>
                        <td>${def(order.caras)}</td>
                        <td>${def(order.papel)}</td>
                        <td>${def(order.encuadernacion)}</td>
                        <td>${def(order.extras)}</td>
                        <td>${def(order.rutaArchivo)}</td>
                        <td>${def(order.codigoRecoger)}</td>
                        <td>${def(order.resumenProductos)}</td>
                        <td>${def(order.usuarioNombre)}</td>
                    </tr>
                `).join("")}
            </tbody>
        </table>
        ${renderListPaginationControls(visibleOrders.length, orders.length, "pedidos")}
        `;

        tableRegion.querySelector("[data-load-more]")?.addEventListener("click", function () {
            visibleCount += ADMIN_PAGE_SIZE;
            paintOrders(orders);
        });
        tableRegion.querySelector("[data-load-less]")?.addEventListener("click", function () {
            visibleCount = Math.max(ADMIN_PAGE_SIZE, visibleCount - ADMIN_PAGE_SIZE);
            paintOrders(orders);
        });
    }

    paintOrders(currentOrders);

    function applyOrderFilters() {
        const query = normalizeAdminSearch(searchInput.value);
        const selectedType = normalizeAdminSearch(typeFilter.value);
        visibleCount = ADMIN_PAGE_SIZE;

        if (!query && !selectedType) {
            currentOrders = data;
            paintOrders(currentOrders);
            return;
        }

        currentOrders = data.filter(order => {
            const searchableText = normalizeAdminSearch(`${order.id} ${order.cliente} ${order.email} ${order.telefono} ${order.tipo}`);
            const matchesSearch = !query || searchableText.includes(query);
            const matchesType = !selectedType || normalizeAdminSearch(order.tipo) === selectedType;

            return matchesSearch && matchesType;
        });

        paintOrders(currentOrders);
    }

    searchInput.addEventListener("input", applyOrderFilters);
    typeFilter.addEventListener("change", applyOrderFilters);
}

function normalizeAdminSearch(value) {
    return String(value || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}

function formatRoleName(role) {
    if (role === "ROLE_ADMIN") {
        return "Admin";
    }

    if (role === "ROLE_WORKER") {
        return "Trabajador";
    }

    return "Usuario";
}

function renderListPaginationControls(visibleCount, totalCount, itemLabel) {
    if (visibleCount >= totalCount && visibleCount <= ADMIN_PAGE_SIZE) {
        return "";
    }

    return `
        <div class="admin-load-more">
            ${visibleCount > ADMIN_PAGE_SIZE ? `
                <button class="button button-outline admin-load-more-button admin-load-less-button" type="button" data-load-less>
                    Ver menos
                </button>
            ` : ""}
            ${visibleCount < totalCount ? `
                <button class="button button-outline admin-load-more-button" type="button" data-load-more>
                    Ver más ${itemLabel}
                </button>
            ` : ""}
            <span>${visibleCount} de ${totalCount}</span>
        </div>
    `;
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

    initAdminFormSelectDropdowns();
});

function initAdminFormSelectDropdowns() {
    document.querySelectorAll(".admin-form-select").forEach(select => {
        if (select.dataset.customSelectReady === "true") {
            return;
        }

        select.dataset.customSelectReady = "true";
        select.classList.add("admin-form-select-native");

        const wrapper = document.createElement("div");
        wrapper.className = "admin-form-select-dropdown";
        wrapper.dataset.formSelectDropdown = "";

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "admin-form-select-trigger";
        trigger.setAttribute("aria-haspopup", "listbox");
        trigger.setAttribute("aria-expanded", "false");
        trigger.innerHTML = `
            <span data-form-select-label></span>
            <span class="admin-role-trigger-icon" aria-hidden="true"></span>
        `;

        const menu = document.createElement("div");
        menu.className = "admin-form-select-menu";
        menu.setAttribute("role", "listbox");

        Array.from(select.options).forEach(option => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "admin-form-select-option";
            item.dataset.optionValue = option.value;
            item.setAttribute("role", "option");
            item.textContent = option.textContent;

            item.addEventListener("click", function () {
                select.value = option.value;
                select.dispatchEvent(new Event("change", { bubbles: true }));
                syncAdminFormSelect(select);
                closeAdminFormSelectDropdown(wrapper);
            });

            menu.appendChild(item);
        });

        select.parentNode.insertBefore(wrapper, select);
        wrapper.appendChild(select);
        wrapper.appendChild(trigger);
        wrapper.appendChild(menu);

        trigger.addEventListener("click", function () {
            const isOpen = wrapper.classList.toggle("is-open");
            trigger.setAttribute("aria-expanded", String(isOpen));
            closeOtherAdminFormSelectDropdowns(wrapper);
        });

        select.addEventListener("change", function () {
            syncAdminFormSelect(select);
        });

        syncAdminFormSelect(select);
    });
}

function syncAdminFormSelect(select) {
    const wrapper = select.closest("[data-form-select-dropdown]");

    if (!wrapper) {
        return;
    }

    const selectedOption = select.options[select.selectedIndex];
    const label = wrapper.querySelector("[data-form-select-label]");
    const options = wrapper.querySelectorAll(".admin-form-select-option");

    if (label && selectedOption) {
        label.textContent = selectedOption.textContent;
        label.classList.toggle("is-placeholder", !selectedOption.value);
    }

    options.forEach(option => {
        const isSelected = option.dataset.optionValue === select.value;
        option.classList.toggle("is-selected", isSelected);
        option.setAttribute("aria-selected", String(isSelected));
    });
}

function closeAdminFormSelectDropdown(wrapper) {
    wrapper.classList.remove("is-open");
    wrapper.querySelector(".admin-form-select-trigger")?.setAttribute("aria-expanded", "false");
}

function closeOtherAdminFormSelectDropdowns(activeWrapper) {
    document.querySelectorAll("[data-form-select-dropdown].is-open").forEach(wrapper => {
        if (wrapper !== activeWrapper) {
            closeAdminFormSelectDropdown(wrapper);
        }
    });
}

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
    if (!event.target.closest("[data-form-select-dropdown]")) {
        closeOtherAdminFormSelectDropdowns(null);
    }

    if (event.target.closest("[data-role-dropdown]")) {
        return;
    }

    if (event.target.matches(".admin-delete-btn[data-user-id]")) {
        event.preventDefault();
        const userId = event.target.dataset.userId;
        const userName = (event.target.dataset.userName || "este usuario").trim();
        openUserDeleteModal(userId, userName || "este usuario");
        return;
    }

    // Delete product handler - solo para productos
    if (event.target.matches(".admin-delete-btn[data-product-id]")) {
        event.preventDefault();
        const productId = event.target.dataset.productId;
        const productName = event.target.closest("tr").querySelector("td:nth-child(2)").textContent.trim();
        openCatalogDeleteModal("product", productId, productName);
        return;
    }

    if (event.target.matches(".admin-delete-btn[data-category-id]")) {
        event.preventDefault();
        const catId = event.target.dataset.categoryId;
        const catName = event.target.dataset.categoryName || "esta categoria";
        openCatalogDeleteModal("category", catId, catName);
        return;
    }

    // New product button
    if (event.target.matches(".admin-new-product-btn")) {
        window.location.href = '/admin/crear-producto';
    }

    closeOtherRoleDropdowns(null);
});

function openUserDeleteModal(userId, userName) {
    const modal = document.querySelector("[data-user-delete-modal]");
    const message = document.querySelector("[data-user-delete-message]");
    const acceptButton = document.querySelector("[data-user-delete-accept]");
    const cancelButtons = document.querySelectorAll("[data-user-delete-cancel]");

    if (!modal || !message || !acceptButton) {
        return;
    }

    message.textContent = `Vas a eliminar a ${userName}. Esta accion no se puede deshacer.`;
    acceptButton.dataset.userId = userId;
    modal.hidden = false;
    document.body.classList.add("admin-modal-open");
    acceptButton.focus();

    cancelButtons.forEach(button => {
        button.onclick = function () {
            closeUserDeleteModal();
        };
    });

    acceptButton.onclick = function () {
        deleteUser(this.dataset.userId);
    };
}

function closeUserDeleteModal() {
    const modal = document.querySelector("[data-user-delete-modal]");
    const acceptButton = document.querySelector("[data-user-delete-accept]");

    if (modal) {
        modal.hidden = true;
    }

    if (acceptButton) {
        delete acceptButton.dataset.userId;
    }

    document.body.classList.remove("admin-modal-open");
}

function openCatalogDeleteModal(type, id, name) {
    const modal = document.querySelector("[data-catalog-delete-modal]");
    const title = document.querySelector("[data-catalog-delete-title]");
    const message = document.querySelector("[data-catalog-delete-message]");
    const acceptButton = document.querySelector("[data-catalog-delete-accept]");
    const cancelButtons = document.querySelectorAll("[data-catalog-delete-cancel]");

    if (!modal || !title || !message || !acceptButton) {
        return;
    }

    const isProduct = type === "product";
    const itemLabel = isProduct ? "producto" : "categoría";
    title.textContent = isProduct ? "Eliminar producto" : "Eliminar categoría";
    message.textContent = `Vas a eliminar ${itemLabel} "${name}". Esta accion no se puede deshacer.`;
    acceptButton.textContent = isProduct ? "Eliminar producto" : "Eliminar categoría";
    acceptButton.dataset.deleteType = type;
    acceptButton.dataset.deleteId = id;

    modal.hidden = false;
    document.body.classList.add("admin-modal-open");
    acceptButton.focus();

    cancelButtons.forEach(button => {
        button.onclick = function () {
            closeCatalogDeleteModal();
        };
    });

    acceptButton.onclick = function () {
        if (this.dataset.deleteType === "product") {
            deleteProduct(this.dataset.deleteId);
            return;
        }

        deleteCategoria(this.dataset.deleteId);
    };
}

function closeCatalogDeleteModal() {
    const modal = document.querySelector("[data-catalog-delete-modal]");
    const acceptButton = document.querySelector("[data-catalog-delete-accept]");

    if (modal) {
        modal.hidden = true;
    }

    if (acceptButton) {
        delete acceptButton.dataset.deleteType;
        delete acceptButton.dataset.deleteId;
    }

    document.body.classList.remove("admin-modal-open");
}

function openAdminMessageModal(title, message, type = "info") {
    const modal = document.querySelector("[data-admin-message-modal]");
    const titleElement = document.querySelector("[data-admin-message-title]");
    const messageElement = document.querySelector("[data-admin-message-text]");
    const iconElement = document.querySelector("[data-admin-message-icon]");
    const closeButtons = document.querySelectorAll("[data-admin-message-close]");

    if (!modal || !titleElement || !messageElement || !iconElement) {
        return;
    }

    titleElement.textContent = title;
    messageElement.textContent = message;
    iconElement.textContent = type === "error" ? "!" : "i";
    modal.classList.toggle("admin-message-error", type === "error");
    modal.hidden = false;
    document.body.classList.add("admin-modal-open");

    closeButtons.forEach(button => {
        button.onclick = function () {
            closeAdminMessageModal();
        };
    });
}

function closeAdminMessageModal() {
    const modal = document.querySelector("[data-admin-message-modal]");

    if (modal) {
        modal.hidden = true;
        modal.classList.remove("admin-message-error");
    }

    document.body.classList.remove("admin-modal-open");
}

function deleteUser(userId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";

    fetch(`/admin/delete-user/${userId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        closeUserDeleteModal();

        if (data.success === 'true') {
            const activeTabButton = document.querySelector('.admin-buttons .active');
            switchTab(activeTabButton, 'users');
            return;
        }

        openAdminMessageModal("No se pudo eliminar", data.message || "No se ha podido eliminar el usuario.", "error");
    })
    .catch(error => {
        closeUserDeleteModal();
        console.error('Error:', error);
        openAdminMessageModal("Error al eliminar", "Error al eliminar usuario.", "error");
    });
}

function deleteCategoria(categoryId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
    fetch(`/admin/delete-categoria/${categoryId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(data => {
        closeCatalogDeleteModal();

        if (data.success === 'true') {
            const activeTabButton = document.querySelector('.admin-buttons .active');
            switchTab(activeTabButton, 'categories');
            openAdminMessageModal("Categoría eliminada", data.message || "Categoría eliminada correctamente.");
        } else {
            openAdminMessageModal("No se pudo eliminar", data.message || "Error desconocido", "error");
        }
    })
    .catch(error => {
        closeCatalogDeleteModal();
        console.error('Error:', error);
        openAdminMessageModal("Error al eliminar", "Error al eliminar: " + error.message, "error");
    });
}

function deleteProduct(productId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]') ? document.querySelector('meta[name="_csrf"]').getAttribute("content") : "";
    fetch(`/admin/delete-producto/${productId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`HTTP ${response.status}: ${text}`);
            });
        }
        return response.json();
    })
    .then(data => {
        closeCatalogDeleteModal();

        if (data.success === 'true') {
            const activeTabButton = document.querySelector('.admin-buttons .active');
            const tabName = activeTabButton ? activeTabButton.dataset.tab || 'products' : 'products';
            if (activeTabButton) switchTab(activeTabButton, tabName);
            openAdminMessageModal("Producto eliminado", data.message || "Producto eliminado correctamente.");
        } else {
            openAdminMessageModal("No se pudo eliminar", data.message || "Error desconocido", "error");
        }
    })
    .catch(error => {
        closeCatalogDeleteModal();
        console.error('Error:', error);
        openAdminMessageModal("Error al eliminar", "Error al eliminar: " + error.message, "error");
    });
}
