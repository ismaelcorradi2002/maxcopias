package com.maxcopias.service;

import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioUsuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final RepositorioUsuario userRepository;

    public ServicioDetallesUsuario(RepositorioUsuario userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("No existe una cuenta con ese email."));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            java.util.List.of(new SimpleGrantedAuthority(user.getRol().name()))
        );
    }
}


