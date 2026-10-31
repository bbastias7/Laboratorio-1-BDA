package com.example.demo.repositories;

import com.example.demo.dto.Dtos.Rendimiento;
import com.example.demo.dto.Dtos.RendimientoCentro;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReporteRepository {

    private static final String SELECT = "SELECT centro_id, centro_nombre, repartidor_id, repartidor_nombre, " +
            "entregas_totales, entregas_a_tiempo, intentos_totales, paquetes_atendidos, pct_a_tiempo, " +
            "intentos_promedio FROM mv_rendimiento_entregas";

    private static final RowMapper<Rendimiento> MAPPER = (rs, i) -> new Rendimiento(
            rs.getInt("centro_id"), rs.getString("centro_nombre"), rs.getInt("repartidor_id"),
            rs.getString("repartidor_nombre"), rs.getLong("entregas_totales"), rs.getLong("entregas_a_tiempo"),
            rs.getLong("intentos_totales"), rs.getLong("paquetes_atendidos"), rs.getBigDecimal("pct_a_tiempo"),
            rs.getBigDecimal("intentos_promedio"));

    private final JdbcTemplate jdbcTemplate;

    public List<Rendimiento> rendimiento() {
        return jdbcTemplate.query(SELECT + " ORDER BY centro_id, repartidor_id", MAPPER);
    }

    public List<Rendimiento> repartidoresBajo(int umbral) {
        return jdbcTemplate.query(SELECT + " WHERE 100.0 * entregas_a_tiempo / NULLIF(entregas_totales,0) < ? ORDER BY pct_a_tiempo, repartidor_id", MAPPER, umbral);
    }

    /** Centers are re-aggregated by summing counts, never by averaging per-courier percentages. */
    public List<RendimientoCentro> centrosBajo(int umbral) {
        return jdbcTemplate.query("""
                WITH resumen AS (
                    SELECT centro_id, centro_nombre, sum(entregas_totales) AS totales,
                           sum(entregas_a_tiempo) AS a_tiempo, sum(intentos_totales) AS intentos
                    FROM mv_rendimiento_entregas GROUP BY centro_id, centro_nombre
                ), paquetes AS (
                    SELECT centro_id, count(DISTINCT paquete_id) AS cantidad
                    FROM mv_rendimiento_entregas CROSS JOIN LATERAL unnest(paquete_ids) AS paquete_id
                    GROUP BY centro_id
                )
                SELECT r.centro_id, r.centro_nombre, r.totales, r.a_tiempo,
                       round(100.0*r.a_tiempo/nullif(r.totales,0),2) AS pct,
                       round(r.intentos::numeric/nullif(p.cantidad,0),2) AS promedio
                FROM resumen r JOIN paquetes p ON p.centro_id=r.centro_id
                WHERE 100.0*r.a_tiempo/nullif(r.totales,0) < ?
                ORDER BY pct,r.centro_id""",
                (rs, i) -> new RendimientoCentro(rs.getInt("centro_id"), rs.getString("centro_nombre"),
                        rs.getLong("totales"), rs.getLong("a_tiempo"), rs.getBigDecimal("pct"),
                        rs.getBigDecimal("promedio")), umbral);
    }

    public void refresh() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW mv_rendimiento_entregas");
    }
}
