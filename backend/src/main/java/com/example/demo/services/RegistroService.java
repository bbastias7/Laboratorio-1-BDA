package com.example.demo.services;

import com.example.demo.dto.Dtos.RegistroRequest;
import com.example.demo.entities.UsuarioEntity;
import com.example.demo.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Public client self-registration: Keycloak user first, then the local profile, with compensation. */
@Service
@RequiredArgsConstructor
public class RegistroService {

    private static final String ROL = "cliente";

    private final KeycloakAdminClient keycloak;
    private final UsuarioRepository usuarios;

    public UsuarioEntity registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String nombre = req.nombre().trim();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("nombre: es obligatorio");
        }
        if (usuarios.findByEmail(email).isPresent()) {
            throw new IllegalStateException("El correo ya está registrado");
        }
        String keycloakId;
        try {
            keycloakId = keycloak.crearUsuario(nombre, email, req.password(), ROL);
        } catch (KeycloakAdminClient.UsuarioExistenteException e) {
            throw new IllegalStateException("El correo ya está registrado");
        }
        try {
            return usuarios.save(new UsuarioEntity(null, nombre, email, ROL, keycloakId));
        } catch (RuntimeException e) {
            keycloak.eliminarUsuario(keycloakId);
            throw e;
        }
    }
}
