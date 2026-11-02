package com.example.demo.repositories;

import com.example.demo.dto.Pagina;
import com.example.demo.entities.RutaPaqueteEntity;
import com.example.demo.dto.Dtos.EntregaView;
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
    public void agregar(Integer ruta, Integer paquete) {
        jdbcTemplate.update("CALL agregar_paquete_ruta(?,?)", ruta, paquete);
    }
    public void quitar(Integer ruta, Integer paquete) {
        jdbcTemplate.update("CALL quitar_paquete_ruta(?,?)", ruta, paquete);
    }
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

    /** Filters: rutaId, paqueteId, resultado. */
    public Pagina<RutaPaqueteEntity> findPage(Integer rutaId, Integer paqueteId, String resultado,
                                              Integer page, Integer size) {
        String sql = RUTA_PAQUETE_SELECT + " WHERE TRUE";
        List<Object> args = new java.util.ArrayList<>();
        if (rutaId != null) { sql += " AND ruta_id = ?"; args.add(rutaId); }
        if (paqueteId != null) { sql += " AND paquete_id = ?"; args.add(paqueteId); }
        if (Consulta.hay(resultado)) { sql += " AND resultado = ?"; args.add(resultado); }
        return Consulta.listar(jdbcTemplate, sql, args, "ruta_id, paquete_id", rutaPaqueteEntityRowMapper, page, size);
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


    public List<EntregaView> listarDeUsuario(Integer usuarioId, boolean soloPendientes) {
        String sql = "SELECT r.id AS ruta_id, r.fecha, p.id AS paquete_id, p.codigo, p.direccion_destino, p.estado, " +
                "rp.resultado, p.intentos, p.fecha_compromiso " +
                "FROM ruta_paquete rp JOIN ruta r ON r.id = rp.ruta_id " +
                "JOIN repartidor rep ON rep.id = r.repartidor_id JOIN paquete p ON p.id = rp.paquete_id " +
                "WHERE rep.usuario_id = ? " + (soloPendientes ? "AND rp.resultado = 'Pendiente' " : "") +
                "ORDER BY r.fecha DESC, r.id DESC, p.id";
        return jdbcTemplate.query(sql, (rs, i) -> new EntregaView(
                rs.getInt("ruta_id"), rs.getObject("fecha", LocalDateTime.class), rs.getInt("paquete_id"),
                rs.getString("codigo"), rs.getString("direccion_destino"), rs.getString("estado"),
                rs.getString("resultado"), rs.getInt("intentos"),
                rs.getObject("fecha_compromiso", LocalDateTime.class)), usuarioId);
    }}
