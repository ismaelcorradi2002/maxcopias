document.addEventListener("DOMContentLoaded", function () {
  initCarrusel();
  initFiltrosTienda();
  initCartFeedback();
  initCartForms();
  initCheckoutDeliveryForm();
});

function initCarrusel() {
  const items = document.querySelectorAll(".carousel-item");
  const indicators = document.querySelectorAll(".carousel-indicator");
  const carousel = document.querySelector(".carousel-inner");

  if (!items.length || !indicators.length || !carousel) {
    return;
  }

  let currentIndex = 0;
  let carouselInterval;

  function showSlide(index) {
    items.forEach((item, itemIndex) => {
      item.style.opacity = itemIndex === index ? "1" : "0";
    });

    indicators.forEach((indicator, indicatorIndex) => {
      indicator.classList.toggle("active", indicatorIndex === index);
    });

    currentIndex = index;
  }

  function nextSlide() {
    const nextIndex = (currentIndex + 1) % items.length;
    showSlide(nextIndex);
  }

  function startCarousel() {
    carouselInterval = setInterval(nextSlide, 3000);
  }

  function stopCarousel() {
    clearInterval(carouselInterval);
  }

  indicators.forEach((indicator, index) => {
    indicator.addEventListener("click", function () {
      stopCarousel();
      showSlide(index);
      startCarousel();
    });
  });

  carousel.addEventListener("mouseenter", stopCarousel);
  carousel.addEventListener("mouseleave", startCarousel);

  showSlide(0);
  startCarousel();
}

function initFiltrosTienda() {
  const tabs = document.querySelectorAll(".section-tab");
  const cards = document.querySelectorAll(".tienda-producto-card");
  const title = document.getElementById("productos-title");

  if (!tabs.length || !cards.length || !title) {
    return;
  }

  function updateTitle(category, label) {
    if (category === "todos") {
      title.innerHTML = '<span class="section-kicker">Todos</span><h2>Catalogo completo</h2>';
      return;
    }

    title.innerHTML = `<span class="section-kicker">${label}</span><h2>Productos ${label}</h2>`;
  }

  function applyFilter(category, label) {
    cards.forEach((card) => {
      const categories = (card.dataset.categories || "").split(",").filter(Boolean);
      const showCard = category === "todos" || categories.includes(category);
      card.hidden = !showCard;
    });

    updateTitle(category, label);
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", function () {
      tabs.forEach((button) => button.classList.remove("active"));
      this.classList.add("active");
      applyFilter(this.dataset.category || "todos", this.textContent.trim());
    });
  });

  applyFilter("todos", "Todos");
}

function initCartFeedback() {
  const feedbacks = document.querySelectorAll(".tienda-feedback");
  if (!feedbacks.length) {
    return;
  }

  setTimeout(() => {
    feedbacks.forEach((feedback) => feedback.classList.add("is-hidden"));
  }, 4000);
}

function initCartForms() {
  const forms = document.querySelectorAll(".tienda-add-form, .producto-add-form");
  forms.forEach((form) => {
    form.addEventListener("submit", () => {
      const button = form.querySelector("button[type='submit']");
      if (!button) {
        return;
      }
      button.disabled = true;
      button.textContent = "Anadiendo...";
    });
  });
}

function initCheckoutDeliveryForm() {
  const addressBlock = document.querySelector("[data-checkout-delivery-address]");
  const deliveryInputs = document.querySelectorAll('input[name="metodoEntrega"]');

  if (!addressBlock || !deliveryInputs.length) {
    return;
  }

  function syncDeliveryAddress() {
    const selected = document.querySelector('input[name="metodoEntrega"]:checked');
    const isHomeDelivery = selected && selected.value === "ENVIO_DOMICILIO";
    addressBlock.classList.toggle("is-visible", Boolean(isHomeDelivery));
  }

  deliveryInputs.forEach((input) => {
    input.addEventListener("change", syncDeliveryAddress);
  });

  syncDeliveryAddress();
}
