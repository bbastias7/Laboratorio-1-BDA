-- Una sola vista materializada: datos por centro y repartidor.
-- paquete_ids permite contar envíos distintos al agregar por centro.
CREATE MATERIALIZED VIEW mv_rendimiento_entregas AS
SELECT r.centro_id, c.nombre AS centro_nombre, r.repartidor_id, u.nombre AS repartidor_nombre,
 count(*) FILTER(WHERE rp.resultado='Entregado')::integer AS entregas_totales,
 count(*) FILTER(WHERE rp.resultado='Entregado' AND rp.fecha_resultado<=p.fecha_compromiso)::integer AS entregas_a_tiempo,
 count(*)::integer AS intentos_totales,
 count(DISTINCT p.id)::integer AS paquetes_atendidos,
 array_agg(DISTINCT p.id) AS paquete_ids,
 round(100.0*count(*) FILTER(WHERE rp.resultado='Entregado' AND rp.fecha_resultado<=p.fecha_compromiso)
       /nullif(count(*) FILTER(WHERE rp.resultado='Entregado'),0),2) AS pct_a_tiempo,
 round(count(*)::numeric/nullif(count(DISTINCT p.id),0),2) AS intentos_promedio
FROM ruta_paquete rp JOIN ruta r ON r.id=rp.ruta_id
JOIN paquete p ON p.id=rp.paquete_id
JOIN centro_distribucion c ON c.id=r.centro_id
JOIN repartidor rep ON rep.id=r.repartidor_id JOIN usuario u ON u.id=rep.usuario_id
WHERE rp.resultado IN ('Entregado','Fallido')
GROUP BY r.centro_id,c.nombre,r.repartidor_id,u.nombre;


-- (centro_id, repartidor_id) es la clave del GROUP BY; el índice único permite REFRESH ... CONCURRENTLY.
CREATE UNIQUE INDEX idx_mv_rendimiento_centro_repartidor
    ON mv_rendimiento_entregas(centro_id, repartidor_id);

-- Refresca la vista sin bloquear lecturas. Requiere la vista ya poblada (se crea con datos).
CREATE OR REPLACE FUNCTION refrescar_mv_rendimiento() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rendimiento_entregas;
END $$;

-- Refresco diario opcional dentro de la base (requiere la extensión pg_cron, no instalada por defecto):
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- SELECT cron.schedule('refrescar-mv-rendimiento', '0 2 * * *', 'SELECT refrescar_mv_rendimiento()');
-- La aplicación Spring ejecuta el mismo refresco diario con @Scheduled.
