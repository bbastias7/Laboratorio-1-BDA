package com.example.demo.repositories;
import com.example.demo.entities.CentroDistribucionEntity;
import com.example.demo.entities.RepartidorEntity;

import com.example.demo.entities.UsuarioEntity;
import com.example.demo.entities.VehiculoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RepartidorRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<RepartidorEntity> repartidorRowMapper = (rs, rowNum) -> {
        UsuarioEntity usuario = new UsuarioEntity(
                rs.getInt("u_id"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("rol")
        );

        CentroDistribucionEntity centro = new CentroDistribucionEntity(
                rs.getInt("centro_id"),
                rs.getString("centro_nombre"),
                rs.getString("centro_direccion"),
                rs.getString("centro_ciudad")
        );

        return new RepartidorEntity(
                rs.getInt("r_id"),
                usuario,
                centro
        );
    };


    public List<RepartidorEntity> findAll() {
        String sql = """
            SELECT r.id AS r_id, r.centro_id,
                   c.nombre AS centro_nombre, c.direccion AS centro_direccion,
                   c.ciudad AS centro_ciudad,
                   u.id AS u_id, u.nombre, u.email, u.password, u.rol
            FROM repartidor r
            INNER JOIN usuario u ON r.usuario_id = u.id
            INNER JOIN centro_distribucion c ON r.centro_id = c.id
            """;
        return jdbcTemplate.query(sql, repartidorRowMapper);
    }


    public Optional<RepartidorEntity> findById(Integer id) {
        String sql = """
            SELECT r.id AS r_id, r.centro_id,
                   c.nombre AS centro_nombre, c.direccion AS centro_direccion,
                   c.ciudad AS centro_ciudad,
                   u.id AS u_id, u.nombre, u.email, u.password, u.rol
            FROM repartidor r
            INNER JOIN usuario u ON r.usuario_id = u.id
            INNER JOIN centro_distribucion c ON r.centro_id = c.id
            WHERE r.id = ?
            """;
        try {
            RepartidorEntity repartidor = jdbcTemplate.queryForObject(sql, repartidorRowMapper, id);
            return Optional.ofNullable(repartidor);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public RepartidorEntity save(RepartidorEntity repartidor) {

    }
}
