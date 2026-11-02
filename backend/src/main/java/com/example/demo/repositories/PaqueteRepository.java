package com.example.demo.repositories;

import com.example.demo.dto.Pagina;
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
            rs.getInt("cliente_id"), //usuario
            rs.getInt("centro_id"), //centro de distribucion
            rs.getObject("paquete_origen_id", Integer.class),
            rs.getString("codigo"),
            rs.getString("estado"),
            rs.getBigDecimal("peso"),
            rs.getBigDecimal("volumen"),
            rs.getString("direccion_destino"),
            rs.getInt("intentos"),
            rs.getObject("fecha_compromiso", LocalDateTime.class),
            rs.getObject("despachado_at", LocalDateTime.class),
            rs.getObject("entregado_at", LocalDateTime.class),
            rs.getObject("fecha_reprogramada", LocalDateTime.class)
    );

    private static final String PAQUETE_SELECT =
            "SELECT id, cliente_id, centro_id, paquete_origen_id, codigo, estado, " +
            "peso, volumen, direccion_destino, intentos, fecha_compromiso, despachado_at, entregado_at, fecha_reprogramada FROM paquete p";

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

    /** Serializa el PUT con los procedimientos que modifican el ciclo de entrega. */
    public Optional<PaqueteEntity> findByIdForUpdate(Integer id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    PAQUETE_SELECT + " WHERE id = ? FOR UPDATE", paqueteRowMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<PaqueteEntity> findByClienteId(Integer clienteId) {
        return jdbcTemplate.query(PAQUETE_SELECT + " WHERE cliente_id = ?", paqueteRowMapper, clienteId);
    }

    public Optional<PaqueteEntity> findByCodigoAndClienteId(String codigo, Integer clienteId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    PAQUETE_SELECT + " WHERE codigo = ? AND cliente_id = ?", paqueteRowMapper, codigo, clienteId
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Ownership predicate in SQL: cliente = own packages, repartidor = packages on own routes, despachador = all. */
    private static String visibilidad(String rol) {
        return switch (rol == null ? "" : rol) {
            case "despachador" -> "TRUE";
            case "cliente" -> "p.cliente_id = ?";
            case "repartidor" -> "EXISTS (SELECT 1 FROM ruta_paquete rp JOIN ruta r ON r.id = rp.ruta_id " +
                    "JOIN repartidor rep ON rep.id = r.repartidor_id WHERE rp.paquete_id = p.id AND rep.usuario_id = ?)";
            default -> "FALSE";
        };
    }

    private static Object[] visibilidadArgs(String rol, Integer uid, Object... extra) {
        boolean usaUid = "cliente".equals(rol) || "repartidor".equals(rol);
        Object[] args = new Object[extra.length + (usaUid ? 1 : 0)];
        int i = 0;
        for (Object e : extra) {
            args[i++] = e;
        }
        if (usaUid) {
            args[i] = uid;
        }
        return args;
    }

    public List<PaqueteEntity> findVisibles(String rol, Integer uid, String estado, Integer centroId) {
        String sql = PAQUETE_SELECT + " WHERE " + visibilidad(rol);
        var args = new java.util.ArrayList<Object>(java.util.Arrays.asList(visibilidadArgs(rol, uid)));
        if (estado != null) { sql += " AND p.estado = ?"; args.add(estado); }
        if (centroId != null) { sql += " AND p.centro_id = ?"; args.add(centroId); }
        return jdbcTemplate.query(sql + " ORDER BY p.id", paqueteRowMapper, args.toArray());
    }

    /** Same visibility and filters as {@link #findVisibles}, plus q = contains over codigo or direccion_destino. */
    public Pagina<PaqueteEntity> findVisiblesPage(String rol, Integer uid, String estado, Integer centroId,
                                                  String q, Integer page, Integer size) {
        String sql = PAQUETE_SELECT + " WHERE " + visibilidad(rol);
        var args = new java.util.ArrayList<Object>(java.util.Arrays.asList(visibilidadArgs(rol, uid)));
        if (estado != null) { sql += " AND p.estado = ?"; args.add(estado); }
        if (centroId != null) { sql += " AND p.centro_id = ?"; args.add(centroId); }
        if (Consulta.hay(q)) {
            sql += " AND (p.codigo ILIKE ? ESCAPE '\\' OR p.direccion_destino ILIKE ? ESCAPE '\\')";
            args.add(Consulta.contiene(q));
            args.add(Consulta.contiene(q));
        }
        return Consulta.listar(jdbcTemplate, sql, args, "p.id", paqueteRowMapper, page, size);
    }

    public boolean esVisible(Integer paqueteId, String rol, Integer uid) {
        Boolean ok = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM paquete p WHERE p.id = ? AND " + visibilidad(rol) + ")",
                Boolean.class, visibilidadArgs(rol, uid, paqueteId));
        return Boolean.TRUE.equals(ok);
    }

    /** Sets the audit user for the DB trigger; only meaningful inside a transaction (is_local = true). */
    public void setAppUser(Integer uid) {
        jdbcTemplate.queryForObject("SELECT set_config('app.user_id', ?, true)", String.class, String.valueOf(uid));
    }

    public int updateDireccion(Integer paqueteId, String direccion) {
        return jdbcTemplate.update("UPDATE paquete SET direccion_destino = ? WHERE id = ?", direccion, paqueteId);
    }

    public PaqueteEntity save(PaqueteEntity paquete) {
        if (paquete.getEstado() == null) {
            paquete.setEstado("Registrado");
        }
        if (paquete.getIntentos() == null) {
            paquete.setIntentos(0);
        }
        // Se omite codigo cuando es null para utilizar el UUID generado por PostgreSQL.
        boolean conCodigo = paquete.getCodigo() != null && !paquete.getCodigo().isBlank();
        String sql = "INSERT INTO paquete (cliente_id, centro_id, paquete_origen_id, " + (conCodigo ? "codigo, " : "") +
                "estado, peso, volumen, direccion_destino, intentos, fecha_compromiso, despachado_at, entregado_at) " +
                "VALUES (?, ?, ?, " + (conCodigo ? "?, " : "") + "?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        java.util.List<Object> args = new java.util.ArrayList<>(java.util.List.of(
                paquete.getUsuarioClienteId(), paquete.getCentroDistId()));
        args.add(paquete.getPaqueteOrigenId());
        if (conCodigo) {
            args.add(paquete.getCodigo());
        }
        args.add(paquete.getEstado());
        args.add(paquete.getPeso());
        args.add(paquete.getVolumen());
        args.add(paquete.getDireccionDestino());
        args.add(paquete.getIntentos());
        args.add(paquete.getFechaCompromiso());
        args.add(paquete.getDespachadoAt());
        args.add(paquete.getEntregadoAt());
        Integer generatedId = jdbcTemplate.queryForObject(sql, Integer.class, args.toArray());
        String codigoFinal = jdbcTemplate.queryForObject("SELECT codigo FROM paquete WHERE id = ?", String.class, generatedId);
        paquete.setCodigo(codigoFinal);
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
        jdbcTemplate.update("DELETE FROM evento_seguimiento WHERE paquete_id = ? AND EXISTS (SELECT 1 FROM paquete WHERE id = ? AND despachado_at IS NULL AND NOT EXISTS (SELECT 1 FROM ruta_paquete WHERE paquete_id = ?))", id, id, id);
        String sql = "DELETE FROM paquete p WHERE p.id = ? AND p.estado IN ('Registrado','En centro') " +
                "AND p.despachado_at IS NULL " +
                "AND NOT EXISTS (SELECT 1 FROM ruta_paquete rp WHERE rp.paquete_id = p.id) " +
                "AND NOT EXISTS (SELECT 1 FROM evento_seguimiento es WHERE es.paquete_id = p.id) " +
                "AND NOT EXISTS (SELECT 1 FROM auditoria_direccion ad WHERE ad.paquete_id = p.id) " +
                "AND NOT EXISTS (SELECT 1 FROM paquete retorno WHERE retorno.paquete_origen_id = p.id)";
        return jdbcTemplate.update(sql, id);
    }

    public void registrarFallida(int paqueteId, int rutaId, String motivo) {
        jdbcTemplate.update("CALL registrar_entrega_fallida(?, ?, ?)", ps -> {
            ps.setInt(1, paqueteId);
            ps.setInt(2, rutaId);
            ps.setString(3, motivo);
        });
    }

    public void registrarExitosa(int paqueteId, int rutaId) {
        jdbcTemplate.update("CALL registrar_entrega_exitosa(?, ?)", paqueteId, rutaId);
    }

}
