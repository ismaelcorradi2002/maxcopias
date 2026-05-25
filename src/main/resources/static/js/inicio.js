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
    initCookieConsent();

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

function initCookieConsent() {
    const storageKey = "maxcopias.cookieConsent.v1";
    const banner = document.querySelector("[data-cookie-banner]");
    const panel = document.querySelector("[data-cookie-panel]");
    const externalToggle = document.querySelector("[data-cookie-toggle='externalMedia']");

    if (!banner || !panel) {
        applyCookiePreferences(loadCookiePreferences(storageKey));
        return;
    }

    const savedPreferences = loadCookiePreferences(storageKey);

    if (externalToggle) {
        externalToggle.checked = Boolean(savedPreferences && savedPreferences.externalMedia);
    }

    applyCookiePreferences(savedPreferences);
    toggleCookieBanner(banner, !savedPreferences);
    toggleCookiePanel(panel, false);

    document.querySelectorAll("[data-cookie-open]").forEach(function (button) {
        button.addEventListener("click", function () {
            if (externalToggle) {
                const currentPreferences = loadCookiePreferences(storageKey);
                externalToggle.checked = Boolean(currentPreferences && currentPreferences.externalMedia);
            }
            toggleCookiePanel(panel, true);
        });
    });

    document.querySelectorAll("[data-cookie-close]").forEach(function (button) {
        button.addEventListener("click", function () {
            toggleCookiePanel(panel, false);
        });
    });

    document.querySelectorAll("[data-cookie-accept-all]").forEach(function (button) {
        button.addEventListener("click", function () {
            const preferences = {
                necessary: true,
                externalMedia: true
            };
            persistCookiePreferences(storageKey, preferences);
            if (externalToggle) {
                externalToggle.checked = true;
            }
            applyCookiePreferences(preferences);
            toggleCookieBanner(banner, false);
            toggleCookiePanel(panel, false);
        });
    });

    document.querySelectorAll("[data-cookie-reject]").forEach(function (button) {
        button.addEventListener("click", function () {
            const preferences = {
                necessary: true,
                externalMedia: false
            };
            persistCookiePreferences(storageKey, preferences);
            if (externalToggle) {
                externalToggle.checked = false;
            }
            applyCookiePreferences(preferences);
            toggleCookieBanner(banner, false);
            toggleCookiePanel(panel, false);
        });
    });

    document.querySelectorAll("[data-cookie-save]").forEach(function (button) {
        button.addEventListener("click", function () {
            const preferences = {
                necessary: true,
                externalMedia: Boolean(externalToggle && externalToggle.checked)
            };
            persistCookiePreferences(storageKey, preferences);
            applyCookiePreferences(preferences);
            toggleCookieBanner(banner, false);
            toggleCookiePanel(panel, false);
        });
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            toggleCookiePanel(panel, false);
        }
    });
}

function loadCookiePreferences(storageKey) {
    try {
        const rawValue = window.localStorage.getItem(storageKey);
        if (!rawValue) {
            return null;
        }
        return JSON.parse(rawValue);
    } catch (error) {
        return null;
    }
}

function persistCookiePreferences(storageKey, preferences) {
    try {
        window.localStorage.setItem(storageKey, JSON.stringify(preferences));
    } catch (error) {
        // Ignore storage errors and keep runtime behavior.
    }
}

function toggleCookieBanner(banner, shouldShow) {
    if (!banner) {
        return;
    }
    banner.hidden = !shouldShow;
}

function toggleCookiePanel(panel, shouldShow) {
    if (!panel) {
        return;
    }
    panel.hidden = !shouldShow;
    document.body.classList.toggle("cookie-panel-open", shouldShow);
}

function applyCookiePreferences(preferences) {
    const allowExternalMedia = Boolean(preferences && preferences.externalMedia);

    document.documentElement.dataset.cookieExternalMedia = String(allowExternalMedia);

    document.querySelectorAll("[data-cookie-category='external-media']").forEach(function (embed) {
        const targetSrc = embed.dataset.cookieSrc;
        if (!targetSrc) {
            return;
        }

        if (allowExternalMedia) {
            if (embed.getAttribute("src") !== targetSrc) {
                embed.setAttribute("src", targetSrc);
            }
            embed.hidden = false;
        } else {
            if (embed.hasAttribute("src")) {
                embed.removeAttribute("src");
            }
            embed.hidden = true;
        }
    });

    document.querySelectorAll("[data-cookie-placeholder='externalMedia']").forEach(function (placeholder) {
        placeholder.hidden = allowExternalMedia;
    });
}
