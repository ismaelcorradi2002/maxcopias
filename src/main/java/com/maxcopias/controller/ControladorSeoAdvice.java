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
            "Maxcopias | Copistería y papelería online con envío a domicilio",
            "Haz pedidos de impresión, copistería y papelería online. Envíos a domicilio en Madrid y toda España.",
            "website"
        ),
        "/copisteria", new SeoPage(
            "Copistería online | Imprime documentos y recíbelos en casa",
            "Sube tus documentos, configura tu impresión online y recibe tus copias en casa. Servicio de copistería online con envío a domicilio.",
            "website"
        ),
        "/copisteria/pedido", new SeoPage(
            "Maxcopias | Configurador de copistería",
            "Prepara tu pedido de impresión online, sube tus archivos y solicita envío a domicilio.",
            "website"
        ),
        "/tienda", new SeoPage(
            "Papelería online | Material de oficina y productos escolares a domicilio",
            "Compra productos de papelería y material de oficina online. Envíos a Madrid y toda España.",
            "website"
        ),
        "/ofertas", new SeoPage(
            "Ofertas de copistería y papelería online | Maxcopias",
            "Consulta promociones activas de copistería online y papelería con envío a domicilio.",
            "website"
        ),
        "/contacto", new SeoPage(
            "Contacto | Maxcopias online",
            "Contacta con Maxcopias para resolver dudas sobre pedidos online, impresión, productos o envíos.",
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
            "Consulta las condiciones de compra de copistería y papelería online de Maxcopias con envío a domicilio.",
            "website"
        )
    );

    @ModelAttribute("seoTitle")
    public String seoTitle(HttpServletRequest request) {
        SeoPage seoPage = resolveSeoPage(request);
        return seoPage != null ? seoPage.title() : null;
    }

    @ModelAttribute("seoDescription")
    public String seoDescription(HttpServletRequest request) {
        SeoPage seoPage = resolveSeoPage(request);
        return seoPage != null ? seoPage.description() : null;
    }

    @ModelAttribute("seoOgTitle")
    public String seoOgTitle(HttpServletRequest request) {
        SeoPage seoPage = resolveSeoPage(request);
        return seoPage != null ? seoPage.title() : null;
    }

    @ModelAttribute("seoOgDescription")
    public String seoOgDescription(HttpServletRequest request) {
        SeoPage seoPage = resolveSeoPage(request);
        return seoPage != null ? seoPage.description() : null;
    }

    @ModelAttribute("seoOgType")
    public String seoOgType(HttpServletRequest request) {
        SeoPage seoPage = resolveSeoPage(request);
        return seoPage != null ? seoPage.ogType() : "website";
    }

    @ModelAttribute("seoCanonicalUrl")
    public String seoCanonicalUrl(HttpServletRequest request) {
        return buildBaseUrl(request) + normalizePath(request);
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
            || path.startsWith("/register")
            || path.startsWith("/copisteria/pedido")) {
            return "noindex,nofollow";
        }
        return "index,follow";
    }

    @ModelAttribute("seoStructuredDataJson")
    public String seoStructuredDataJson(HttpServletRequest request) {
        String path = normalizePath(request);
        if (!"/".equals(path) && !"/contacto".equals(path) && !"/copisteria".equals(path)) {
            return null;
        }

        String baseUrl = buildBaseUrl(request);
        String canonicalUrl = baseUrl + path;
        String imageUrl = baseUrl + DEFAULT_OG_IMAGE;

        return """
            {
              "@context": "https://schema.org",
              "@type": ["Organization", "OnlineStore"],
              "name": "Maxcopias",
              "url": "%s",
              "image": "%s",
              "telephone": "+34 916 563 555",
              "email": "pedidos@maxcopias.es",
              "description": "Copisteria y papeleria online con envio a domicilio en Madrid y toda Espana.",
              "address": {
                "@type": "PostalAddress",
                "streetAddress": "C/ Soledad, Nº3",
                "postalCode": "28850",
                "addressLocality": "Torrejón de Ardoz",
                "addressRegion": "Madrid",
                "addressCountry": "ES"
              },
              "areaServed": ["ES", "Madrid"]
            }
            """.formatted(canonicalUrl, imageUrl);
    }

    private SeoPage resolveSeoPage(HttpServletRequest request) {
        String path = normalizePath(request);
        SeoPage exact = SEO_PAGES.get(path);
        if (exact != null) {
            return exact;
        }
        if (path.startsWith("/copisteria/pedido")) {
            return SEO_PAGES.get("/copisteria/pedido");
        }
        return null;
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
