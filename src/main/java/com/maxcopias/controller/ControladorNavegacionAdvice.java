package com.maxcopias.controller;

import com.maxcopias.model.Oferta;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioOferta;
import com.maxcopias.service.ServicioUsuario;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ControladorNavegacionAdvice {

    private final ServicioOferta servicioOferta;
    private final ServicioUsuario servicioUsuario;

    public ControladorNavegacionAdvice(ServicioOferta servicioOferta, ServicioUsuario servicioUsuario) {
        this.servicioOferta = servicioOferta;
        this.servicioUsuario = servicioUsuario;
    }

    @ModelAttribute("navOfertaPrincipal")
    public Oferta navOfertaPrincipal() {
        return servicioOferta.obtenerOfertaPrincipalActiva().orElse(null);
    }

    @ModelAttribute("navOfertaBadge")
    public String navOfertaBadge() {
        Oferta oferta = servicioOferta.obtenerOfertaPrincipalActiva().orElse(null);
        if (oferta == null) {
            return null;
        }

        Integer porcentaje = oferta.getPorcentajeDescuento();
        if (porcentaje != null && porcentaje > 0) {
            return porcentaje + "% OFF";
        }

        return "Nueva";
    }

    @ModelAttribute("navUser")
    public NavUserView navUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        Usuario usuario = servicioUsuario.findByEmail(authentication.getName()).orElse(null);
        if (usuario == null) {
            return null;
        }

        String fullName = buildFullName(usuario);
        String displayName = buildDisplayName(usuario);
        String avatarInitial = buildAvatarInitial(displayName, usuario.getEmail());

        return new NavUserView(
            displayName,
            fullName,
            usuario.getEmail(),
            avatarInitial
        );
    }

    private String buildFullName(Usuario usuario) {
        String firstName = safeTrim(usuario.getFirstName());
        String lastName = safeTrim(usuario.getLastName());
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return prettifyEmailLocalPart(usuario.getEmail());
    }

    private String buildDisplayName(Usuario usuario) {
        String firstName = safeTrim(usuario.getFirstName());
        if (!firstName.isBlank()) {
            return formatWord(firstName);
        }
        return prettifyEmailLocalPart(usuario.getEmail());
    }

    private String buildAvatarInitial(String displayName, String email) {
        String source = safeTrim(displayName);
        if (source.isBlank()) {
            source = prettifyEmailLocalPart(email);
        }

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                return String.valueOf(Character.toUpperCase(current));
            }
        }
        return "M";
    }

    private String prettifyEmailLocalPart(String email) {
        String normalized = safeTrim(email);
        int atIndex = normalized.indexOf('@');
        String localPart = atIndex > 0 ? normalized.substring(0, atIndex) : normalized;
        if (localPart.isBlank()) {
            return "Mi cuenta";
        }

        String[] fragments = localPart.split("[._\\-]+");
        StringBuilder builder = new StringBuilder();
        for (String fragment : fragments) {
            String word = formatWord(fragment);
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word);
        }

        return builder.length() == 0 ? "Mi cuenta" : builder.toString();
    }

    private String formatWord(String value) {
        String trimmed = safeTrim(value);
        if (trimmed.isBlank()) {
            return "";
        }

        String lowerCased = trimmed.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowerCased.charAt(0)) + lowerCased.substring(1);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public record NavUserView(
        String displayName,
        String fullName,
        String email,
        String avatarInitial
    ) {}
}
