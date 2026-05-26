package com.maxcopias.config;

/**
 * Configuración de seguridad Spring Security para la app.
 */
import com.maxcopias.service.ServicioDetallesUsuario;
import com.maxcopias.service.ServicioCarrito;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class ConfiguracionSeguridad {

    /**
     * Configura reglas de acceso, login y logout.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthenticationProvider authenticationProvider,
        AuthenticationSuccessHandler authenticationSuccessHandler
    ) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/contacto", "/tienda", "/ofertas", "/detalles-producto/**", "/api/tienda/**", "/carrito", "/carrito/**", "/login", "/register", "/register/check-email", "/error", "/aviso-legal", "/privacidad", "/cookies", "/condiciones-compra", "/robots.txt", "/sitemap.xml", "/css/**", "/js/**", "/images/**", "/media/productos/**", "/media/ofertas/**").permitAll()
                .requestMatchers(
                    "/admin/api/products",
                    "/admin/api/categorias",
                    "/admin/crear-producto",
                    "/admin/crear-categoria",
                    "/admin/update-producto/**",
                    "/editarstock/**"
                ).hasAnyRole("ADMIN", "WORKER")
                .requestMatchers("/worker/**").hasAnyRole("WORKER", "ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/dashboard", "/area-personal", "/mis-pedidos", "/pedido", "/copisteria", "/copisteria/**", "/pedidos/copisteria/**", "/checkout", "/pedido-confirmado/**").hasAnyRole("USER", "ADMIN", "WORKER")
                .anyRequest().authenticated()
            )
            // Configura login y logout
            .formLogin(login -> login
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    /**
     * Proveedor de autenticación con BD.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
        ServicioDetallesUsuario userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Redirige a Home (todos los roles iguales). Soporta ?copisteriaRequired=true.
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(ServicioCarrito servicioCarrito) {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

        return (request, response, authentication) -> {
            boolean wantsCopisteria = "true".equalsIgnoreCase(request.getParameter("copisteriaRequired"));
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            boolean isWorker = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_WORKER".equals(authority.getAuthority()));
            SavedRequest savedRequest = requestCache.getRequest(request, response);

            servicioCarrito.sincronizarTrasLogin(authentication, request.getSession(false));

            if (savedRequest != null && shouldRedirectToSavedRequest(savedRequest, request, response)) {
                return;
            }

            if (wantsCopisteria) {
                response.sendRedirect("/copisteria");
                return;
            }

            if (isAdmin) {
                response.sendRedirect("/admin");
                return;
            }

            if (isWorker) {
                response.sendRedirect("/worker");
                return;
            }

            response.sendRedirect("/");
        };
    }

    private boolean shouldRedirectToSavedRequest(
        SavedRequest savedRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws java.io.IOException {
        String redirectUrl = savedRequest.getRedirectUrl();
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return false;
        }

        String contextPath = request.getContextPath();
        String path = redirectUrl;
        int schemeIndex = redirectUrl.indexOf("://");
        if (schemeIndex >= 0) {
            int pathStart = redirectUrl.indexOf('/', schemeIndex + 3);
            path = pathStart >= 0 ? redirectUrl.substring(pathStart) : "/";
        }

        if (path.startsWith(contextPath + "/checkout") || path.startsWith(contextPath + "/carrito")) {
            response.sendRedirect(path);
            return true;
        }

        return false;
    }
}


