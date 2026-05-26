package com.maxcopias.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ControladorSeoAdvice {

    private static final String DEFAULT_OG_IMAGE = "/images/maxcopias-welcome-hero-premium.png";

    private static final Map<String, SeoPage> SEO_PAGES = Map.of(
        "/", new SeoPage(
            "Maxcopias | Copisteria y papeleria en Torrejon de Ardoz",
            "Copisteria y papeleria en Torrejon de Ardoz. Imprime documentos online, compra papeleria y elige recogida o envio local.",
            "website"
        ),
        "/copisteria", new SeoPage(
            "Copisteria online en Torrejon de Ardoz | Imprime documentos en Maxcopias",
            "Imprime documentos en Torrejon de Ardoz con recogida en tienda. Fotocopias, encuadernacion y copisteria online en Maxcopias.",
            "website"
        ),
        "/tienda", new SeoPage(
            "Papeleria online en Torrejon de Ardoz | Maxcopias",
            "Compra papeleria online en Torrejon de Ardoz con recogida en tienda o envio local. Material escolar y de oficina en Maxcopias.",
            "website"
        ),
        "/ofertas", new SeoPage(
            "Ofertas de copisteria y papeleria | Maxcopias Torrejon de Ardoz",
            "Consulta ofertas de copisteria y papeleria en Torrejon de Ardoz. Promociones locales para imprimir y comprar en Maxcopias.",
            "website"
        ),
        "/contacto", new SeoPage(
            "Contacto y ubicacion | Maxcopias Torrejon de Ardoz",
            "Contacta con Maxcopias en C/ Soledad, N 3, 28850 Torrejon de Ardoz, Madrid. Telefono, email, horario y ubicacion.",
            "website"
        ),
        "/aviso-legal", new SeoPage(
            "Aviso legal | Maxcopias",
            "Consulta el aviso legal de Maxcopias y la informacion general sobre el uso de la web y sus servicios.",
            "website"
        ),
        "/privacidad", new SeoPage(
            "Politica de privacidad | Maxcopias",
            "Consulta como trata Maxcopias los datos personales de clientes, pedidos, cuentas y archivos subidos desde la web.",
            "website"
        ),
        "/cookies", new SeoPage(
            "Politica de cookies | Maxcopias",
            "Informacion sobre cookies tecnicas, contenido externo y gestion del consentimiento en la web de Maxcopias.",
            "website"
        ),
        "/condiciones-compra", new SeoPage(
            "Condiciones de compra | Maxcopias",
            "Consulta las condiciones de compra de copisteria y papeleria online de Maxcopias, con recogida en tienda y envio local.",
            "website"
        )
    );

    @ModelAttribute("seoTitle")
    public String seoTitle(HttpServletRequest request) {
        SeoPage seoPage = SEO_PAGES.get(normalizePath(request));
        return seoPage != null ? seoPage.title() : null;
    }

    @ModelAttribute("seoDescription")
    public String seoDescription(HttpServletRequest request) {
        SeoPage seoPage = SEO_PAGES.get(normalizePath(request));
        return seoPage != null ? seoPage.description() : null;
    }

    @ModelAttribute("seoOgTitle")
    public String seoOgTitle(HttpServletRequest request) {
        SeoPage seoPage = SEO_PAGES.get(normalizePath(request));
        return seoPage != null ? seoPage.title() : null;
    }

    @ModelAttribute("seoOgDescription")
    public String seoOgDescription(HttpServletRequest request) {
        SeoPage seoPage = SEO_PAGES.get(normalizePath(request));
        return seoPage != null ? seoPage.description() : null;
    }

    @ModelAttribute("seoOgType")
    public String seoOgType(HttpServletRequest request) {
        SeoPage seoPage = SEO_PAGES.get(normalizePath(request));
        return seoPage != null ? seoPage.ogType() : "website";
    }

    @ModelAttribute("seoCanonicalUrl")
    public String seoCanonicalUrl(HttpServletRequest request) {
        String path = normalizePath(request);
        return buildBaseUrl(request) + path;
    }

    @ModelAttribute("seoOgImageUrl")
    public String seoOgImageUrl(HttpServletRequest request) {
        return buildBaseUrl(request) + DEFAULT_OG_IMAGE;
    }

    @ModelAttribute("seoRobots")
    public String seoRobots(HttpServletRequest request) {
        String path = normalizePath(request);
        if (path.startsWith("/admin")
            || path.startsWith("/worker")
            || path.startsWith("/area-personal")
            || path.startsWith("/mis-pedidos")
            || path.startsWith("/checkout")
            || path.startsWith("/carrito")
            || path.startsWith("/login")
            || path.startsWith("/register")) {
            return "noindex,nofollow";
        }
        return "index,follow";
    }

    @ModelAttribute("seoStructuredDataJson")
    public String seoStructuredDataJson(HttpServletRequest request) {
        String path = normalizePath(request);
        if (!"/".equals(path) && !"/contacto".equals(path)) {
            return null;
        }

        String baseUrl = buildBaseUrl(request);
        String canonicalUrl = baseUrl + path;
        String imageUrl = baseUrl + DEFAULT_OG_IMAGE;

        return """
            {
              "@context": "https://schema.org",
              "@type": ["Store", "OfficeEquipmentStore"],
              "name": "Maxcopias",
              "url": "%s",
              "image": "%s",
              "telephone": "+34 916 563 555",
              "email": "pedidos@maxcopias.es",
              "address": {
                "@type": "PostalAddress",
                "streetAddress": "C/ Soledad, N 3",
                "postalCode": "28850",
                "addressLocality": "Torrejon de Ardoz",
                "addressRegion": "Madrid",
                "addressCountry": "ES"
              },
              "areaServed": {
                "@type": "City",
                "name": "Torrejon de Ardoz"
              }
            }
            """.formatted(canonicalUrl, imageUrl);
    }

    private String normalizePath(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "/";
        }
        String normalized = contextPath.isBlank() ? requestUri : requestUri.substring(contextPath.length());
        return normalized.isBlank() ? "/" : normalized;
    }

    private String buildBaseUrl(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());

        int port = request.getServerPort();
        boolean includePort = ("http".equalsIgnoreCase(request.getScheme()) && port != 80)
            || ("https".equalsIgnoreCase(request.getScheme()) && port != 443);

        if (includePort) {
            builder.append(':').append(port);
        }

        if (request.getContextPath() != null && !request.getContextPath().isBlank()) {
            builder.append(request.getContextPath());
        }

        return builder.toString();
    }

    private record SeoPage(String title, String description, String ogType) {}
}
