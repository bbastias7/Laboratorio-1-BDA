package com.example.demo.services;

import com.example.demo.entities.UsuarioEntity;
import com.example.demo.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;


    public List<UsuarioEntity> findAll() {
        return usuarioRepository.findAll();
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


    @Transactional
    public UsuarioEntity saveUsuario(UsuarioEntity usuario) {
        usuarioRepository.findByEmail(usuario.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("El email ya se encuentra registrado");
        });

        return usuarioRepository.save(usuario);
    }


    @Transactional
    public UsuarioEntity updateUsuario(Integer id, UsuarioEntity usuarioDetalles) {
        UsuarioEntity usuarioExistente = findById(id);
        usuarioExistente.setNombre(usuarioDetalles.getNombre());
        usuarioExistente.setEmail(usuarioDetalles.getEmail());
        usuarioExistente.setRol(usuarioDetalles.getRol());
        if (usuarioDetalles.getKeycloakId() != null && !usuarioDetalles.getKeycloakId().equals(usuarioExistente.getKeycloakId()))
            throw new IllegalArgumentException("keycloakId es inmutable");
        int filasAfectadas = usuarioRepository.update(usuarioExistente);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar el usuario");
        }
        return usuarioExistente;
    }


    @Transactional
    public void deleteById(Integer id) {
        int filasAfectadas = usuarioRepository.deleteById(id);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el usuario");
        }
    }
}
