document.addEventListener("DOMContentLoaded", function () {
    const body = document.body;
    const menuToggle = document.querySelector("[data-menu-toggle]");
    const navLinks = document.querySelectorAll(".nav-panel a");
    const revealItems = document.querySelectorAll("[data-reveal]");

    if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver(function (entries, currentObserver) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) {
                    return;
                }

                entry.target.classList.add("is-visible");
                currentObserver.unobserve(entry.target);
            });
        }, {
            threshold: 0.18,
            rootMargin: "0px 0px -40px 0px"
        });

        revealItems.forEach(function (item) {
            observer.observe(item);
        });
    } else {
        revealItems.forEach(function (item) {
            item.classList.add("is-visible");
        });
    }

    if (!menuToggle) {
        return;
    }

    menuToggle.addEventListener("click", function () {
        const isOpen = body.classList.toggle("nav-open");
        menuToggle.setAttribute("aria-expanded", String(isOpen));
    });

    navLinks.forEach(function (link) {
        link.addEventListener("click", function () {
            body.classList.remove("nav-open");
            menuToggle.setAttribute("aria-expanded", "false");
        });
    });
});
