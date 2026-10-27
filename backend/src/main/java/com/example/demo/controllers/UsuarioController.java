package com.example.demo.controllers;

import com.example.demo.entities.UsuarioEntity;
import com.example.demo.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;


    @GetMapping
    public ResponseEntity<List<UsuarioEntity>> findAll() {
        List<UsuarioEntity> usuarios = usuarioService.findAll();
        return ResponseEntity.ok(usuarios);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UsuarioEntity> findById(@PathVariable Integer id) {
        UsuarioEntity usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuario);
    }


    @GetMapping("/email")
    public ResponseEntity<UsuarioEntity> findByEmail(@RequestParam String correo) {
        UsuarioEntity usuario = usuarioService.findByEmail(correo);
        return ResponseEntity.ok(usuario);
    }


    @GetMapping("/find")
    public ResponseEntity<List<UsuarioEntity>> findByNombre(@RequestParam String nombre) {
        List<UsuarioEntity> usuarios = usuarioService.findByNombre(nombre);
        return ResponseEntity.ok(usuarios);
    }


    @GetMapping("/rol/{rol}")
    public ResponseEntity<List<UsuarioEntity>> findByRol(@PathVariable String rol) {
        List<UsuarioEntity> usuarios = usuarioService.findByRol(rol);
        return ResponseEntity.ok(usuarios);
    }


    @PostMapping
    public ResponseEntity<UsuarioEntity> saveUsuario(@RequestBody UsuarioEntity usuario) {
        UsuarioEntity nuevoUsuario = usuarioService.saveUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }


    @PutMapping("/{id}")
    public ResponseEntity<UsuarioEntity> updateUsuario(
            @PathVariable Integer id,
            @RequestBody UsuarioEntity usuario) {
        UsuarioEntity usuarioActualizado = usuarioService.updateUsuario(id, usuario);
        return ResponseEntity.ok(usuarioActualizado);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable Integer id) {
        usuarioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}