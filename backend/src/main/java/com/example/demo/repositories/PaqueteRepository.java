package com.example.demo.repositories;

import com.example.demo.entities.PaqueteEntity;
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
public class PaqueteRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PaqueteEntity> paqueteRowMapper = (rs, rowNum) -> new PaqueteEntity(
            rs.getInt("id"),
            rs.getInt("usuario_cliente_id"),
            rs.getInt("centro_dist_id"),
            rs.getObject("paquete_origen_id", Integer.class),
            rs.getString("codigo"),
            rs.getString("estado"),
            rs.getDouble("peso"),
            rs.getDouble("volumen"),
            rs.getString("direccion_destino"),
            rs.getInt("intentos"),
            rs.getObject("fecha_compromiso", LocalDateTime.class),
            rs.getObject("despachado_at", LocalDateTime.class),
            rs.getObject("entregado_at", LocalDateTime.class)
    );

    private static final String PAQUETE_SELECT =
            "SELECT id, usuario_cliente_id, centro_dist_id, paquete_origen_id, codigo, estado, " +
            "peso, volumen, direccion_destino, intentos, fecha_compromiso, despachado_at, entregado_at FROM paquete";

    public List<PaqueteEntity> findAll() {
        return jdbcTemplate.query(PAQUETE_SELECT, paqueteRowMapper);
    }

    public Optional<PaqueteEntity> findById(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    PAQUETE_SELECT + " WHERE id = ?", paqueteRowMapper, id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<PaqueteEntity> findByCodigo(String codigo) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    PAQUETE_SELECT + " WHERE codigo = ?", paqueteRowMapper, codigo
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public PaqueteEntity save(PaqueteEntity paquete) {
        Integer generatedId = jdbcTemplate.queryForObject(
                "INSERT INTO paquete (cliente_id, centro_id, paquete_origen_id, codigo, estado, peso, volumen, direccion_destino, intentos, fecha_compromiso, despachado_at, entregado_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Integer.class,
                paquete.getUsuarioClienteId(),
                paquete.getCentroDistId(),
                paquete.getPaqueteOrigenId(),
                paquete.getCodigo(),
                paquete.getEstado(),
                paquete.getPeso(),
                paquete.getVolumen(),
                paquete.getDireccionDestino(),
                paquete.getIntentos(),
                paquete.getFechaCompromiso(),
                paquete.getDespachadoAt(),
                paquete.getEntregadoAt()
        );
        if (generatedId == null) {
            throw new IllegalStateException("La inserción no devolvió el ID del paquete");
        }
        paquete.setPaqueteId(generatedId);
        return paquete;
    }

    public int update(PaqueteEntity paquete) {
        return jdbcTemplate.update(
                "UPDATE paquete SET cliente_id = ?, centro_id = ?, paquete_origen_id = ?, codigo = ?, estado = ?, peso = ?, volumen = ?, direccion_destino = ?, intentos = ?, fecha_compromiso = ?, despachado_at = ?, entregado_at = ? WHERE id = ?",
                paquete.getUsuarioClienteId(),
                paquete.getCentroDistId(),
                paquete.getPaqueteOrigenId(),
                paquete.getCodigo(),
                paquete.getEstado(),
                paquete.getPeso(),
                paquete.getVolumen(),
                paquete.getDireccionDestino(),
                paquete.getIntentos(),
                paquete.getFechaCompromiso(),
                paquete.getDespachadoAt(),
                paquete.getEntregadoAt(),
                paquete.getPaqueteId()
        );
    }

    public int deleteById(Integer id) {
        String sql = "DELETE FROM paquete WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

}
