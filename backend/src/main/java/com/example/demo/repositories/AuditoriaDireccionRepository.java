package com.example.demo.repositories;

import com.example.demo.dto.Pagina;
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
            rs.getObject("usuario_id", Integer.class),
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

    /** Filters: paqueteId, usuarioId. */
    public Pagina<AuditoriaDireccionEntity> findPage(Integer paqueteId, Integer usuarioId, Integer page, Integer size) {
        String sql = "SELECT id, paquete_id, usuario_id, direccion_anterior, direccion_nueva, fecha " +
                "FROM auditoria_direccion WHERE TRUE";
        List<Object> args = new java.util.ArrayList<>();
        if (paqueteId != null) { sql += " AND paquete_id = ?"; args.add(paqueteId); }
        if (usuarioId != null) { sql += " AND usuario_id = ?"; args.add(usuarioId); }
        return Consulta.listar(jdbcTemplate, sql, args, "id", auditoriaDireccionRowMapper, page, size);
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
}
