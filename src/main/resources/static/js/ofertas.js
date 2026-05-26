document.addEventListener("DOMContentLoaded", function () {
    initOfferForm();
    initHomeOfferCountdown();
});

const OFFER_PRODUCT_PAGE_SIZE = 5;

function initOfferForm() {
    const form = document.querySelector("[data-offer-form]");
    if (!form) {
        return;
    }

    const typeInputs = form.querySelectorAll("[data-offer-type]");
    const productBlock = form.querySelector("[data-product-target]");
    const categoryBlock = form.querySelector("[data-category-target]");
    const productSearch = form.querySelector("[data-product-offer-search]");
    const productCategory = form.querySelector("[data-product-offer-category]");
    const productItems = form.querySelectorAll(".offer-smart-item");
    const productPagination = form.querySelector("[data-product-offer-pagination]");
    const categorySelect = form.querySelector("[data-category-offer-select]");
    const discountInput = form.querySelector("[data-discount-input]");
    const discountRange = form.querySelector("[data-discount-range]");
    const discountLabel = form.querySelector("[data-discount-label]");
    const startInput = form.querySelector("[data-offer-start]");
    const endInput = form.querySelector("[data-offer-end]");
    const imagePanel = form.querySelector("[data-offer-image-panel]");
    const imageHelp = form.querySelector("[data-offer-image-help]");
    const imageUpload = form.querySelector("[data-offer-image-upload]");
    const imageInput = form.querySelector("[data-offer-image-input]");
    const imagePreview = form.querySelector("[data-offer-image-preview]");
    const imagePlaceholder = form.querySelector("[data-offer-image-placeholder]");
    const imageRemove = form.querySelector("[data-offer-image-remove]");
    const imageRemoveWrapper = form.querySelector("[data-offer-remove-wrapper]");

    function currentType() {
        return form.querySelector("[data-offer-type]:checked")?.value || "GLOBAL";
    }

    const productState = {
        visibleCount: OFFER_PRODUCT_PAGE_SIZE
    };

    function syncTargetBlocks() {
        const type = currentType();
        productBlock.hidden = type !== "PRODUCTO";
        categoryBlock.hidden = type !== "CATEGORIA";
        syncImageBlock(type);
        if (type === "PRODUCTO") {
            productState.visibleCount = OFFER_PRODUCT_PAGE_SIZE;
            renderOfferProductList();
        }
        updateOfferPreview();
    }

    function syncImageBlock(type) {
        if (!imagePanel || !imageUpload) {
            return;
        }

        const isProductOffer = type === "PRODUCTO";
        imageUpload.classList.toggle("is-disabled", isProductOffer);
        imageInput.disabled = isProductOffer;

        if (imageHelp) {
            imageHelp.textContent = isProductOffer
                ? "En ofertas de producto se usará automáticamente la imagen del producto seleccionado."
                : "Sube una imagen para ofertas globales o de categoría.";
        }
    }

    function syncImagePreview(file) {
        if (!imagePreview) {
            return;
        }

        if (file) {
            imagePreview.src = URL.createObjectURL(file);
            imagePreview.hidden = false;
            imageRemoveWrapper?.removeAttribute("hidden");
            if (imageRemove) {
                imageRemove.checked = false;
            }
            return;
        }

        const initialSrc = imagePreview.dataset.initialSrc || "";
        if (initialSrc) {
            imagePreview.src = initialSrc;
            imagePreview.hidden = false;
            return;
        }

        if (!imagePreview.getAttribute("src") || imagePreview.src.startsWith("data:image/gif;base64")) {
            imagePreview.hidden = true;
        }
    }

    function syncDiscount(source) {
        const rawValue = Number(source.value || 10);
        const value = Math.min(99, Math.max(1, rawValue));
        discountInput.value = value;
        discountRange.value = value;
        discountLabel.textContent = value;
        updateOfferPreview();
    }

    function getFilteredProductItems() {
        const query = normalizeOfferSearch(productSearch?.value || "");
        const category = normalizeOfferSearch(productCategory?.value || "");

        return Array.from(productItems).filter(item => {
            const text = normalizeOfferSearch(item.dataset.search || "");
            const categories = normalizeOfferSearch(item.dataset.categories || "");
            const matchesQuery = !query || text.includes(query);
            const matchesCategory = !category || categories.includes(category);
            return matchesQuery && matchesCategory;
        });
    }

    function renderOfferProductList() {
        const filteredItems = getFilteredProductItems();
        const visibleItems = filteredItems.slice(0, productState.visibleCount);

        productItems.forEach(item => {
            item.hidden = true;
        });
        visibleItems.forEach(item => {
            item.hidden = false;
        });

        renderOfferProductPagination(filteredItems.length, visibleItems.length);
    }

    function renderOfferProductPagination(totalCount, visibleCount) {
        if (!productPagination) {
            return;
        }

        productPagination.innerHTML = "";

        if (totalCount === 0) {
            productPagination.innerHTML = '<span>No se han encontrado productos con esos filtros.</span>';
            return;
        }

        if (totalCount <= OFFER_PRODUCT_PAGE_SIZE) {
            return;
        }

        if (productState.visibleCount > OFFER_PRODUCT_PAGE_SIZE) {
            const lessButton = document.createElement("button");
            lessButton.type = "button";
            lessButton.className = "button button-outline worker-load-more-button";
            lessButton.textContent = "Ver menos";
            lessButton.addEventListener("click", function () {
                productState.visibleCount = Math.max(OFFER_PRODUCT_PAGE_SIZE, productState.visibleCount - OFFER_PRODUCT_PAGE_SIZE);
                renderOfferProductList();
            });
            productPagination.appendChild(lessButton);
        }

        if (visibleCount < totalCount) {
            const moreButton = document.createElement("button");
            moreButton.type = "button";
            moreButton.className = "button button-outline worker-load-more-button";
            moreButton.textContent = "Ver mas";
            moreButton.addEventListener("click", function () {
                productState.visibleCount += OFFER_PRODUCT_PAGE_SIZE;
                renderOfferProductList();
            });
            productPagination.appendChild(moreButton);
        }

        const counter = document.createElement("span");
        counter.textContent = `${visibleCount} de ${totalCount}`;
        productPagination.appendChild(counter);
    }

    productSearch?.addEventListener("input", function () {
        productState.visibleCount = OFFER_PRODUCT_PAGE_SIZE;
        renderOfferProductList();
    });

    productCategory?.addEventListener("change", function () {
        productState.visibleCount = OFFER_PRODUCT_PAGE_SIZE;
        renderOfferProductList();
    });
    imageInput?.addEventListener("change", function () {
        syncImagePreview(this.files?.[0]);
    });
    imageRemove?.addEventListener("change", function () {
        if (!this.checked) {
            return;
        }
        if (imageInput) {
            imageInput.value = "";
        }
        if (imagePreview) {
            imagePreview.removeAttribute("src");
            imagePreview.dataset.initialSrc = "";
            imagePreview.hidden = true;
        }
    });

    typeInputs.forEach(input => input.addEventListener("change", syncTargetBlocks));
    productItems.forEach(item => item.addEventListener("change", updateOfferPreview));
    categorySelect?.addEventListener("change", updateOfferPreview);
    startInput?.addEventListener("change", updateOfferPreview);
    endInput?.addEventListener("change", updateOfferPreview);
    discountInput?.addEventListener("input", function () { syncDiscount(this); });
    discountRange?.addEventListener("input", function () { syncDiscount(this); });

    syncTargetBlocks();
    syncDiscount(discountInput);
    renderOfferProductList();
    syncImagePreview(imageInput?.files?.[0]);
}

