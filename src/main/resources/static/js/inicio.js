document.addEventListener("DOMContentLoaded", function () {
    const body = document.body;
    const menuToggle = document.querySelector("[data-menu-toggle]");
    const navLinks = document.querySelectorAll(".nav-panel a, .nav-auth-actions a, .nav-user-dropdown a, .nav-icon-link");
    const userMenu = document.querySelector("[data-user-menu]");
    const userTrigger = document.querySelector("[data-user-menu-trigger]");
    const revealItems = document.querySelectorAll("[data-reveal]");

    initModernSelects();
    initActiveNavState();
    initCartBadgeAnimation();

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
        initUserMenu(userMenu, userTrigger);
        return;
    }

    menuToggle.addEventListener("click", function () {
        const isOpen = body.classList.toggle("nav-open");
        menuToggle.setAttribute("aria-expanded", String(isOpen));
        if (!isOpen) {
            closeUserMenu(userMenu, userTrigger);
        }
    });

    navLinks.forEach(function (link) {
        link.addEventListener("click", function () {
            body.classList.remove("nav-open");
            menuToggle.setAttribute("aria-expanded", "false");
        });
    });

    initUserMenu(userMenu, userTrigger);

    document.addEventListener("click", function (event) {
        if (!event.target.closest(".site-header .header-shell")) {
            body.classList.remove("nav-open");
            menuToggle.setAttribute("aria-expanded", "false");
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            body.classList.remove("nav-open");
            menuToggle.setAttribute("aria-expanded", "false");
            closeUserMenu(userMenu, userTrigger);
        }
    });
});

function initActiveNavState() {
    const currentPath = window.location.pathname || "/";
    document.querySelectorAll("[data-nav-link]").forEach(function (link) {
        const matchRaw = link.getAttribute("data-nav-match");
        if (!matchRaw) {
            return;
        }

        const matches = matchRaw.split(",").map(function (item) {
            return item.trim();
        }).filter(Boolean);

        const isActive = matches.some(function (match) {
            if (match === "/") {
                return currentPath === "/" || currentPath === "";
            }
            return currentPath === match || currentPath.startsWith(match + "/") || currentPath.startsWith(match);
        });

        link.classList.toggle("is-active", isActive);
    });
}

function initUserMenu(userMenu, userTrigger) {
    if (!userMenu || !userTrigger) {
        return;
    }

    userTrigger.addEventListener("click", function (event) {
        event.stopPropagation();
        const isOpen = userMenu.classList.toggle("is-open");
        userTrigger.setAttribute("aria-expanded", String(isOpen));
    });

    document.addEventListener("click", function (event) {
        if (!event.target.closest("[data-user-menu]")) {
            closeUserMenu(userMenu, userTrigger);
        }
    });
}

function closeUserMenu(userMenu, userTrigger) {
    if (!userMenu || !userTrigger) {
        return;
    }
    userMenu.classList.remove("is-open");
    userTrigger.setAttribute("aria-expanded", "false");
}

function initCartBadgeAnimation() {
    const badge = document.querySelector(".nav-cart-badge");
    if (!badge) {
        return;
    }

    const currentCount = Number.parseInt(badge.dataset.count || badge.textContent || "0", 10) || 0;
    const storageKey = "maxcopias.nav.cartCount";
    const previousCount = Number.parseInt(window.sessionStorage.getItem(storageKey) || String(currentCount), 10) || 0;

    if (currentCount !== previousCount) {
        badge.classList.add("is-bump");
        window.setTimeout(function () {
            badge.classList.remove("is-bump");
        }, 380);
    }

    window.sessionStorage.setItem(storageKey, String(currentCount));
}

function initModernSelects() {
    document.querySelectorAll("select.select-modern").forEach(function (select) {
        if (select.dataset.customSelectReady === "true" || select.dataset.selectModernReady === "true") {
            return;
        }

        select.dataset.selectModernReady = "true";
        select.classList.add("select-modern-native");

        const wrapper = document.createElement("div");
        wrapper.className = "select-modern-dropdown";
        wrapper.dataset.selectModernDropdown = "";

        const trigger = document.createElement("button");
        trigger.type = "button";
        trigger.className = "select-modern-trigger";
        trigger.setAttribute("aria-haspopup", "listbox");
        trigger.setAttribute("aria-expanded", "false");
        trigger.innerHTML = `
            <span data-select-modern-label></span>
            <span class="select-modern-arrow" aria-hidden="true"></span>
        `;

        const menu = document.createElement("div");
        menu.className = "select-modern-menu";
        menu.setAttribute("role", "listbox");

        Array.from(select.options).forEach(function (option) {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "select-modern-option";
            item.dataset.optionValue = option.value;
            item.setAttribute("role", "option");
            item.textContent = option.textContent;

            item.addEventListener("click", function () {
                select.value = option.value;
                select.dispatchEvent(new Event("change", { bubbles: true }));
                syncModernSelect(select);
                closeModernSelectDropdown(wrapper);
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
            closeOtherModernSelectDropdowns(wrapper);
        });

        select.addEventListener("change", function () {
            syncModernSelect(select);
        });

        syncModernSelect(select);
    });
}

function syncModernSelect(select) {
    const wrapper = select.closest("[data-select-modern-dropdown]");
    if (!wrapper) {
        return;
    }

    const selectedOption = select.options[select.selectedIndex];
    const label = wrapper.querySelector("[data-select-modern-label]");
    const options = wrapper.querySelectorAll(".select-modern-option");

    if (label && selectedOption) {
        label.textContent = selectedOption.textContent;
        label.classList.toggle("is-placeholder", !selectedOption.value);
    }

    if (selectedOption) {
        wrapper.dataset.currentValue = selectedOption.value || "";
    }

    options.forEach(function (option) {
        const isSelected = option.dataset.optionValue === select.value;
        option.classList.toggle("is-selected", isSelected);
        option.setAttribute("aria-selected", String(isSelected));
    });
}

function closeModernSelectDropdown(wrapper) {
    if (!wrapper) {
        return;
    }
    wrapper.classList.remove("is-open");
    wrapper.querySelector(".select-modern-trigger")?.setAttribute("aria-expanded", "false");
}

function closeOtherModernSelectDropdowns(activeWrapper) {
    document.querySelectorAll("[data-select-modern-dropdown].is-open").forEach(function (wrapper) {
        if (wrapper !== activeWrapper) {
            closeModernSelectDropdown(wrapper);
        }
    });
}

document.addEventListener("click", function (event) {
    if (!event.target.closest("[data-select-modern-dropdown]")) {
        closeOtherModernSelectDropdowns(null);
    }
});
