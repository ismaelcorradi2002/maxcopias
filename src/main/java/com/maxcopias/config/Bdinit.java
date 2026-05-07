package com.maxcopias.config;

import com.maxcopias.model.Rol;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioUsuario;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador general de datos de usuarios (bdinit).
 * Crea usuarios seed si no existen. Expansible para más usuarios.
 */
@Component
@Profile("dev")
public class Bdinit implements CommandLineRunner {

    private final RepositorioUsuario userRepository;
    private final PasswordEncoder passwordEncoder;

    public Bdinit(RepositorioUsuario userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Lista de usuarios seed - Añade más aquí en el futuro
        List<UsuarioSeed> usuariosSeed = List.of(
            new UsuarioSeed(
"Admin", 
                "Maxcopias", 
                "admin@gmail.com", 
                "600 000 000", 
                "admin",
                Rol.ROLE_ADMIN
            ),
            new UsuarioSeed(
                "Carlos", 
                "Usuario", 
                "carlos@gmail.com", 
                "600 000 001", 
                "carlos",
                Rol.ROLE_USER
            ),
            new UsuarioSeed(
                "Trabajador", 
                "Usuario", 
                "worker@gmail.com", 
                "600 000 001", 
                "worker",
                Rol.ROLE_WORKER
            )
            );

        for (UsuarioSeed seed : usuariosSeed) {
            if (!userRepository.existsByEmailIgnoreCase(seed.email)) {
                Usuario usuario = new Usuario();
                usuario.setFirstName(seed.firstName);
                usuario.setLastName(seed.lastName);
                usuario.setEmail(seed.email);
                usuario.setPhone(seed.phone);
                usuario.setPassword(passwordEncoder.encode(seed.password));
                usuario.setRol(seed.rol);
                usuario.setCreatedAt(LocalDateTime.now()); // Forzar fecha

                userRepository.save(usuario);
                System.out.println("✅ Creado usuario seed: " + seed.email);
            } else {
                System.out.println("ℹ️ Usuario seed ya existe: " + seed.email);
            }
        }
    }

    // Record auxiliar para datos seed (Java 17+ compatible)
    private record UsuarioSeed(String firstName, String lastName, String email, String phone, String password, Rol rol) {}
}
