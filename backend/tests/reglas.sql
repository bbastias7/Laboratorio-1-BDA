\set ON_ERROR_STOP on
BEGIN;
CREATE FUNCTION pg_temp.rechaza(q text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
 BEGIN EXECUTE q;
 EXCEPTION WHEN check_violation OR foreign_key_violation OR unique_violation THEN RETURN; END;
 RAISE EXCEPTION 'Se aceptó una operación inválida: %',q;
END $$;
-- Fuerza un fallo al crear el retorno para comprobar la atomicidad del SP2.
CREATE FUNCTION pg_temp.rechazar_retorno() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.paquete_origen_id IS NOT NULL AND current_setting('app.prueba_retorno',true)='rechazar' THEN
  RAISE EXCEPTION 'Fallo simulado al generar retorno' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER prueba_retorno BEFORE INSERT ON paquete FOR EACH ROW EXECUTE FUNCTION pg_temp.rechazar_retorno();
DO $$ DECLARE cli int; actor int; centro int; rep int; veh int; pid int; big int; vol int; rid int; n int; eventos int; p paquete%ROWTYPE;
BEGIN
 SELECT id INTO cli FROM usuario WHERE email='cliente2@grupo5.cl';
 SELECT id INTO actor FROM usuario WHERE email='despachador@grupo5.cl';
 PERFORM set_config('app.user_id',actor::text,true);
 SELECT id INTO centro FROM centro_distribucion WHERE nombre='CD Santiago G5';
 SELECT id INTO rep FROM repartidor LIMIT 1;
 SELECT id INTO veh FROM vehiculo WHERE patente='G5-TEST';
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(2,1,'Prueba',now()+interval '1 day',cli,centro) RETURNING id INTO pid;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(200,1,'Exceso',now(),cli,centro) RETURNING id INTO big;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(2,11,'Exceso volumen',now(),cli,centro) RETURNING id INTO vol;
 SELECT count(*) INTO n FROM ruta;
 PERFORM pg_temp.rechaza(format('CALL asignar_ruta(%s,%s,%s,now()::timestamp,ARRAY[%s,%s],NULL)',centro,rep,veh,pid,big));
 PERFORM pg_temp.rechaza(format('CALL asignar_ruta(%s,%s,%s,now()::timestamp,ARRAY[%s,%s],NULL)',centro,rep,veh,pid,vol));
 PERFORM pg_temp.rechaza(format('CALL asignar_ruta(%s,%s,%s,now()::timestamp,ARRAY[%s,%s],NULL)',centro,rep,veh,pid,pid));
 IF (SELECT count(*) FROM ruta)<>n OR EXISTS(SELECT 1 FROM ruta_paquete WHERE paquete_id=pid)
 THEN RAISE EXCEPTION 'SP1 no es atómico'; END IF;
 CALL asignar_ruta(centro,rep,veh,now()::timestamp,ARRAY[pid],rid);
 PERFORM pg_temp.rechaza(format('UPDATE paquete SET peso=1000 WHERE id=%s',pid));
 PERFORM pg_temp.rechaza(format('UPDATE vehiculo SET capacidad_volumen=0.01 WHERE id=%s',veh));
 PERFORM pg_temp.rechaza(format('UPDATE ruta SET fecha=now()+interval ''1 day'' WHERE id=%s',rid));
 PERFORM pg_temp.rechaza(format('INSERT INTO repartidor(usuario_id,centro_id) VALUES(%s,%s)',cli,centro));
 UPDATE paquete SET direccion_destino='Dirección auditada' WHERE id=pid;
 IF NOT EXISTS(SELECT 1 FROM auditoria_direccion WHERE paquete_id=pid AND usuario_id=actor
 AND direccion_anterior='Prueba' AND direccion_nueva='Dirección auditada') THEN RAISE EXCEPTION 'Auditoría incorrecta'; END IF;
 PERFORM set_config('app.user_id','',true);
 PERFORM pg_temp.rechaza(format('UPDATE paquete SET direccion_destino=''Sin actor'' WHERE id=%s',pid));
 PERFORM set_config('app.user_id',actor::text,true);
 FOR n IN 1..4 LOOP
  IF n>1 THEN CALL asignar_ruta(centro,rep,veh,now()::timestamp,ARRAY[pid],rid); END IF;
  IF n=4 THEN
   SELECT count(*) INTO eventos FROM evento_seguimiento WHERE paquete_id=pid;
   PERFORM set_config('app.prueba_retorno','rechazar',true);
   PERFORM pg_temp.rechaza(format('CALL registrar_entrega_fallida(%s,%s,''Retorno fallido'')',pid,rid));
   IF (SELECT intentos FROM paquete WHERE id=pid)<>3
    OR (SELECT resultado FROM ruta_paquete WHERE ruta_id=rid AND paquete_id=pid)<>'Pendiente'
    OR (SELECT count(*) FROM evento_seguimiento WHERE paquete_id=pid)<>eventos
    OR EXISTS(SELECT 1 FROM paquete WHERE paquete_origen_id=pid)
   THEN RAISE EXCEPTION 'SP2 no es atómico'; END IF;
   PERFORM set_config('app.prueba_retorno','',true);
  END IF;
  CALL registrar_entrega_fallida(pid,rid,'Ausente');
  SELECT * INTO p FROM paquete WHERE id=pid;
  IF p.intentos<>n THEN RAISE EXCEPTION 'Intentos incorrectos'; END IF;
  IF n<4 AND (p.estado<>'Reprogramado' OR p.fecha_reprogramada IS NULL) THEN RAISE EXCEPTION 'Sin reprogramación'; END IF;
  PERFORM pg_temp.rechaza(format('CALL registrar_entrega_fallida(%s,%s,''Duplicado'')',pid,rid));
 END LOOP;
 IF p.estado<>'Devuelto a origen' OR (SELECT count(*) FROM paquete WHERE paquete_origen_id=pid)<>1
 THEN RAISE EXCEPTION 'Retorno incorrecto'; END IF;
 -- El destino de un paquete en estado final no cambia.
 PERFORM pg_temp.rechaza(format('UPDATE paquete SET direccion_destino=''Tras retorno'' WHERE id=%s',pid));
 UPDATE paquete SET estado='Entregado' WHERE id=pid;
 PERFORM pg_temp.rechaza(format('UPDATE paquete SET direccion_destino=''Tras entrega'' WHERE id=%s',pid));
 -- La auditoría es inmutable.
 PERFORM pg_temp.rechaza('UPDATE auditoria_direccion SET direccion_nueva=''Alterada''');
 PERFORM pg_temp.rechaza('DELETE FROM auditoria_direccion');
 REFRESH MATERIALIZED VIEW mv_rendimiento_entregas;
 IF EXISTS(SELECT 1 FROM mv_rendimiento_entregas WHERE entregas_totales>0 AND pct_a_tiempo NOT BETWEEN 0 AND 100)
 THEN RAISE EXCEPTION 'Porcentaje incorrecto'; END IF;
END $$;
ROLLBACK;
\echo Pruebas de integridad aprobadas

\set ON_ERROR_STOP on
BEGIN;
DO $$ DECLARE actor int; cli int; ctr int; u1 int; u2 int; rep1 int; rep2 int; veh int; p1 int; p2 int; rid int; pct numeric; promedio numeric; i int;
BEGIN
 SELECT id INTO actor FROM usuario WHERE email='despachador@grupo5.cl';
 SELECT id INTO cli FROM usuario WHERE email='cliente1@grupo5.cl';
 PERFORM set_config('app.user_id',actor::text,true);
 INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('R1 reporte','r1-reporte@example.invalid','repartidor',gen_random_uuid()::text) RETURNING id INTO u1;
 INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('R2 reporte','r2-reporte@example.invalid','repartidor',gen_random_uuid()::text) RETURNING id INTO u2;
 INSERT INTO centro_distribucion(nombre,direccion,ciudad) VALUES('Test reporte','A','Santiago') RETURNING id INTO ctr;
 INSERT INTO repartidor(usuario_id,centro_id) VALUES(u1,ctr) RETURNING id INTO rep1;
 INSERT INTO repartidor(usuario_id,centro_id) VALUES(u2,ctr) RETURNING id INTO rep2;
 INSERT INTO vehiculo(patente,capacidad_peso,capacidad_volumen,centro_id) VALUES('TEST-REPORT',10,2,ctr) RETURNING id INTO veh;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(2,0.1,'A',now()+interval '1 day',cli,ctr) RETURNING id INTO p1;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(2,0.1,'B',date_trunc('day',now()),cli,ctr) RETURNING id INTO p2;
 CALL asignar_ruta(ctr,rep1,veh,now()::timestamp,ARRAY[p1],rid);
 CALL registrar_entrega_fallida(p1,rid,'Reintento');
 CALL asignar_ruta(ctr,rep2,veh,now()::timestamp,ARRAY[p1,p2],rid);
 CALL registrar_entrega_exitosa(p1,rid);
 CALL registrar_entrega_exitosa(p2,rid);
 IF EXISTS(SELECT 1 FROM mv_rendimiento_entregas WHERE centro_id=ctr) THEN RAISE EXCEPTION 'La vista se actualizó sin refresco'; END IF;
 REFRESH MATERIALIZED VIEW mv_rendimiento_entregas;
 SELECT round(100.0*sum(entregas_a_tiempo)/nullif(sum(entregas_totales),0),2),
 round(sum(intentos_totales)::numeric/(SELECT count(DISTINCT id) FROM mv_rendimiento_entregas
 CROSS JOIN LATERAL unnest(paquete_ids) AS id WHERE centro_id=ctr),2)
 INTO pct,promedio FROM mv_rendimiento_entregas WHERE centro_id=ctr;
 IF pct<>50 OR promedio<>1.50 THEN RAISE EXCEPTION 'Métricas de centro incorrectas: %, %',pct,promedio; END IF;
 IF (SELECT entregas_a_tiempo FROM mv_rendimiento_entregas WHERE centro_id=ctr AND repartidor_id=rep2)<>1
 THEN RAISE EXCEPTION 'Se ignoró la hora de compromiso'; END IF;
 IF (SELECT pct_a_tiempo FROM mv_rendimiento_entregas WHERE centro_id=ctr AND repartidor_id=rep1) IS NOT NULL
 THEN RAISE EXCEPTION 'Se inventó porcentaje sin entregas'; END IF;
 -- El repartidor 2 ya tiene una entrega puntual y una tardía.
 -- Agregar 16 puntuales y dos tardías deja 17 de 20: exactamente 85 %.
 FOR i IN 1..18 LOOP
  INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
  VALUES(1,0.1,'Umbral',now()+CASE WHEN i<=16 THEN interval '1 day' ELSE interval '-1 day' END,cli,ctr)
  RETURNING id INTO p2;
  CALL asignar_ruta(ctr,rep2,veh,now()::timestamp,ARRAY[p2],rid);
  CALL registrar_entrega_exitosa(p2,rid);
 END LOOP;
 REFRESH MATERIALIZED VIEW mv_rendimiento_entregas;
 IF (SELECT pct_a_tiempo FROM mv_rendimiento_entregas WHERE centro_id=ctr AND repartidor_id=rep2)<>85
  OR EXISTS(SELECT 1 FROM mv_rendimiento_entregas WHERE centro_id=ctr AND repartidor_id=rep2
            AND 100.0*entregas_a_tiempo/nullif(entregas_totales,0)<85)
 THEN RAISE EXCEPTION 'Se alertó al alcanzar exactamente 85 por ciento'; END IF;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(1,0.1,'Bajo umbral',now()-interval '1 day',cli,ctr) RETURNING id INTO p2;
 CALL asignar_ruta(ctr,rep2,veh,now()::timestamp,ARRAY[p2],rid);
 CALL registrar_entrega_exitosa(p2,rid);
 REFRESH MATERIALIZED VIEW mv_rendimiento_entregas;
 IF NOT EXISTS(SELECT 1 FROM mv_rendimiento_entregas WHERE centro_id=ctr AND repartidor_id=rep2
               AND 100.0*entregas_a_tiempo/nullif(entregas_totales,0)<85)
 THEN RAISE EXCEPTION 'No se detectó el rendimiento inferior al 85 por ciento'; END IF;
END $$;
ROLLBACK;
\echo Pruebas de reportes aprobadas

BEGIN;
CREATE FUNCTION pg_temp.rechaza(q text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
 BEGIN EXECUTE q;
 EXCEPTION WHEN check_violation THEN RETURN; END;
 RAISE EXCEPTION 'Se aceptó una operación inválida: %',q;
END $$;
DO $$ DECLARE cli int; ctr int; rep int; veh int; p1 int; p2 int; p3 int; rid int;
BEGIN
 SELECT id INTO cli FROM usuario WHERE email='cliente2@grupo5.cl';
 SELECT id INTO ctr FROM centro_distribucion WHERE nombre='CD Santiago G5';
 SELECT id INTO rep FROM repartidor WHERE centro_id=ctr ORDER BY id LIMIT 1;
 INSERT INTO vehiculo(patente,capacidad_peso,capacidad_volumen,centro_id)
 VALUES('TEST-CAP-TOTAL',10,2,ctr) RETURNING id INTO veh;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(6,1,'A',now()+interval '1 day',cli,ctr) RETURNING id INTO p1;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(6,1,'B',now()+interval '1 day',cli,ctr) RETURNING id INTO p2;
 INSERT INTO paquete(peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
 VALUES(1,1.5,'C',now()+interval '1 day',cli,ctr) RETURNING id INTO p3;
 CALL asignar_ruta(ctr,rep,veh,now()::timestamp,ARRAY[p1],rid);
 CALL registrar_entrega_exitosa(p1,rid);
 PERFORM pg_temp.rechaza(format('CALL agregar_paquete_ruta(%s,%s)',rid,p2));
 PERFORM pg_temp.rechaza(format('CALL agregar_paquete_ruta(%s,%s)',rid,p3));
 PERFORM pg_temp.rechaza(format('UPDATE vehiculo SET capacidad_peso=5 WHERE id=%s',veh));
 PERFORM pg_temp.rechaza(format('UPDATE vehiculo SET capacidad_volumen=0.5 WHERE id=%s',veh));
 IF (SELECT estado FROM paquete WHERE id=p2)<>'Registrado'
  OR EXISTS(SELECT 1 FROM ruta_paquete WHERE paquete_id=p2)
 THEN RAISE EXCEPTION 'La asignación rechazada dejó cambios'; END IF;
 UPDATE paquete SET peso=4 WHERE id=p2;
 CALL agregar_paquete_ruta(rid,p2); -- igualdad exacta con ambos límites: válida
 IF (SELECT count(*) FROM ruta_paquete WHERE ruta_id=rid)<>2
 THEN RAISE EXCEPTION 'Se rechazó la capacidad exacta'; END IF;
END $$;
ROLLBACK;
\echo Pruebas de capacidad total aprobadas
