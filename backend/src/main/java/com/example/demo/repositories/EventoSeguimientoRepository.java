package com.example.demo.repositories;

import com.example.demo.entities.EventoSeguimientoEntity;
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
public class EventoSeguimientoRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<EventoSeguimientoEntity> eventoSeguimientoRowMapper = (rs, rowNum) -> new EventoSeguimientoEntity(
            rs.getInt("id"),
            rs.getInt("paquete_id"),
            rs.getString("estado"),
            rs.getString("descripcion"),
            rs.getObject("fecha", LocalDateTime.class)
    );

    public List<EventoSeguimientoEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT id, paquete_id, estado, descripcion, fecha FROM evento_seguimiento",
                eventoSeguimientoRowMapper
        );
    }

    public Optional<EventoSeguimientoEntity> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT id, paquete_id, estado, descripcion, fecha FROM evento_seguimiento WHERE id = ?",
                    eventoSeguimientoRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public EventoSeguimientoEntity save(EventoSeguimientoEntity eventoSeguimiento) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO evento_seguimiento (paquete_id, estado, descripcion, fecha) VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class,
                eventoSeguimiento.getPaqueteId(),
                eventoSeguimiento.getEstado(),
                eventoSeguimiento.getDescripcion(),
                eventoSeguimiento.getFecha()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La inserción no devolvió el ID del evento de seguimiento");
        }
        eventoSeguimiento.setEventoId(generatedId);
        return eventoSeguimiento;
    }

    public int update(EventoSeguimientoEntity eventoSeguimiento) {
        return jdbcTemplate.update(
                "UPDATE evento_seguimiento SET paquete_id = ?, estado = ?, descripcion = ?, fecha = ? WHERE id = ?",
                eventoSeguimiento.getPaqueteId(),
                eventoSeguimiento.getEstado(),
                eventoSeguimiento.getDescripcion(),
                eventoSeguimiento.getFecha(),
                eventoSeguimiento.getEventoId()
        );
    }

    public int deleteById(Integer id) {
        return jdbcTemplate.update(
                "DELETE FROM evento_seguimiento WHERE id = ?",
                id
        );
    }

}
