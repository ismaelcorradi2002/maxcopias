document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-product-image-form]").forEach(initProductImageForm);
});

function initProductImageForm(form) {
    const input = form.querySelector("[data-product-image-input]");
    const preview = form.querySelector("[data-product-image-preview-img]");
    const changeButton = form.querySelector("[data-product-image-change]");
    const clearButton = form.querySelector("[data-product-image-clear]");
    const removeInput = form.querySelector("[data-product-image-remove]");
    const placeholder = "/images/placeholders/product-placeholder-card.svg";

    if (!input || !preview || !changeButton || !clearButton || !removeInput) {
        return;
    }

    changeButton.addEventListener("click", function () {
        input.click();
    });

    clearButton.addEventListener("click", function () {
        input.value = "";
        preview.src = placeholder;
        removeInput.value = "true";
    });

    input.addEventListener("change", function () {
        const [file] = input.files || [];
        if (!file) {
            return;
        }

        removeInput.value = "false";
        const reader = new FileReader();
        reader.onload = function (event) {
            preview.src = event.target?.result || placeholder;
        };
        reader.readAsDataURL(file);
    });
}
