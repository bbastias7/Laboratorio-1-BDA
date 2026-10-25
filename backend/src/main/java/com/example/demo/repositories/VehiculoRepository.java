package com.example.demo.repositories;
import com.example.demo.entities.CentroDistribucionEntity;
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
public class VehiculoRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<VehiculoEntity> vehiculoEntityRowMapper = (rs, rowNum) ->  {
        VehiculoEntity vehiculoEntity = new VehiculoEntity();
        vehiculoEntity.setId(rs.getInt("id"));
        vehiculoEntity.setPatente(rs.getString("patente"));
        vehiculoEntity.setCapacidadPeso(rs.getDouble("capacidad_peso"));
        vehiculoEntity.setCapacidadVolumen(rs.getDouble("capacidad_volumen"));
        vehiculoEntity.setCentro(new CentroDistribucionEntity(
                rs.getInt("centro_id"),
                rs.getString("centro_nombre"),
                rs.getString("centro_direccion"),
                rs.getString("centro_ciudad")
        ));
        return vehiculoEntity;
    };

    private static final String VEHICULO_SELECT = """
            SELECT v.id, v.patente, v.capacidad_peso, v.capacidad_volumen,
                   c.id AS centro_id, c.nombre AS centro_nombre,
                   c.direccion AS centro_direccion, c.ciudad AS centro_ciudad
            FROM vehiculo v
            INNER JOIN centro_distribucion c ON v.centro_id = c.id
            """;

    public List<VehiculoEntity> findAll() {
        String sql = VEHICULO_SELECT;
        return jdbcTemplate.query(sql, vehiculoEntityRowMapper);
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


    public List<VehiculoEntity> findByCapacidadPeso(Double capacidadPeso) {
        String sql = VEHICULO_SELECT + " WHERE v.capacidad_peso = ?";
        return jdbcTemplate.query(sql, vehiculoEntityRowMapper, capacidadPeso);
    }


    public List<VehiculoEntity> findByCapacidadVolumen(Double volumen) {
        String sql = VEHICULO_SELECT + " WHERE v.capacidad_volumen = ?";
        return jdbcTemplate.query(sql, vehiculoEntityRowMapper, volumen);
    }


    public VehiculoEntity save(VehiculoEntity vehiculo) {
        validarCentro(vehiculo);
        String sql = "INSERT INTO vehiculo (patente, capacidad_peso, capacidad_volumen, centro_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, vehiculo.getPatente());
            ps.setDouble(2, vehiculo.getCapacidadPeso());
            ps.setDouble(3, vehiculo.getCapacidadVolumen());
            ps.setInt(4, vehiculo.getCentro().getId());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            vehiculo.setId(((Number) keyHolder.getKeys().get("id")).intValue());
        }
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
                vehiculo.getCentro().getId(),
                vehiculo.getId()
        );
    }


    private void validarCentro(VehiculoEntity vehiculo) {
        if (vehiculo.getCentro() == null || vehiculo.getCentro().getId() == null) {
            throw new IllegalArgumentException("El vehículo debe tener un centro de distribución válido");
        }
    }
}
