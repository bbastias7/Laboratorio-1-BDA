package com.example.demo.repositories;
import com.example.demo.entities.UsuarioEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<UsuarioEntity> usuarioRowMapper = (rs, rowNum) -> new UsuarioEntity(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("email"),
            rs.getString("rol")
    );

    private static final String USUARIO_SELECT = "SELECT id, nombre, email, rol FROM usuario";

    public List<UsuarioEntity> findAll() {
        return jdbcTemplate.query(USUARIO_SELECT, usuarioRowMapper);
    }


    public Optional<UsuarioEntity> findById(Integer id) {
        try {
            UsuarioEntity usuario = jdbcTemplate.queryForObject(
                    USUARIO_SELECT + " WHERE id = ?", usuarioRowMapper, id);
            return Optional.ofNullable(usuario);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public Optional<UsuarioEntity> findByEmail(String email) {
        try {
            UsuarioEntity usuario = jdbcTemplate.queryForObject(
                    USUARIO_SELECT + " WHERE email = ?", usuarioRowMapper, email);
            return Optional.ofNullable(usuario);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public UsuarioEntity save(UsuarioEntity usuario) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO usuario (nombre, email, rol) VALUES (?, ?, ?) RETURNING id",
                Integer.class,
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La insercion no devolvio el ID del usuario");
        }
        usuario.setUsuarioId(generatedId);
        return usuario;
    }


    public int update(UsuarioEntity usuario) {
        String sql = "UPDATE usuario SET nombre = ?, email = ?, rol = ? WHERE id = ?";
        return jdbcTemplate.update(
                sql,
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getUsuarioId()
        );
    }


    public int deleteById(Integer id) {
        String sql = "DELETE FROM usuario WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
