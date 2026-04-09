package com.maxcopias.service;

import com.maxcopias.dto.FormularioActualizarPerfil;
import com.maxcopias.dto.FormularioRegistro;
import com.maxcopias.model.Rol;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioUsuario;
import java.util.Optional;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ServicioUsuario {

    private final RepositorioUsuario userRepository;
    private final PasswordEncoder passwordEncoder;

    public ServicioUsuario(RepositorioUsuario userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registerUsuario(FormularioRegistro form) {
        String normalizedEmail = normalizeEmail(form.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ExcepcionRegistroUsuario("Ya existe una cuenta registrada con ese email.");
        }

        Usuario user = new Usuario();
        user.setFirstName(normalizeText(form.getFirstName()));
        user.setLastName(normalizeText(form.getLastName()));
        user.setEmail(normalizedEmail);
        user.setPhone(normalizeText(form.getPhone()));
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRol(Rol.ROLE_USER);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public Usuario findRequiredByEmail(String email) {
        return findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No se ha encontrado el usuario autenticado."));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional
    public Usuario updateProfile(String currentEmail, FormularioActualizarPerfil form) {
        Usuario user = findRequiredByEmail(currentEmail);
        user.setFirstName(normalizeText(form.getFirstName()));
        user.setLastName(normalizeText(form.getLastName()));
        user.setPhone(normalizeText(form.getPhone()));
        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}

