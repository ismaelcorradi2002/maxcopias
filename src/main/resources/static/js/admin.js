const ADMIN_PAGE_SIZE = 5;

function switchTab(button, tab) {
    document.querySelectorAll(".admin-buttons button").forEach(btn => btn.classList.remove("active"));
    button.classList.add("active");

    const tableContainer = document.querySelector(".table-container");
    tableContainer.classList.toggle("is-users-tab", tab === "users");
    tableContainer.innerHTML = '<div class="admin-loading">Cargando información del panel...</div>';

    const isOrdersTab = tab === "orders" || tab === "orders-copy" || tab === "orders-store";
    const apiEndpoint = tab === "users"
        ? "/admin/api/users"
        : tab === "products"
            ? "/admin/api/products"
            : isOrdersTab
                ? "/admin/api/pedidos"
                : "/admin/api/categorias";

    fetch(apiEndpoint)
        .then(response => response.json())
        .then(data => {
            if (!Array.isArray(data)) {
                console.error("Respuesta inesperada de la API:", data);
                tableContainer.innerHTML = '<p class="no-data">No hemos podido cargar esta sección. Inténtalo de nuevo en unos segundos.</p>';
                return;
            }

            if (data.length === 0) {
                tableContainer.innerHTML = `<p class="no-data">${tab === "users" ? "Todavía no hay usuarios registrados." : tab === "products" ? "Todavía no hay productos creados." : tab === "orders" ? "Todavía no hay pedidos registrados." : "Todavía no hay categorías creadas."}</p>`;
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
            if (isOrdersTab) {
                renderOrdersTableWorkerStyle(tableContainer, data, tab);
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
    const totalAdmins = data.filter(user => user.rol === "ROLE_ADMIN").length;

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
                    <th class="admin-col-id">ID</th>
                    <th class="admin-col-name">Nombre</th>
                    <th class="admin-col-email">Email</th>
                    <th class="admin-col-phone">Teléfono</th>
                    <th class="admin-col-role">Rol</th>
                    <th class="admin-col-date">Creado</th>
                    <th class="admin-col-actions">Cambiar rol</th>
                    <th class="admin-col-actions">Eliminar</th>
                </tr>
            </thead>
            <tbody>
                ${visibleUsers.map(user => {
                    const isLastAdmin = totalAdmins === 1 && user.rol === "ROLE_ADMIN";
                    return `
                    <tr>
                        <td class="admin-cell-id" data-label="ID">${user.id}</td>
                        <td class="admin-cell-name" data-label="Nombre" title="${user.firstName || ""} ${user.lastName || ""}">
                            ${user.firstName || ""} ${user.lastName || ""}
                        </td>
                        <td class="admin-cell-email" data-label="Email" title="${user.email}">
                            ${user.email}
                        </td>
                        <td class="admin-cell-phone" data-label="Teléfono">${user.phone || ""}</td>
                        <td class="admin-role-cell admin-col-role" data-label="Rol"><span class="admin-role-badge">${formatRoleName(user.rol)}</span></td>
                        <td class="admin-date-cell" data-label="Creado">${formatAdminDate(user.createdAt)}</td>
                        <td class="admin-action-cell" data-label="Cambiar rol">
                            ${isLastAdmin ? `
                            <div class="admin-role-lock" title="Debe existir al menos un administrador activo">
                                <span class="admin-role-badge">Admin</span>
                                <small>Ultimo admin</small>
                            </div>
                            ` : `
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
                            `}
                        </td>
                        <td class="admin-action-cell" data-label="Eliminar">
                            <button class="button button-small button-outline admin-delete-btn" type="button" data-user-id="${user.id}" data-user-name="${user.firstName || ""} ${user.lastName || ""}" title="Eliminar usuario">
                                Eliminar
                            </button>
                        </td>
                    </tr>
                `;}).join("")}
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
                        <th class="admin-col-id">ID</th>
                        <th class="admin-col-name">Nombre</th>
                        <th class="admin-col-description">Descripción</th>
                        <th class="admin-col-count"># Productos</th>
                        <th class="admin-col-actions">Eliminar</th>
                    </tr>
                </thead>
                <tbody>
                    ${visibleCategories.map(category => `
                        <tr>
                            <td class="admin-col-id" data-label="ID">${category.id}</td>
                            <td class="admin-col-name" data-label="Nombre">${category.nombre}</td>
                            <td class="admin-category-description admin-col-description" data-label="Descripción" title="${category.descripcion || ''}">${category.descripcion || '-'}</td>
                            <td class="admin-col-count" data-label="Productos">${category.productos ? category.productos.length : 0}</td>
                            <td class="admin-action-cell admin-col-actions" data-label="Eliminar">
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
                <input id="admin-product-search" type="search" placeholder="Buscar por ID, nombre o categoría" autocomplete="off">
            </label>
            <label class="admin-product-filter" for="admin-category-filter">
                <span>Filtrar categoría</span>
                <select id="admin-category-filter" class="admin-form-select">
                    <option value="">Todas las categorías</option>
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
                    <th class="admin-col-id">ID</th>
                    <th class="admin-col-name">Nombre</th>
                    <th class="admin-col-price">Precio</th>
                    <th class="admin-col-count">Stock</th>
                    <th class="admin-col-description">Categorías</th>
                    <th class="admin-col-actions">Acción</th>
                    <th class="admin-col-actions">Eliminar</th>
                </tr>
            </thead>
            <tbody>
                ${visibleProducts.map(product => `
                    <tr>
                        <td class="admin-col-id" data-label="ID">${product.id}</td>
                        <td class="admin-col-name" data-label="Nombre">${product.nombre}</td>
                        <td class="admin-col-price" data-label="Precio">${product.precio}</td>
                        <td class="admin-col-count" data-label="Stock">${product.stock}</td>
                        <td class="admin-product-categories admin-col-description" data-label="Categorías">${product.categorias ? product.categorias.map(cat => cat.nombre).join(", ") : ""}</td>
                        <td class="admin-action-cell admin-col-actions" data-label="Editar">
                            <button class="button button-small button-outline" type="button" onclick="window.location.href='/editarstock/${product.id}'">
                                Editar
                            </button>
                        </td>
                        <td class="admin-action-cell admin-col-actions" data-label="Eliminar">
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

    function renderOrderFileCell(order) {
        if (!order.archivoDescargaUrl) {
            return "Sin archivo";
        }

        const archivoNombre = def(order.archivoNombre);
        const verUrl = order.archivoDescargaUrl;
        const isRemoteUrl = /^https?:\/\//i.test(order.archivoDescargaUrl);
        const descargarUrl = isRemoteUrl ? order.archivoDescargaUrl : order.archivoDescargaUrl + "?download=true";

        return `
            <div class="admin-order-file-cell">
                <strong>${archivoNombre}</strong>
                <div>
                    <a href="${verUrl}" target="_blank" rel="noopener noreferrer">Ver archivo</a>
                    <a href="${descargarUrl}">Descargar</a>
                </div>
            </div>
        `;
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
                    <th class="admin-col-id">ID</th>
                    <th class="admin-col-short">Tipo</th>
                    <th class="admin-col-name">Cliente</th>
                    <th class="admin-col-email">Email</th>
                    <th class="admin-col-phone">Teléfono</th>
                    <th class="admin-col-status">Estado</th>
                    <th class="admin-col-date">Fecha</th>
                    <th class="admin-col-price">Total</th>
                    <th class="admin-col-job">Trabajo</th>
                    <th class="admin-col-count">Copias</th>
                    <th class="admin-col-short">Color</th>
                    <th class="admin-col-short">Tamaño</th>
                    <th class="admin-col-short">Caras</th>
                    <th class="admin-col-short">Papel</th>
                    <th class="admin-col-short">Encuadernación</th>
                    <th class="admin-col-description">Extras</th>
                    <th class="admin-col-file">Archivo</th>
                    <th class="admin-col-code">Código recoger</th>
                    <th class="admin-col-summary">Resumen productos</th>
                    <th class="admin-col-name">Usuario</th>
                </tr>
            </thead>
            <tbody>
                ${visibleOrders.map(order => `
                    <tr>
                        <td class="admin-col-id">${def(order.id)}</td>
                        <td class="admin-col-short">${def(order.tipo)}</td>
                        <td class="admin-cell-name">${def(order.cliente)}</td>
                        <td class="admin-cell-email">${def(order.email)}</td>
                        <td class="admin-cell-phone">${def(order.telefono)}</td>
                        <td class="admin-col-status"><span class="admin-role-badge">${def(order.estado)}</span></td>
                        <td class="admin-date-cell">${formatAdminDate(order.fechaCreacion)}</td>
                        <td class="admin-col-price">${order.total != null ? order.total + " EUR" : "-"}</td>
                        <td class="admin-col-job">${def(order.trabajo)}</td>
                        <td class="admin-col-count">${def(order.copias)}</td>
                        <td class="admin-col-short">${def(order.color)}</td>
                        <td class="admin-col-short">${def(order.tamano)}</td>
                        <td class="admin-col-short">${def(order.caras)}</td>
                        <td class="admin-col-short">${def(order.papel)}</td>
                        <td class="admin-col-short">${def(order.encuadernacion)}</td>
                        <td class="admin-col-description">${def(order.extras)}</td>
                        <td class="admin-col-file">${renderOrderFileCell(order)}</td>
                        <td class="admin-col-code">${def(order.codigoRecoger)}</td>
                        <td class="admin-col-summary">${def(order.resumenProductos)}</td>
                        <td class="admin-col-name">${def(order.usuarioNombre)}</td>
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

function renderOrdersTableWorkerStyle(tableContainer, data, activeOrdersTab) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
    const state = {
        copisteriaVisibleCount: ADMIN_PAGE_SIZE,
        tiendaVisibleCount: ADMIN_PAGE_SIZE
    };

    function def(value) {
        return value != null && value !== "" ? value : "-";
    }

    function stateLabel(value) {
        const labels = {
            PENDIENTE: "Pendiente",
            EN_PREPARACION: "En preparación",
            LISTO_PARA_RECOGER: "Listo para recoger",
            ENTREGADO: "Entregado",
            CANCELADO: "Cancelado"
        };
        return labels[value] || def(value);
    }

    function stateBadgeClass(value) {
        if (value === "ENTREGADO") return "worker-state-badge is-done";
        if (value === "LISTO_PARA_RECOGER") return "worker-state-badge is-ready";
        if (value === "CANCELADO") return "worker-state-badge is-cancelled";
        return "worker-state-badge";
    }

    function formatOrderStatus(status) {
        const labels = {
            PENDIENTE: "Pendiente",
            EN_PREPARACION: "En preparación",
            LISTO_PARA_RECOGER: "Listo para recoger",
            ENTREGADO: "Entregado",
            CANCELADO: "Cancelado"
        };

        return labels[status] || def(status).replaceAll("_", " ");
    }

    function getOrderStateOptions(type, selectedStatus) {
        const options = ["PENDIENTE", "EN_PREPARACION", "LISTO_PARA_RECOGER", "ENTREGADO", "CANCELADO"];

        return options.map(status => `
            <option value="${status}" ${status === selectedStatus ? "selected" : ""}>${formatOrderStatus(status)}</option>
        `).join("");
    }

    function getOrderType(order) {
        return normalizeAdminSearch(order.tipo) === "copisteria" ? "copisteria" : "tienda";
    }

    function renderOrderFileCell(order) {
        if (!order.archivoDescargaUrl) {
            return '<span class="worker-col-short">Sin archivo</span>';
        }

        const archivoNombre = def(order.archivoNombre);
        const verUrl = order.archivoDescargaUrl;
        return `
            <div class="worker-file-actions" title="${archivoNombre}">
                <a class="button button-secondary-dark worker-file-button" href="${verUrl}" target="_blank" rel="noopener noreferrer">Abrir</a>
            </div>
        `;
    }

    function isUrgentCopyOrder(order) {
        return getOrderType(order) === "copisteria"
            && typeof order.extras === "string"
            && order.extras.includes("urgente=true");
    }

    function isHomeDeliveryCopyOrder(order) {
        return getOrderType(order) === "copisteria"
            && typeof order.extras === "string"
            && order.extras.includes("deliveryMethod='HOME_DELIVERY'");
    }

    function isStorePickupCopyOrder(order) {
        return getOrderType(order) === "copisteria"
            && typeof order.extras === "string"
            && order.extras.includes("deliveryMethod='STORE_PICKUP'");
    }

    function isHomeDeliveryOrder(order) {
        return getOrderType(order) === "tienda" && order.metodoEntrega === "ENVIO_DOMICILIO";
    }

    function isStorePickupOrder(order) {
        return getOrderType(order) === "tienda" && order.metodoEntrega === "RECOGIDA_TIENDA";
    }

    function formatCompactOrderTotal(order) {
        return order.total != null ? `${order.total} EUR` : "-";
    }

    function renderOrderCodeCell(order) {
        const isCopisteria = getOrderType(order) === "copisteria";
        const code = def(order.codigoRecoger);
        const badgeClass = isCopisteria
            ? "worker-payment-badge is-neutral"
            : order.pagado
                ? "worker-payment-badge is-paid"
                : "worker-payment-badge is-unpaid";
        const badgeLabel = isCopisteria ? "Copistería" : order.pagado ? "Pagado" : "No pagado";

        return `
            <div class="worker-order-code">
                <span class="worker-cell-title">${code}</span>
                <span class="${badgeClass}">${badgeLabel}</span>
            </div>
            <small>${formatAdminDate(order.fechaCreacion)}</small>
        `;
    }

    function renderCustomerCell(order) {
        return `
            <div class="worker-customer-block">
                <span class="worker-cell-title">${def(order.cliente)}</span>
            </div>
            <small>${def(order.telefono)}</small>
            ${renderPriorityFlags(order)}
        `;
    }

    function renderWorkCell(order) {
        if (getOrderType(order) === "copisteria") {
            return `
                <span class="worker-cell-title">${def(order.trabajo)}</span>
                <small>${order.copias != null ? order.copias : "-"} copia(s)${order.tamano ? ` · ${order.tamano}` : ""}</small>
            `;
        }

        return `<span class="worker-order-summary">${def(order.resumenProductos)}</span>`;
    }

    function renderDeliveryCell(order) {
        if (getOrderType(order) === "copisteria") {
            return `
                <div class="worker-order-flags">
                    ${isHomeDeliveryCopyOrder(order)
                        ? '<span class="worker-order-flag is-delivery">A domicilio</span>'
                        : '<span class="worker-order-flag is-pickup">Recogida en tienda</span>'}
                </div>
            `;
        }

        return `
            <div class="worker-order-flags">
                ${isHomeDeliveryOrder(order)
                    ? '<span class="worker-order-flag is-delivery">Envío a domicilio</span>'
                    : isStorePickupOrder(order)
                        ? '<span class="worker-order-flag is-pickup">Recogida en tienda</span>'
                        : ""}
            </div>
        `;
    }

    function renderPriorityFlags(order) {
        const flags = [];

        if (isUrgentCopyOrder(order)) {
            flags.push('<span class="worker-order-flag is-urgent">Urgente</span>');
        }

        if (isHomeDeliveryCopyOrder(order)) {
            flags.push('<span class="worker-order-flag is-delivery">A domicilio</span>');
        }

        if (isStorePickupCopyOrder(order)) {
            flags.push('<span class="worker-order-flag is-pickup">Recogida</span>');
        }

        if (isHomeDeliveryOrder(order)) {
            flags.push('<span class="worker-order-flag is-delivery">A domicilio</span>');
        }

        return flags.length ? `<div class="worker-order-flags">${flags.join("")}</div>` : "";
    }

    tableContainer.innerHTML = '<div class="admin-orders-board" data-orders-board></div>';
    const board = tableContainer.querySelector("[data-orders-board]");

    function filterOrdersByPanel(type) {
        const searchInput = board.querySelector(`[data-admin-order-search="${type}"]`);
        const statusInput = board.querySelector(`[data-admin-order-status="${type}"]`);
        const dateInput = board.querySelector(`[data-admin-order-date="${type}"]`);
        const visibilityInput = board.querySelector(`[data-admin-order-visibility="${type}"]`);
        const query = normalizeAdminSearch(searchInput?.value || "");
        const selectedStatus = statusInput?.value || "";
        const selectedDate = dateInput?.value || "";
        const selectedVisibility = visibilityInput?.value || "active";

        return data.filter(order => {
            if (getOrderType(order) !== type) {
                return false;
            }

            const searchableText = type === "copisteria"
                ? normalizeAdminSearch(`${order.id} ${order.cliente} ${order.email} ${order.telefono} ${order.codigoRecoger || ""} ${order.trabajo || ""}`)
                : normalizeAdminSearch(`${order.id} ${order.cliente} ${order.email} ${order.telefono} ${order.resumenProductos || ""}`);

            const matchesSearch = !query || searchableText.includes(query);
            const matchesStatus = !selectedStatus || order.estado === selectedStatus;
            const orderDate = order.fechaCreacion ? new Date(order.fechaCreacion).toISOString().slice(0, 10) : "";
            const matchesDate = !selectedDate || orderDate === selectedDate;
            const matchesVisibility = selectedVisibility === "all"
                || (selectedVisibility === "deleted" && order.eliminado)
                || (selectedVisibility === "active" && !order.eliminado);

            return matchesSearch && matchesStatus && matchesDate && matchesVisibility;
        });
    }

    function buildPanel(type) {
        const filteredOrders = filterOrdersByPanel(type);
        const visibleCount = type === "copisteria" ? state.copisteriaVisibleCount : state.tiendaVisibleCount;
        const visibleOrders = filteredOrders.slice(0, visibleCount);
        const isCopisteria = type === "copisteria";

        return `
            <section class="worker-section admin-order-panel">
                <div class="worker-section-header">
                    <div>
                        <h2>${isCopisteria ? "Pedidos de copistería" : "Pedidos de papelería"}</h2>
                        <p>${isCopisteria
                            ? "Revisa archivos, códigos de recogida y actualiza el estado de cada encargo."
                            : "Consulta pedidos de productos y controla su avance hasta la entrega."}</p>
                    </div>
                </div>
                <div class="worker-filters">
                    <label>
                        <span>Buscar pedido</span>
                        <input type="search" data-admin-order-search="${type}" placeholder="${isCopisteria ? "Cliente, email, código o ID" : "Cliente, email, productos o ID"}">
                    </label>
                    <label>
                        <span>Estado</span>
                        <select data-admin-order-status="${type}" class="worker-select select-modern">
                            <option value="">Todos los estados</option>
                            ${getOrderStateOptions(type, "")}
                        </select>
                    </label>
                    <label>
                        <span>Fecha</span>
                        <input type="date" data-admin-order-date="${type}">
                    </label>
                    <label>
                        <span>Visibilidad</span>
                        <select data-admin-order-visibility="${type}" class="worker-select select-modern">
                            <option value="active">Activos</option>
                            <option value="deleted">Eliminados</option>
                            <option value="all">Todos</option>
                        </select>
                    </label>
                </div>
                <div class="worker-table-wrap">
                    <table class="worker-table worker-table-compact" data-worker-table="${type}">
                        <thead>
                            <tr>
                                <th>Codigo</th>
                                <th>Cliente</th>
                                <th class="worker-col-job">${isCopisteria ? "Trabajo" : "Productos"}</th>
                                <th class="worker-col-short">Entrega</th>
                                <th class="worker-col-price">Total</th>
                                <th class="worker-col-status">Estado</th>
                                <th class="worker-col-actions">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${visibleOrders.map(order => `
                                <tr class="${[
                                    order.eliminado ? "admin-order-row-deleted" : "",
                                    isUrgentCopyOrder(order) || isHomeDeliveryCopyOrder(order) || isHomeDeliveryOrder(order)
                                        ? "worker-order-row-priority"
                                        : "worker-order-row-standard",
                                    "worker-order-row-card"
                                ].filter(Boolean).join(" ")}">
                                    <td data-label="Codigo">
                                        ${renderOrderCodeCell(order)}
                                    </td>
                                    <td data-label="Cliente">
                                        ${renderCustomerCell(order)}
                                    </td>
                                    <td class="worker-col-job" data-label="${isCopisteria ? "Trabajo" : "Productos"}">
                                        ${renderWorkCell(order)}
                                    </td>
                                    <td data-label="Entrega">
                                        ${renderDeliveryCell(order)}
                                    </td>
                                    <td class="worker-col-price" data-label="Total">
                                        <span class="worker-price-strong">${formatCompactOrderTotal(order)}</span>
                                    </td>
                                    <td class="worker-col-status" data-label="Estado">
                                        <div class="worker-status-stack">
                                            <form action="${isCopisteria ? `/admin/pedidos/copisteria/${order.id}/estado` : `/admin/pedidos/tienda/${order.id}/estado`}" method="post" class="worker-status-form">
                                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                                <select name="estado" class="worker-select select-modern" onchange="this.form.submit()" ${order.eliminado ? "disabled" : ""}>
                                                    ${getOrderStateOptions(type, order.estado)}
                                                </select>
                                            </form>
                                        </div>
                                    </td>
                                    <td class="worker-col-actions" data-label="Acciones">
                                        <div class="worker-actions-cell order-actions">
                                            <a class="order-action-btn order-action-view" href="/admin/pedidos/${order.id}?tipo=${type}" title="Ver detalle" aria-label="Ver detalle">
                                                <span aria-hidden="true">&#128065;</span>
                                                <span class="worker-sr-only">Ver detalle</span>
                                            </a>
                                            ${order.eliminado
                                                ? `<span class="admin-role-badge">Eliminado</span>`
                                                : `
                                                    <button
                                                        type="button"
                                                        class="order-action-btn order-action-delete admin-delete-btn"
                                                        title="Eliminar pedido"
                                                        aria-label="Eliminar pedido"
                                                        data-order-id="${order.id}"
                                                        data-order-type="${type}"
                                                        data-order-name="${isCopisteria ? `pedido de copistería ${def(order.codigoRecoger)}` : `pedido de tienda #${def(order.id)}`}"
                                                    >
                                                        <span aria-hidden="true">&#128465;</span>
                                                        <span class="worker-sr-only">Eliminar</span>
                                                    </button>
                                                `}
                                        </div>
                                    </td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                    ${filteredOrders.length === 0 ? `<p class="worker-empty">No hay pedidos de ${isCopisteria ? "copistería" : "papelería"} con esos filtros.</p>` : ""}
                    ${renderAdminOrderPaginationControls(type, visibleOrders.length, filteredOrders.length, "pedidos")}
                </div>
            </section>
        `;
    }

    function bindPanelEvents(type) {
        const searchInput = board.querySelector(`[data-admin-order-search="${type}"]`);
        const statusInput = board.querySelector(`[data-admin-order-status="${type}"]`);
        const dateInput = board.querySelector(`[data-admin-order-date="${type}"]`);
        const visibilityInput = board.querySelector(`[data-admin-order-visibility="${type}"]`);
        const loadMoreButton = board.querySelector(`[data-admin-order-load-more="${type}"]`);
        const loadLessButton = board.querySelector(`[data-admin-order-load-less="${type}"]`);

        [searchInput, statusInput, dateInput, visibilityInput].forEach(input => {
            if (!input) {
                return;
            }

            input.addEventListener("input", function () {
                state[`${type}VisibleCount`] = ADMIN_PAGE_SIZE;
                paintOrders();
            });
            input.addEventListener("change", function () {
                state[`${type}VisibleCount`] = ADMIN_PAGE_SIZE;
                paintOrders();
            });
        });

        loadMoreButton?.addEventListener("click", function () {
            state[`${type}VisibleCount`] += ADMIN_PAGE_SIZE;
            paintOrders();
        });

        loadLessButton?.addEventListener("click", function () {
            state[`${type}VisibleCount`] = Math.max(ADMIN_PAGE_SIZE, state[`${type}VisibleCount`] - ADMIN_PAGE_SIZE);
            paintOrders();
        });
    }

    function paintOrders() {
        const panels = activeOrdersTab === "orders-copy"
            ? [buildPanel("copisteria")]
            : activeOrdersTab === "orders-store"
                ? [buildPanel("tienda")]
                : [buildPanel("copisteria"), buildPanel("tienda")];

        board.innerHTML = panels.join("");

        if (typeof initModernSelects === "function") {
            initModernSelects();
        }

        if (activeOrdersTab !== "orders-store") {
            bindPanelEvents("copisteria");
        }
        if (activeOrdersTab !== "orders-copy") {
            bindPanelEvents("tienda");
        }
        initAdminOrderDeleteButtons();
    }

    paintOrders();
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

function renderAdminOrderPaginationControls(type, visibleCount, totalCount, itemLabel) {
    if (totalCount === 0) {
        return "";
    }

    if (visibleCount >= totalCount && visibleCount <= ADMIN_PAGE_SIZE) {
        return "";
    }

    return `
        <div class="worker-load-more admin-order-load-more">
            ${visibleCount > ADMIN_PAGE_SIZE ? `
                <button class="button button-outline worker-load-more-button" type="button" data-admin-order-load-less="${type}">
                    Ver menos
                </button>
            ` : ""}
            ${visibleCount < totalCount ? `
                <button class="button button-outline worker-load-more-button" type="button" data-admin-order-load-more="${type}">
                    Ver mas ${itemLabel}
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
    const navContainer = document.querySelector(".admin-buttons.admin-panel-nav");
    const requestedTab = navContainer?.dataset.activeTab || "users";
    const activeTab = requestedTab === "orders" ? "orders-copy" : requestedTab;
    const activeButton = navContainer?.querySelector(`[data-tab="${activeTab}"]`);
    if (activeButton) {
        activeButton.classList.add("active");
        switchTab(activeButton, activeTab);
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
        message.textContent = `Vas a cambiar el rol de ${userName} a ${newRoleText}. Esta acción modificará sus permisos de acceso.`;
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

function initAdminOrderDeleteButtons() {
    document.querySelectorAll(".admin-delete-btn[data-order-id]").forEach(button => {
        button.addEventListener("click", function () {
            openOrderDeleteModal(
                this.dataset.orderType,
                this.dataset.orderId,
                this.dataset.orderName || "este pedido"
            );
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

    if (event.target.matches(".admin-delete-btn[data-order-id]")) {
        event.preventDefault();
        openOrderDeleteModal(
            event.target.dataset.orderType,
            event.target.dataset.orderId,
            event.target.dataset.orderName || "este pedido"
        );
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

    message.textContent = `Vas a eliminar a ${userName}. Esta acción no se puede deshacer.`;
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
    message.textContent = `Vas a eliminar ${itemLabel} "${name}". Esta acción no se puede deshacer.`;
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

function openOrderDeleteModal(type, id, name) {
    const modal = document.querySelector("[data-catalog-delete-modal]");
    const title = document.querySelector("[data-catalog-delete-title]");
    const message = document.querySelector("[data-catalog-delete-message]");
    const acceptButton = document.querySelector("[data-catalog-delete-accept]");
    const cancelButtons = document.querySelectorAll("[data-catalog-delete-cancel]");

    if (!modal || !title || !message || !acceptButton) {
        return;
    }

    title.textContent = "Eliminar pedido";
    message.textContent = `Vas a eliminar ${name}. Esta acción no se puede deshacer y dejará de estar disponible en el panel.`;
    acceptButton.textContent = "Eliminar pedido";
    acceptButton.dataset.orderDeleteType = type;
    acceptButton.dataset.orderDeleteId = id;

    modal.hidden = false;
    document.body.classList.add("admin-modal-open");
    acceptButton.focus();

    cancelButtons.forEach(button => {
        button.onclick = function () {
            closeOrderDeleteModal();
        };
    });

    acceptButton.onclick = function () {
        deleteOrder(this.dataset.orderDeleteType, this.dataset.orderDeleteId);
    };
}

function closeOrderDeleteModal() {
    const modal = document.querySelector("[data-catalog-delete-modal]");
    const acceptButton = document.querySelector("[data-catalog-delete-accept]");

    if (modal) {
        modal.hidden = true;
    }

    if (acceptButton) {
        delete acceptButton.dataset.orderDeleteType;
        delete acceptButton.dataset.orderDeleteId;
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

        openAdminMessageModal("No se pudo completar la eliminación", data.message || "No hemos podido eliminar el usuario.", "error");
    })
    .catch(error => {
        closeUserDeleteModal();
        console.error('Error:', error);
        openAdminMessageModal("Error al eliminar", "No hemos podido eliminar el usuario.", "error");
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
            openAdminMessageModal("Categoría eliminada", data.message || "La categoría se ha eliminado correctamente.");
        } else {
            openAdminMessageModal("No se pudo completar la eliminación", data.message || "No hemos podido eliminar este elemento.", "error");
        }
    })
    .catch(error => {
        closeCatalogDeleteModal();
        console.error('Error:', error);
        openAdminMessageModal("Error al eliminar", "No hemos podido completar la eliminación. " + error.message, "error");
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
            openAdminMessageModal("Producto eliminado", data.message || "El producto se ha eliminado correctamente.");
        } else {
            openAdminMessageModal("No se pudo completar la eliminación", data.message || "No hemos podido eliminar el producto.", "error");
        }
    })
    .catch(error => {
        closeCatalogDeleteModal();
        console.error('Error:', error);
        openAdminMessageModal("Error al eliminar", "No hemos podido eliminar el producto. " + error.message, "error");
    });
}

function deleteOrder(orderType, orderId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
    const endpoint = orderType === "copisteria"
        ? `/admin/pedidos/copisteria/${orderId}/eliminar`
        : `/admin/pedidos/tienda/${orderId}/eliminar`;

    fetch(endpoint, {
        method: "POST",
        headers: {
            "X-CSRF-TOKEN": csrfToken
        }
    })
    .then(response => {
        closeOrderDeleteModal();

        if (!response.ok) {
            throw new Error("No hemos podido eliminar el pedido.");
        }

        const activeTabButton = document.querySelector(".admin-buttons .active");
        switchTab(activeTabButton, activeTabButton?.dataset.tab || "orders-copy");
    })
    .catch(error => {
        closeOrderDeleteModal();
        console.error("Error:", error);
        openAdminMessageModal("Error al eliminar", error.message, "error");
    });
}


