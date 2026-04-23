package com.maxcopias.config;

/**
 * Configuración de seguridad Spring Security para la app.
 */
import com.maxcopias.service.ServicioDetallesUsuario;
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
                .requestMatchers("/", "/contacto", "/tienda", "/detalles-producto/**", "/api/tienda/**", "/login", "/register", "/register/check-email", "/error", "/css/**", "/js/**", "/images/**").permitAll()
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
                .requestMatchers("/dashboard", "/area-personal", "/mis-pedidos", "/pedido", "/copisteria", "/copisteria/**").hasAnyRole("USER", "ADMIN", "WORKER")
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
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean wantsCopisteria = "true".equalsIgnoreCase(request.getParameter("copisteriaRequired"));
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            boolean isWorker = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_WORKER".equals(authority.getAuthority()));

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
}


