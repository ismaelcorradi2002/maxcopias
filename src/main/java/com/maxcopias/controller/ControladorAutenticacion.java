package com.maxcopias.controller;

/**
 * Controlador de login y registro de usuarios.
 */
import com.maxcopias.dto.FormularioRegistro;
import com.maxcopias.model.Rol;
import com.maxcopias.service.ExcepcionRegistroUsuario;
import com.maxcopias.service.ServicioUsuario;
import com.maxcopias.service.ServicioCorreoBienvenida;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ControladorAutenticacion {
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final ServicioUsuario userService;
    private final ServicioCorreoBienvenida welcomeEmailService;

    /**
     * Inyecta servicios de usuario y email bienvenida.
     */
    public ControladorAutenticacion(ServicioUsuario userService, ServicioCorreoBienvenida welcomeEmailService) {
        this.userService = userService;
        this.welcomeEmailService = welcomeEmailService;
    }

    /**
     * Muestra formulario de registro.
     */
    @GetMapping("/register")
    public String showRegister(
        Model model,
        Authentication authentication,
        @RequestParam(name = "copisteriaRequired", defaultValue = "false") boolean copisteriaRequired
    ) {
        if (isAuthenticated(authentication)) {
            return redirectByRol(authentication, copisteriaRequired);
        }

        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new FormularioRegistro());
        }

        model.addAttribute("copisteriaRequired", copisteriaRequired);

        return "autenticacion/registro";
    }

    /**
     * Procesa registro de nuevo usuario + envía email bienvenida.
     */
    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registerForm") FormularioRegistro registerForm,
        BindingResult bindingResult,
        @RequestParam(name = "copisteriaRequired", defaultValue = "false") boolean copisteriaRequired,
        Model model
    ) {
        if (!registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Las contrasenas no coinciden.");
        }

        if (userService.emailExists(registerForm.getEmail())) {
            bindingResult.rejectValue("email", "email.exists", "Ya existe una cuenta registrada con ese email.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("copisteriaRequired", copisteriaRequired);
            return "autenticacion/registro";
        }

        try {
            var createdUsuario = userService.registerUsuario(registerForm);
            welcomeEmailService.sendWelcomeEmail(createdUsuario);
            return copisteriaRequired
                ? "redirect:/login?registered&copisteriaRequired=true"
                : "redirect:/login?registered";
        } catch (ExcepcionRegistroUsuario exception) {
            bindingResult.rejectValue("email", "email.exists", exception.getMessage());
            model.addAttribute("copisteriaRequired", copisteriaRequired);
            return "autenticacion/registro";
        }
    }

    @GetMapping("/login")
    public String showLogin(
        Authentication authentication,
        @RequestParam(name = "copisteriaRequired", defaultValue = "false") boolean copisteriaRequired
    ) {
        if (isAuthenticated(authentication)) {
            return redirectByRol(authentication, copisteriaRequired);
        }

        return "autenticacion/iniciar-sesion";
    }

    @GetMapping("/register/check-email")
    @ResponseBody
    public Map<String, Object> checkEmailAvailability(@RequestParam(name = "email", defaultValue = "") String email) {
        boolean hasText = email != null && !email.trim().isEmpty();
        boolean validFormat = hasText && email.trim().matches(EMAIL_REGEX);
        boolean available = hasText && validFormat && !userService.emailExists(email);

        return Map.of(
            "available", available,
            "message", available
                ? "Email disponible."
                : (!hasText
                    ? "Introduce un email para comprobarlo."
                    : (validFormat
                        ? "Ya existe una cuenta con este email."
                        : "Introduce un email valido."))
        );
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String redirectByRol(Authentication authentication, boolean copisteriaRequired) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(Rol.ROLE_ADMIN.name()));

        if (isAdmin) {
            return "redirect:/admin";
        }

        return copisteriaRequired ? "redirect:/copisteria" : "redirect:/";
    }
}

