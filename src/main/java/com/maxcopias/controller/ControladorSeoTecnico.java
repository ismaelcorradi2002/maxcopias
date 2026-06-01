package com.maxcopias.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorSeoTecnico {

    private static final List<String> SITEMAP_PATHS = List.of(
        "/",
        "/copisteria",
        "/tienda",
        "/ofertas",
        "/contacto",
        "/aviso-legal",
        "/privacidad",
        "/cookies",
        "/condiciones-compra"
    );

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        return """
            User-agent: *
            Allow: /

            Sitemap: %s/sitemap.xml
            """.formatted(baseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        String lastModified = LocalDate.now().toString();

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        for (String path : SITEMAP_PATHS) {
            builder.append("<url>");
            builder.append("<loc>").append(escapeXml(baseUrl + path)).append("</loc>");
            builder.append("<lastmod>").append(lastModified).append("</lastmod>");
            builder.append("</url>");
        }

        builder.append("</urlset>");
        return builder.toString();
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

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
