package com.example.demo.repositories;
import com.example.demo.entities.CentroDistribucionEntity;
import com.example.demo.entities.RepartidorEntity;

import com.example.demo.entities.UsuarioEntity;
import com.example.demo.entities.VehiculoEntity;
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
public class RepartidorRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RepartidorEntity> repartidorRowMapper = (rs, rowNum) -> new RepartidorEntity(
            rs.getInt("id"),
            rs.getInt("usuario_id"),
            rs.getInt("centro_id")
        );

    public List<RepartidorEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT id, usuario_id, centro_id FROM repartidor",
                repartidorRowMapper
        );
    }


    public Optional<RepartidorEntity> findById(Integer id) {
        try {
            RepartidorEntity repartidor = jdbcTemplate.queryForObject(
                    "SELECT id, usuario_id, centro_id FROM repartidor WHERE id = ?",
                    repartidorRowMapper, id);
            return Optional.ofNullable(repartidor);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<RepartidorEntity> findByUsuarioId(Integer usuarioId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT id, usuario_id, centro_id FROM repartidor WHERE usuario_id = ?",
                    repartidorRowMapper, usuarioId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public RepartidorEntity save(RepartidorEntity repartidor) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO repartidor (usuario_id, centro_id) VALUES (?, ?) RETURNING id",
                Integer.class,
                repartidor.getUsuarioId(),
                repartidor.getCentroDistId()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La inserción no devolvió el ID del repartidor");
        }
        repartidor.setRepartidorId(generatedId);
        return repartidor;
    }

    public int update(RepartidorEntity repartidor) {
        String sql = "UPDATE repartidor SET centro_id = ? WHERE id = ?";
        return jdbcTemplate.update(
                sql,
                repartidor.getCentroDistId(),
                repartidor.getRepartidorId()
        );
    }

    public int deleteById(Integer id) {
        String sql = "DELETE FROM repartidor WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}

