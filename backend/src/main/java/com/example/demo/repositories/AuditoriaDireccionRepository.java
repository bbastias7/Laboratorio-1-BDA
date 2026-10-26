package com.example.demo.repositories;

import com.example.demo.entities.AuditoriaDireccionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuditoriaDireccionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AuditoriaDireccionEntity> auditoriaDireccionRowMapper = (rs, rowNum) -> new AuditoriaDireccionEntity(
            rs.getInt("id"),
            rs.getInt("paquete_id"),
            rs.getInt("usuario_id"),
            rs.getString("direccion_anterior"),
            rs.getString("direccion_nueva"),
            rs.getObject("fecha", LocalDateTime.class)
    );

    public List<AuditoriaDireccionEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT id, paquete_id, usuario_id, direccion_anterior, direccion_nueva, fecha FROM auditoria_direccion",
                auditoriaDireccionRowMapper
        );
    }

    public Optional<AuditoriaDireccionEntity> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT id, paquete_id, usuario_id, direccion_anterior, direccion_nueva, fecha FROM auditoria_direccion WHERE id = ?",
                    auditoriaDireccionRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public AuditoriaDireccionEntity save(AuditoriaDireccionEntity auditoriaDireccion) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO auditoria_direccion (paquete_id, usuario_id, direccion_anterior, direccion_nueva, fecha) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Integer.class,
                auditoriaDireccion.getPaqueteId(),
                auditoriaDireccion.getUsuarioId(),
                auditoriaDireccion.getDireccionAnterior(),
                auditoriaDireccion.getDireccionNueva(),
                auditoriaDireccion.getFecha()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La inserción no devolvió el ID de la auditoría de dirección");
        }
        auditoriaDireccion.setAuditoriaId(generatedId);
        return auditoriaDireccion;

    }

    public int update(AuditoriaDireccionEntity auditoriaDireccion) {
        return jdbcTemplate.update(
                "UPDATE auditoria_direccion SET paquete_id = ?, usuario_id = ?, direccion_anterior = ?, direccion_nueva = ?, fecha = ? WHERE id = ?",
                auditoriaDireccion.getPaqueteId(),
                auditoriaDireccion.getUsuarioId(),
                auditoriaDireccion.getDireccionAnterior(),
                auditoriaDireccion.getDireccionNueva(),
                auditoriaDireccion.getFecha(),
                auditoriaDireccion.getAuditoriaId()
        );
    }

    public int deleteById(Integer id) {
        return jdbcTemplate.update(
                "DELETE FROM auditoria_direccion WHERE id = ?",
                id
        );
    }

}
