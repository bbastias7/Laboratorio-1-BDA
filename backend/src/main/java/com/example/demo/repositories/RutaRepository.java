package com.example.demo.repositories;

import com.example.demo.dto.Pagina;
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

    /** Filters: centroId, repartidorId. {@code usuarioRepartidor} (when not null) restricts to that courier's routes. */
    public Pagina<RutaEntity> findPage(Integer usuarioRepartidor, Integer centroId, Integer repartidorId,
                                       Integer page, Integer size) {
        String sql = RUTA_SELECT + " WHERE TRUE";
        List<Object> args = new java.util.ArrayList<>();
        if (usuarioRepartidor != null) {
            sql += " AND repartidor_id IN (SELECT id FROM repartidor WHERE usuario_id = ?)";
            args.add(usuarioRepartidor);
        }
        if (centroId != null) { sql += " AND centro_id = ?"; args.add(centroId); }
        if (repartidorId != null) { sql += " AND repartidor_id = ?"; args.add(repartidorId); }
        String orden = usuarioRepartidor != null ? "fecha DESC, id DESC" : "id";
        return Consulta.listar(jdbcTemplate, sql, args, orden, rutaEntityRowMapper, page, size);
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

    public List<RutaEntity> findByUsuarioRepartidor(Integer usuarioId) {
        return jdbcTemplate.query(RUTA_SELECT + " WHERE repartidor_id IN (SELECT id FROM repartidor WHERE usuario_id = ?) " +
                "ORDER BY fecha DESC, id DESC", rutaEntityRowMapper, usuarioId);
    }

    /** True when the route belongs to the repartidor whose usuario_id is {@code usuarioId}. */
    public boolean esDeUsuario(Integer rutaId, Integer usuarioId) {
        Boolean ok = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ruta r JOIN repartidor rep ON rep.id = r.repartidor_id " +
                        "WHERE r.id = ? AND rep.usuario_id = ?)", Boolean.class, rutaId, usuarioId);
        return Boolean.TRUE.equals(ok);
    }

    /** CALL asignar_ruta(..., INOUT p_ruta_id): the INOUT value comes back as a one-row result set. */
    public int asignarRuta(int centroId, int repartidorId, int vehiculoId, LocalDateTime fecha, List<Integer> paqueteIds) {
        Integer id = jdbcTemplate.execute((java.sql.Connection con) -> {
            java.sql.Array ids = con.createArrayOf("integer", paqueteIds.toArray(new Integer[0]));
            try (java.sql.PreparedStatement ps = con.prepareStatement("CALL asignar_ruta(?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, centroId);
                ps.setInt(2, repartidorId);
                ps.setInt(3, vehiculoId);
                ps.setObject(4, fecha);
                ps.setArray(5, ids);
                ps.setNull(6, java.sql.Types.INTEGER);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            } finally {
                ids.free();
            }
        });
        if (id == null) {
            throw new IllegalStateException("asignar_ruta no devolvió el ID de la ruta");
        }
        return id;
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
