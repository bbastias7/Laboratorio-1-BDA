package com.example.demo.repositories;

import com.example.demo.entities.RutaPaqueteEntity;
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
public class RutaPaqueteRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RutaPaqueteEntity> rutaPaqueteEntityRowMapper = (rs, rowNum) -> new RutaPaqueteEntity(
            rs.getInt("ruta_id"),
            rs.getInt("paquete_id"),
            rs.getString("resultado"),
            rs.getObject("fecha_resultado", LocalDateTime.class)
    );

    private static final String RUTA_PAQUETE_SELECT =
            "SELECT ruta_id, paquete_id, resultado, fecha_resultado FROM ruta_paquete";

    public List<RutaPaqueteEntity> findAll() {
        return jdbcTemplate.query(RUTA_PAQUETE_SELECT, rutaPaqueteEntityRowMapper);
    }

    public Optional<RutaPaqueteEntity> findByIds(Integer rutaId, Integer paqueteId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    RUTA_PAQUETE_SELECT + " WHERE ruta_id = ? AND paquete_id = ?",
                    rutaPaqueteEntityRowMapper,
                    rutaId,
                    paqueteId
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public RutaPaqueteEntity save(RutaPaqueteEntity rutaPaqueteEntity) {
        if (rutaPaqueteEntity.getResultado() == null) {
            rutaPaqueteEntity.setResultado("Pendiente");
        }
        jdbcTemplate.update(
                "INSERT INTO ruta_paquete (ruta_id, paquete_id, resultado, fecha_resultado) VALUES (?, ?, ?, ?)",
                rutaPaqueteEntity.getRutaId(),
                rutaPaqueteEntity.getPaqueteId(),
                rutaPaqueteEntity.getResultado(),
                rutaPaqueteEntity.getFechaResultado()
        );
        return rutaPaqueteEntity;
    }

    public int update(RutaPaqueteEntity rutaPaqueteEntity) {
        return jdbcTemplate.update(
                "UPDATE ruta_paquete SET resultado = ?, fecha_resultado = ? WHERE paquete_id = ? AND ruta_id = ?",
                rutaPaqueteEntity.getResultado(),
                rutaPaqueteEntity.getFechaResultado(),
                rutaPaqueteEntity.getPaqueteId(),
                rutaPaqueteEntity.getRutaId()
        );
    }

    public int deleteByIds(Integer rutaId, Integer paqueteId) {
        return jdbcTemplate.update(
                "DELETE FROM ruta_paquete WHERE ruta_id = ? AND paquete_id = ?",
                rutaId,
                paqueteId
        );
    }

    public List<RutaPaqueteEntity> findByRutaId(Integer rutaId) {
        return jdbcTemplate.query(RUTA_PAQUETE_SELECT + " WHERE ruta_id = ? ORDER BY paquete_id",
                rutaPaqueteEntityRowMapper, rutaId);
    }

    public List<RutaPaqueteEntity> findByPaqueteId(Integer paqueteId) {
        return jdbcTemplate.query(
                "SELECT rp.ruta_id, rp.paquete_id, rp.resultado, rp.fecha_resultado " +
                        "FROM ruta_paquete rp JOIN ruta r ON r.id = rp.ruta_id " +
                        "WHERE rp.paquete_id = ? ORDER BY r.fecha, r.id",
                rutaPaqueteEntityRowMapper, paqueteId);
    }

    public boolean existsAsignacionPendiente(Integer paqueteId, Integer repartidorId) {
        Boolean existe = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ruta_paquete rp " +
                        "JOIN ruta r ON r.id = rp.ruta_id " +
                        "WHERE rp.paquete_id = ? AND r.repartidor_id = ? AND rp.resultado = 'Pendiente')",
                Boolean.class, paqueteId, repartidorId);
        return Boolean.TRUE.equals(existe);
    }


}
