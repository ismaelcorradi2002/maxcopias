package com.maxcopias.config;

import com.maxcopias.model.Rol;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioUsuario;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Crea usuarios iniciales solo cuando las variables de entorno necesarias
 * estan definidas. No sobrescribe usuarios ya existentes salvo reset temporal
 * explicito de passwords internas.
 */
@Component
public class Bdinit implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bdinit.class);

    private final RepositorioUsuario userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String workerEmail;
    private final String workerPassword;
    private final boolean resetInternalUsersPasswords;

    public Bdinit(
        RepositorioUsuario userRepository,
        BCryptPasswordEncoder passwordEncoder,
        @Value("${maxcopias.bootstrap.admin-email:}") String adminEmail,
        @Value("${maxcopias.bootstrap.admin-password:}") String adminPassword,
        @Value("${maxcopias.bootstrap.worker-email:}") String workerEmail,
        @Value("${maxcopias.bootstrap.worker-password:}") String workerPassword,
        @Value("${maxcopias.bootstrap.reset-internal-users-passwords:false}") boolean resetInternalUsersPasswords
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.workerEmail = workerEmail;
        this.workerPassword = workerPassword;
        this.resetInternalUsersPasswords = resetInternalUsersPasswords;
    }

    @Override
    public void run(String... args) {
        processSeed(new UsuarioSeed(
            "Admin",
            "Maxcopias",
            adminEmail,
            "600 000 000",
            adminPassword,
            Rol.ROLE_ADMIN,
            "admin"
        ));
        processSeed(new UsuarioSeed(
            "Trabajador",
            "Usuario",
            workerEmail,
            "600 000 001",
            workerPassword,
            Rol.ROLE_WORKER,
            "worker"
        ));
    }

    private void processSeed(UsuarioSeed seed) {
        if (!hasRequiredCredentials(seed)) {
            LOGGER.warn(
                "No se crea el usuario inicial de {} porque faltan variables de entorno requeridas.",
                seed.etiqueta()
            );
            return;
        }

        String normalizedEmail = seed.email().trim();
        String normalizedPassword = seed.password().trim();

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(usuario -> {
            if (!resetInternalUsersPasswords) {
                LOGGER.info("Usuario seed ya existe: {}", normalizedEmail);
                return;
            }

            usuario.setPassword(passwordEncoder.encode(normalizedPassword));
            userRepository.save(usuario);
            logReset(seed.etiqueta());
        }, () -> createUser(seed, normalizedEmail, normalizedPassword));
    }

    private boolean hasRequiredCredentials(UsuarioSeed seed) {
        return StringUtils.hasText(seed.email()) && StringUtils.hasText(seed.password());
    }

    private void createUser(UsuarioSeed seed, String normalizedEmail, String normalizedPassword) {
        Usuario usuario = new Usuario();
        usuario.setFirstName(seed.firstName());
        usuario.setLastName(seed.lastName());
        usuario.setEmail(normalizedEmail);
        usuario.setPhone(seed.phone());
        usuario.setPassword(passwordEncoder.encode(normalizedPassword));
        usuario.setRol(seed.rol());
        usuario.setCreatedAt(LocalDateTime.now());

        userRepository.save(usuario);
        LOGGER.warn("Creado usuario seed inicial para rol {} con email {}", seed.rol().name(), normalizedEmail);
    }

    private void logReset(String etiqueta) {
        if ("admin".equals(etiqueta)) {
            LOGGER.warn("Password de admin actualizada por reset temporal");
        } else if ("worker".equals(etiqueta)) {
            LOGGER.warn("Password de worker actualizada por reset temporal");
        }
    }

    private record UsuarioSeed(
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        Rol rol,
        String etiqueta
    ) {}
}
