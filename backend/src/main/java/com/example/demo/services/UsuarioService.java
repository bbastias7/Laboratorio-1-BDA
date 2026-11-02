package com.example.demo.services;

import com.example.demo.dto.Dtos.UsuarioCrearRequest;
import com.example.demo.dto.Pagina;
import com.example.demo.entities.UsuarioEntity;
import com.example.demo.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final KeycloakAdminClient keycloak;


    public List<UsuarioEntity> findAll() {
        return usuarioRepository.findAll();
    }


    public Pagina<UsuarioEntity> findPage(String rol, String q, Integer page, Integer size) {
        if (rol != null && !rol.isBlank() && !java.util.Set.of("cliente", "repartidor", "despachador").contains(rol))
            throw new IllegalArgumentException("Rol no válido");
        return usuarioRepository.findPage(rol, q, page, size);
    }


    public UsuarioEntity findById(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + id));
    }


    public UsuarioEntity findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con email: " + email));
    }


    public List<UsuarioEntity> findByNombre(String nombre) {
        return usuarioRepository.findByNombre(nombre);
    }


    public List<UsuarioEntity> findByRol(String rol) {
        return usuarioRepository.findByRol(rol);
    }


    /**
     * Creates a profile. With a password the Keycloak account is created first (temporary password, realm role) and
     * deleted again if the insert fails; with a keycloakId an existing account is linked and Keycloak is not touched.
     */
    public UsuarioEntity saveUsuario(UsuarioCrearRequest req) {
        boolean conPassword = req.password() != null && !req.password().isBlank();
        boolean conKeycloakId = req.keycloakId() != null && !req.keycloakId().isBlank();
        if (conPassword == conKeycloakId) {
            throw new IllegalArgumentException("Envíe la contraseña temporal o el identificador de Keycloak, pero no ambos ni ninguno");
        }
        String nombre = req.nombre().trim();
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("nombre: es obligatorio");
        }
        if (conKeycloakId) {
            return guardar(new UsuarioEntity(null, nombre, req.email(), req.rol(), req.keycloakId()));
        }
        if (req.password().length() < 8) {
            throw new IllegalArgumentException("password: debe tener entre 8 y 128 caracteres");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        verificarEmailLibre(email, null);
        String keycloakId;
        try {
            keycloakId = keycloak.crearUsuario(nombre, email, req.password(), req.rol(), true);
        } catch (KeycloakAdminClient.UsuarioExistenteException e) {
            throw new IllegalStateException("El email ya se encuentra registrado");
        }
        try {
            return usuarioRepository.save(new UsuarioEntity(null, nombre, email, req.rol(), keycloakId));
        } catch (RuntimeException e) {
            keycloak.eliminarUsuario(keycloakId);
            throw e;
        }
    }

    private UsuarioEntity guardar(UsuarioEntity usuario) {
        verificarEmailLibre(usuario.getEmail(), null);
        return usuarioRepository.save(usuario);
    }

    private void verificarEmailLibre(String email, Integer propioId) {
        usuarioRepository.findByEmail(email).ifPresent(u -> {
            if (!u.getUsuarioId().equals(propioId)) {
                throw new IllegalStateException("El email ya se encuentra registrado");
            }
        });
    }

    /**
     * Updates nombre/email/rol. Keycloak is changed first and rolled back if the database update fails, so a failure
     * never leaves the account and the profile disagreeing. {@code actorId} is the caller's own profile id.
     */
    public UsuarioEntity updateUsuario(Integer id, UsuarioEntity detalles, Integer actorId) {
        UsuarioEntity actual = findById(id);
        if (detalles.getKeycloakId() != null && !detalles.getKeycloakId().equals(actual.getKeycloakId()))
            throw new IllegalArgumentException("keycloakId es inmutable");
        boolean cambiaRol = !actual.getRol().equals(detalles.getRol());
        if (cambiaRol && id.equals(actorId)) {
            throw new IllegalStateException("No puede cambiar su propio rol");
        }
        String nombre = detalles.getNombre().trim();
        String email = detalles.getEmail().trim();
        boolean cambiaDatos = !actual.getNombre().equals(nombre) || !actual.getEmail().equalsIgnoreCase(email);
        if (!actual.getEmail().equals(email)) {
            verificarEmailLibre(email, id);
        }
        String correoKeycloak = email.toLowerCase(Locale.ROOT);
        boolean datosEnKeycloak = false;
        boolean rolEnKeycloak = false;
        try {
            if (cambiaDatos) {
                try {
                    datosEnKeycloak = keycloak.actualizarUsuario(actual.getKeycloakId(), nombre, correoKeycloak);
                } catch (KeycloakAdminClient.UsuarioExistenteException e) {
                    throw new IllegalStateException("El email ya se encuentra registrado");
                }
            }
            if (cambiaRol) {
                rolEnKeycloak = keycloak.cambiarRol(actual.getKeycloakId(), actual.getRol(), detalles.getRol());
            }
            UsuarioEntity nuevo = new UsuarioEntity(id, nombre, email, detalles.getRol(), actual.getKeycloakId());
            if (usuarioRepository.update(nuevo) == 0) {
                throw new IllegalStateException("No se pudo actualizar el usuario");
            }
            return nuevo;
        } catch (RuntimeException e) {
            revertirKeycloak(actual, datosEnKeycloak, rolEnKeycloak, detalles.getRol());
            throw e;
        }
    }

    private void revertirKeycloak(UsuarioEntity original, boolean datos, boolean rol, String rolNuevo) {
        try {
            if (datos) {
                keycloak.actualizarUsuario(original.getKeycloakId(), original.getNombre(),
                        original.getEmail().toLowerCase(Locale.ROOT));
            }
            if (rol) {
                keycloak.cambiarRol(original.getKeycloakId(), rolNuevo, original.getRol());
            }
        } catch (RuntimeException ex) {
            log.error("Could not roll back Keycloak user {}: {}", original.getKeycloakId(), ex.getClass().getSimpleName());
        }
    }

    /** Deletes the profile, then the Keycloak account (best effort: a failure there is only logged). */
    public void deleteById(Integer id, Integer actorId) {
        if (id.equals(actorId)) {
            throw new IllegalStateException("No puede eliminar su propia cuenta");
        }
        UsuarioEntity usuario = findById(id);
        int filasAfectadas = usuarioRepository.deleteById(id);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el usuario");
        }
        if (!keycloak.eliminarUsuario(usuario.getKeycloakId())) {
            log.warn("Profile {} deleted but its Keycloak account {} could not be removed", id, usuario.getKeycloakId());
        }
    }
}
