package com.example.demo.repositories;

import com.example.demo.entities.CentroDistribucionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CentroDistribucionRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<CentroDistribucionEntity> centroDistribucionRowMapper = (rs, rowNum) -> new CentroDistribucionEntity(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("direccion"),
            rs.getString("ciudad")
    );

    public List<CentroDistribucionEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT id, nombre, direccion, ciudad FROM centro_distribucion",
                centroDistribucionRowMapper
        );
    }

    public Optional<CentroDistribucionEntity> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT id, nombre, direccion, ciudad FROM centro_distribucion WHERE id = ?",
                    centroDistribucionRowMapper,
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public CentroDistribucionEntity save(CentroDistribucionEntity centroDistribucion) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO centro_distribucion (nombre, direccion, ciudad) VALUES (?, ?, ?) RETURNING id",
                Integer.class,
                centroDistribucion.getNombre(),
                centroDistribucion.getDireccion(),
                centroDistribucion.getCiudad()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La inserción no devolvió el ID del centro de distribución");
        }
        centroDistribucion.setCentroDistId(generatedId);
        return centroDistribucion;
    }

    public int update(CentroDistribucionEntity centroDistribucion) {
        return jdbcTemplate.update(
                "UPDATE centro_distribucion SET nombre = ?, direccion = ?, ciudad = ? WHERE id = ?",
                centroDistribucion.getNombre(),
                centroDistribucion.getDireccion(),
                centroDistribucion.getCiudad(),
                centroDistribucion.getCentroDistId()
        );
    }

    public int deleteById(Integer id) {
        return jdbcTemplate.update(
                "DELETE FROM centro_distribucion WHERE id = ?",
                id
        );
    }
}
