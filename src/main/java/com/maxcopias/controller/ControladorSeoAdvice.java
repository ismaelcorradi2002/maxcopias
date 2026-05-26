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
            "Maxcopias | Copistería y papelería en Torrejón de Ardoz",
            "Copistería y papelería en Torrejón de Ardoz. Imprime documentos online, compra papelería y elige recogida o envío local.",
            "website"
        ),
        "/copisteria", new SeoPage(
            "Copistería online en Torrejón de Ardoz | Imprime documentos en Maxcopias",
            "Imprime documentos en Torrejón de Ardoz con recogida en tienda. Fotocopias, encuadernación y copistería online en Maxcopias.",
            "website"
        ),
        "/tienda", new SeoPage(
            "Papelería online en Torrejón de Ardoz | Maxcopias",
            "Compra papelería online en Torrejón de Ardoz con recogida en tienda o envío local. Material escolar y de oficina en Maxcopias.",
            "website"
        ),
        "/ofertas", new SeoPage(
            "Ofertas de copistería y papelería | Maxcopias Torrejón de Ardoz",
            "Consulta ofertas de copistería y papelería en Torrejón de Ardoz. Promociones locales para imprimir y comprar en Maxcopias.",
            "website"
        ),
        "/contacto", new SeoPage(
            "Contacto y ubicación | Maxcopias Torrejón de Ardoz",
            "Contacta con Maxcopias en C/ Soledad, N 3, 28850 Torrejón de Ardoz, Madrid. Teléfono, email, horario y ubicación.",
            "website"
        ),
        "/aviso-legal", new SeoPage(
            "Aviso legal | Maxcopias",
            "Consulta el aviso legal de Maxcopias y la información general sobre el uso de la web y sus servicios.",
            "website"
        ),
        "/privacidad", new SeoPage(
            "Política de privacidad | Maxcopias",
            "Consulta cómo trata Maxcopias los datos personales de clientes, pedidos, cuentas y archivos subidos desde la web.",
            "website"
        ),
        "/cookies", new SeoPage(
            "Política de cookies | Maxcopias",
            "Información sobre cookies técnicas, contenido externo y gestión del consentimiento en la web de Maxcopias.",
            "website"
        ),
        "/condiciones-compra", new SeoPage(
            "Condiciones de compra | Maxcopias",
            "Consulta las condiciones de compra de copistería y papelería online de Maxcopias, con recogida en tienda y envío local.",
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
                "addressLocality": "Torrejón de Ardoz",
                "addressRegion": "Madrid",
                "addressCountry": "ES"
              },
              "areaServed": {
                "@type": "City",
                "name": "Torrejón de Ardoz"
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
