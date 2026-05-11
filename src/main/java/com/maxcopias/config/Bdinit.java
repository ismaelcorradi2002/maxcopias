package com.maxcopias.config;

import com.maxcopias.model.Rol;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioUsuario;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Crea usuarios iniciales solo cuando las variables de entorno necesarias
 * estan definidas. No sobrescribe usuarios ya existentes.
 */
@Component
public class Bdinit implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bdinit.class);

    private final RepositorioUsuario userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String workerEmail;
    private final String workerPassword;
    private final boolean resetInternalUsersPasswords;

    public Bdinit(
        RepositorioUsuario userRepository,
        PasswordEncoder passwordEncoder,
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
        List<UsuarioSeed> usuariosSeed = new ArrayList<>();

        addSeedIfConfigured(
            usuariosSeed,
            "Admin",
            "Maxcopias",
            adminEmail,
            "600 000 000",
            adminPassword,
            Rol.ROLE_ADMIN,
            "admin"
        );
        addSeedIfConfigured(
            usuariosSeed,
            "Trabajador",
            "Usuario",
            workerEmail,
            "600 000 001",
            workerPassword,
            Rol.ROLE_WORKER,
            "worker"
        );

        if (resetInternalUsersPasswords) {
            resetPasswordIfUserExists(adminEmail, adminPassword, Rol.ROLE_ADMIN, "admin");
            resetPasswordIfUserExists(workerEmail, workerPassword, Rol.ROLE_WORKER, "worker");
        }

        for (UsuarioSeed seed : usuariosSeed) {
            if (userRepository.existsByEmailIgnoreCase(seed.email())) {
                LOGGER.info("Usuario seed ya existe: {}", seed.email());
                continue;
            }

            Usuario usuario = new Usuario();
            usuario.setFirstName(seed.firstName());
            usuario.setLastName(seed.lastName());
            usuario.setEmail(seed.email());
            usuario.setPhone(seed.phone());
            usuario.setPassword(passwordEncoder.encode(seed.password()));
            usuario.setRol(seed.rol());
            usuario.setCreatedAt(LocalDateTime.now());

            userRepository.save(usuario);
            LOGGER.warn("Creado usuario seed inicial para rol {} con email {}", seed.rol().name(), seed.email());
        }
    }

    private void addSeedIfConfigured(
        List<UsuarioSeed> usuariosSeed,
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        Rol rol,
        String etiqueta
    ) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            LOGGER.warn(
                "No se crea el usuario inicial de {} porque faltan variables de entorno requeridas.",
                etiqueta
            );
            return;
        }

        usuariosSeed.add(new UsuarioSeed(
            firstName,
            lastName,
            email.trim(),
            phone,
            password.trim(),
            rol
        ));
    }

    private void resetPasswordIfUserExists(String email, String password, Rol rol, String etiqueta) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            LOGGER.warn(
                "No se puede actualizar la password de {} porque faltan variables de entorno requeridas.",
                etiqueta
            );
            return;
        }

        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(usuario -> {
            usuario.setPassword(passwordEncoder.encode(password.trim()));
            usuario.setRol(rol);
            userRepository.save(usuario);

            if (Rol.ROLE_ADMIN.equals(rol)) {
                LOGGER.warn("Contraseña admin actualizada correctamente");
            } else if (Rol.ROLE_WORKER.equals(rol)) {
                LOGGER.warn("Contraseña worker actualizada correctamente");
            }
        });
    }

    private record UsuarioSeed(
        String firstName,
        String lastName,
        String email,
        String phone,
        String password,
        Rol rol
    ) {}
}
