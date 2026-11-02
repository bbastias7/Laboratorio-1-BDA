package com.example.demo.controllers;
import com.example.demo.dto.Pagina;
import com.example.demo.config.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.demo.dto.Dtos.UsuarioCrearRequest;
import com.example.demo.entities.UsuarioEntity;
import com.example.demo.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('DESPACHADOR')")
@RequestMapping("/api")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;


    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioEntity>> findAll(
            @RequestParam(required = false) String rol, @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return usuarioService.findPage(rol, q, page, size).responder();
    }


    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioEntity> findById(@PathVariable Integer id) {
        UsuarioEntity usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuario);
    }


    @GetMapping("/usuarios/email")
    public ResponseEntity<UsuarioEntity> findByEmail(@RequestParam String correo) {
        UsuarioEntity usuario = usuarioService.findByEmail(correo);
        return ResponseEntity.ok(usuario);
    }


    @GetMapping("/usuarios/find")
    public ResponseEntity<List<UsuarioEntity>> findByNombre(@RequestParam String nombre) {
        List<UsuarioEntity> usuarios = usuarioService.findByNombre(nombre);
        return ResponseEntity.ok(usuarios);
    }


    @GetMapping("/usuarios/rol/{rol}")
    public ResponseEntity<List<UsuarioEntity>> findByRol(@PathVariable String rol) {
        List<UsuarioEntity> usuarios = usuarioService.findByRol(rol);
        return ResponseEntity.ok(usuarios);
    }


    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioEntity> saveUsuario(@jakarta.validation.Valid @RequestBody UsuarioCrearRequest usuario) {
        UsuarioEntity nuevoUsuario = usuarioService.saveUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }


    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioEntity> updateUsuario(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody UsuarioEntity usuario,
            @AuthenticationPrincipal Jwt jwt) {
        UsuarioEntity usuarioActualizado = usuarioService.updateUsuario(id, usuario, AuthUser.from(jwt).uid());
        return ResponseEntity.ok(usuarioActualizado);
    }


    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.deleteById(id, AuthUser.from(jwt).uid());
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/auth/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioEntity> perfil(
            @AuthenticationPrincipal
            Jwt jwt) {
        return ResponseEntity.ok(usuarioService.findById(AuthUser.from(jwt).uid()));
    }
}
