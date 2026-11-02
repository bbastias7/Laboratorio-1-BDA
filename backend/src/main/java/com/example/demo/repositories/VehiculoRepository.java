package com.example.demo.repositories;
import com.example.demo.dto.Pagina;
import com.example.demo.entities.VehiculoEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class VehiculoRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<VehiculoEntity> vehiculoEntityRowMapper = (rs, rowNum) ->
            new VehiculoEntity(
                    rs.getInt("id"),
                    rs.getInt("centro_id"),
                    rs.getString("patente"),
                    rs.getBigDecimal("capacidad_peso"),
                    rs.getBigDecimal("capacidad_volumen")
            );

    private static final String VEHICULO_SELECT = """
            SELECT v.id, v.centro_id, v.patente, v.capacidad_peso, v.capacidad_volumen
            FROM vehiculo v
            """;

    public List<VehiculoEntity> findAll() {
        String sql = VEHICULO_SELECT;
        return jdbcTemplate.query(sql, vehiculoEntityRowMapper);
    }


    /** Filters: centroId; q = contains over patente. */
    public Pagina<VehiculoEntity> findPage(Integer centroId, String q, Integer page, Integer size) {
        String sql = VEHICULO_SELECT + " WHERE TRUE";
        List<Object> args = new java.util.ArrayList<>();
        if (centroId != null) { sql += " AND v.centro_id = ?"; args.add(centroId); }
        if (Consulta.hay(q)) { sql += " AND v.patente ILIKE ? ESCAPE '\\'"; args.add(Consulta.contiene(q)); }
        return Consulta.listar(jdbcTemplate, sql, args, "v.id", vehiculoEntityRowMapper, page, size);
    }

    public Optional<VehiculoEntity> findById(Integer id) {
        String sql = VEHICULO_SELECT + " WHERE v.id = ?";
        try {
            VehiculoEntity vehiculo = jdbcTemplate.queryForObject(sql, vehiculoEntityRowMapper, id);
            return Optional.ofNullable(vehiculo);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public Optional<VehiculoEntity> findByPatente(String patente) {
        String sql = VEHICULO_SELECT + " WHERE v.patente = ?";
        try {
            VehiculoEntity vehiculo = jdbcTemplate.queryForObject(sql, vehiculoEntityRowMapper, patente);
            return Optional.ofNullable(vehiculo);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public List<VehiculoEntity> findByCapacidadPeso(BigDecimal capacidadPeso) {
        String sql = VEHICULO_SELECT + " WHERE v.capacidad_peso = ?";
        return jdbcTemplate.query(sql, vehiculoEntityRowMapper, capacidadPeso);
    }


    public List<VehiculoEntity> findByCapacidadVolumen(BigDecimal volumen) {
        String sql = VEHICULO_SELECT + " WHERE v.capacidad_volumen = ?";
        return jdbcTemplate.query(sql, vehiculoEntityRowMapper, volumen);
    }


    public VehiculoEntity save(VehiculoEntity vehiculo) {
        validarCentro(vehiculo);
        Integer generatedId = jdbcTemplate.queryForObject(
                """
                INSERT INTO vehiculo (patente, capacidad_peso, capacidad_volumen, centro_id)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                vehiculo.getPatente(),
                vehiculo.getCapacidadPeso(),
                vehiculo.getCapacidadVolumen(),
                vehiculo.getCentroDistId()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La insercion no devolvio el ID del vehiculo");
        }
        vehiculo.setVehiculoId(generatedId);
        return vehiculo;
    }


    public int update(VehiculoEntity vehiculo) {
        validarCentro(vehiculo);
        String sql = "UPDATE vehiculo SET patente = ?, capacidad_peso = ?, capacidad_volumen = ?, centro_id = ? WHERE id = ?";
        return jdbcTemplate.update(
                sql,
                vehiculo.getPatente(),
                vehiculo.getCapacidadPeso(),
                vehiculo.getCapacidadVolumen(),
                vehiculo.getCentroDistId(),
                vehiculo.getVehiculoId()
        );
    }


    public int deleteById(Integer id) {
        String sql = "DELETE FROM vehiculo v WHERE v.id = ?";
        return jdbcTemplate.update(sql, id);
    }


    private void validarCentro(VehiculoEntity vehiculo) {
        if (vehiculo.getCentroDistId() == null) {
            throw new IllegalArgumentException("El vehículo debe tener un centro de distribución válido");
        }
    }
}
