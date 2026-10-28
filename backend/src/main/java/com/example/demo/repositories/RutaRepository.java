package com.example.demo.repositories;

import com.example.demo.entities.RutaEntity;
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
public class RutaRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RutaEntity> rutaEntityRowMapper = (rs, rowNum) -> new RutaEntity(
            rs.getInt("id"),
            rs.getInt("centro_id"),
            rs.getInt("repartidor_id"),
            rs.getInt("vehiculo_id"),
            rs.getObject("fecha", LocalDateTime.class)
    );

    private static final String RUTA_SELECT = "SELECT id, centro_id, repartidor_id, vehiculo_id, fecha FROM ruta";

    public List<RutaEntity> findAll() {
        return jdbcTemplate.query(RUTA_SELECT, rutaEntityRowMapper);
    }

    public Optional<RutaEntity> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    RUTA_SELECT + " WHERE id = ?", rutaEntityRowMapper, id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<RutaEntity> findByRepartidorId(Integer repartidorId) {
        return jdbcTemplate.query(RUTA_SELECT + " WHERE repartidor_id = ? ORDER BY fecha DESC, id DESC",
                rutaEntityRowMapper, repartidorId);
    }

    public RutaEntity save(RutaEntity rutaEntity) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO ruta (centro_id, repartidor_id, vehiculo_id, fecha) VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class,
                rutaEntity.getCentroDistId(),
                rutaEntity.getRepartidorId(),
                rutaEntity.getVehiculoId(),
                rutaEntity.getFecha()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La inserción no devolvió el ID de la ruta");
        }
        rutaEntity.setRutaId(generatedId);
        return rutaEntity;
    }

    public int update(RutaEntity rutaEntity) {
        return jdbcTemplate.update(
                "UPDATE ruta SET centro_id = ?, repartidor_id = ?, vehiculo_id = ?, fecha = ? WHERE id = ?",
                rutaEntity.getCentroDistId(),
                rutaEntity.getRepartidorId(),
                rutaEntity.getVehiculoId(),
                rutaEntity.getFecha(),
                rutaEntity.getRutaId()
        );
    }

    public int deleteById(Integer id) {
        return jdbcTemplate.update(
                "DELETE FROM ruta WHERE id = ?",
                id
        );
    }

}