function updateOfferPreview() {
    const form = document.querySelector("[data-offer-form]");
    if (!form) {
        return;
    }

    const type = form.querySelector("[data-offer-type]:checked")?.value || "GLOBAL";
    const discount = Number(form.querySelector("[data-discount-input]")?.value || 10);
    const selectedProducts = Array.from(form.querySelectorAll("input[name='productoIds']:checked"))
        .map(input => input.closest(".offer-smart-item"))
        .filter(Boolean);
    const selectedCategory = form.querySelector("[data-category-offer-select]");
    const categoryText = selectedCategory?.options[selectedCategory.selectedIndex]?.textContent || "";
    const title = form.querySelector("[data-preview-title]");
    const description = form.querySelector("[data-preview-description]");
    const priceBox = form.querySelector("[data-preview-price]");
    const original = form.querySelector("[data-preview-original]");
    const final = form.querySelector("[data-preview-final]");
    const dateText = form.querySelector("[data-preview-date]");
    const start = form.querySelector("[data-offer-start]")?.value;
    const end = form.querySelector("[data-offer-end]")?.value;
    const previewImage = form.querySelector("[data-offer-image-preview]");

    if (type === "PRODUCTO") {
        const previewProductImage = selectedProducts.find(item => item.dataset.productImage)?.dataset.productImage || "";
        if (previewImage) {
            if (previewProductImage) {
                previewImage.src = previewProductImage;
                previewImage.hidden = false;
            } else if (!previewImage.dataset.initialSrc) {
                previewImage.hidden = true;
            }
        }
    } else {
        syncImagePreview(form.querySelector("[data-offer-image-input]")?.files?.[0]);
    }

    if (type === "PRODUCTO" && selectedProducts.length === 1) {
        const selectedProduct = selectedProducts[0];
        const productName = selectedProduct.dataset.productName || "producto seleccionado";
        const price = Number(String(selectedProduct.dataset.productPrice || "0").replace(",", "."));
        const discounted = price * (100 - discount) / 100;

        title.textContent = `${discount}% en ${productName}`;
        description.textContent = `Se aplicara un ${discount}% de descuento al producto ${productName}.`;
        original.textContent = formatOfferPrice(price);
        final.textContent = formatOfferPrice(discounted);
        priceBox.hidden = false;
    } else if (type === "PRODUCTO" && selectedProducts.length > 1) {
        title.textContent = `${discount}% en ${selectedProducts.length} productos`;
        description.textContent = `Se aplicara un ${discount}% de descuento a los ${selectedProducts.length} productos seleccionados.`;
        priceBox.hidden = true;
    } else if (type === "PRODUCTO") {
        title.textContent = "Selecciona productos";
        description.textContent = "Marca uno o varios productos para aplicarles el mismo descuento.";
        priceBox.hidden = true;
    } else if (type === "CATEGORIA" && selectedCategory?.value) {
        title.textContent = `${discount}% en ${categoryText}`;
        description.textContent = `Se aplicará un ${discount}% de descuento a todos los productos de la categoría ${categoryText}.`;
        priceBox.hidden = true;
    } else if (type === "GLOBAL") {
        title.textContent = `${discount}% en toda la tienda`;
        description.textContent = `Se aplicara un ${discount}% de descuento a toda la tienda siguiendo la prioridad de ofertas.`;
        priceBox.hidden = true;
    } else {
        title.textContent = "Configura la oferta";
        description.textContent = "Selecciona el destino de la oferta para ver el resultado antes de guardar.";
        priceBox.hidden = true;
    }

    if (end) {
        dateText.textContent = `Disponible hasta el ${formatOfferDate(end)}.`;
    } else if (start) {
        dateText.textContent = `Disponible desde el ${formatOfferDate(start)}.`;
    } else {
        dateText.textContent = "Sin fechas limitadas.";
    }
}

function initHomeOfferCountdown() {
    document.querySelectorAll("[data-home-offer-end]").forEach(element => {
        const endDate = element.dataset.homeOfferEnd;
        if (!endDate) {
            return;
        }

        const end = new Date(`${endDate}T23:59:59`);
        const now = new Date();
        const diffDays = Math.ceil((end - now) / (1000 * 60 * 60 * 24));

        if (Number.isNaN(diffDays) || diffDays < 0) {
            element.textContent = "Oferta por tiempo limitado";
            return;
        }

        if (diffDays === 0) {
            element.textContent = "Termina hoy";
            return;
        }

        element.textContent = diffDays === 1 ? "Termina manana" : `Quedan ${diffDays} dias`;
    });
}

function formatOfferPrice(value) {
    return new Intl.NumberFormat("es-ES", {
        style: "currency",
        currency: "EUR"
    }).format(value || 0);
}

function formatOfferDate(value) {
    const date = new Date(`${value}T00:00:00`);
    return new Intl.DateTimeFormat("es-ES", {
        day: "2-digit",
        month: "long",
        year: "numeric"
    }).format(date);
}

function normalizeOfferSearch(value) {
    return String(value || "")
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim();
}
