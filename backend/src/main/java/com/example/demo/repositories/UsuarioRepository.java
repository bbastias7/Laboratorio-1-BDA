package com.example.demo.repositories;
import com.example.demo.entities.Usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Usuario> usuarioRowMapper = (rs, rowNum) -> new Usuario(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getString("rol")
    );


    public List<Usuario> findAll() {
        String sql = "SELECT id, nombre, email, password, rol FROM usuario";
        return jdbcTemplate.query(sql, usuarioRowMapper);
    }


    public Optional<Usuario> findById(Integer id) {
        String sql = "SELECT id, nombre, email, password, rol FROM usuario WHERE id = ?";
        try {
            Usuario usuario = jdbcTemplate.queryForObject(sql, usuarioRowMapper, id);
            return Optional.ofNullable(usuario);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public Optional<Usuario> findByEmail(String email) {
        String sql = "SELECT id, nombre, email, password, rol FROM usuario WHERE email = ?";
        try {
            Usuario usuario = jdbcTemplate.queryForObject(sql, usuarioRowMapper, email);
            return Optional.ofNullable(usuario);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public Usuario save(Usuario usuario) {
        String sql = "INSERT INTO usuario (nombre, email, password, rol) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getEmail());
            ps.setString(3, usuario.getPassword());
            ps.setString(4, usuario.getRol());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            usuario.setId((Integer) keyHolder.getKeys().get("id"));
        }

        return usuario;
    }


    public int update(Usuario usuario) {
        String sql = "UPDATE usuario SET nombre = ?, email = ?, password = ?, rol = ? WHERE id = ?";
        return jdbcTemplate.update(
                sql,
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getRol(),
                usuario.getId()
        );
    }


    public int deleteById(Integer id) {
        String sql = "DELETE FROM usuario WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}