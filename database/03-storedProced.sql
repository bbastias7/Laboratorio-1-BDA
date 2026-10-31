-- SP1: create a route and assign packages atomically. Any failure (including the
-- capacity trigger) rolls back the route, assignments, state changes and events.
-- Call: CALL asignar_ruta(centro, repartidor, vehiculo, fecha, ARRAY[ids], NULL);
-- The new route id comes back in the INOUT parameter p_ruta_id.
CREATE OR REPLACE PROCEDURE asignar_ruta(
    p_centro_id INT, p_repartidor_id INT, p_vehiculo_id INT, p_fecha TIMESTAMP,
    p_paquetes INT[], INOUT p_ruta_id INT DEFAULT NULL)
AS $$
DECLARE
    v_ref_centro INT;
    v_bad INT[];
BEGIN
    IF p_paquetes IS NULL OR cardinality(p_paquetes) = 0 THEN
        RAISE EXCEPTION 'At least one package is required' USING ERRCODE = '23514';
    END IF;
    IF array_position(p_paquetes, NULL) IS NOT NULL THEN
        RAISE EXCEPTION 'Package list contains NULL' USING ERRCODE = '23514';
    END IF;
    IF cardinality(p_paquetes) <> (SELECT count(DISTINCT x) FROM unnest(p_paquetes) AS x) THEN
        RAISE EXCEPTION 'Package list contains duplicate ids' USING ERRCODE = '23514';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM centro_distribucion WHERE id = p_centro_id) THEN
        RAISE EXCEPTION 'Center % not found', p_centro_id USING ERRCODE = 'P0002';
    END IF;

    SELECT centro_id INTO v_ref_centro FROM repartidor WHERE id = p_repartidor_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Courier % not found', p_repartidor_id USING ERRCODE = 'P0002';
    END IF;
    IF v_ref_centro <> p_centro_id THEN
        RAISE EXCEPTION 'Courier % does not belong to center %', p_repartidor_id, p_centro_id
            USING ERRCODE = '23514';
    END IF;

    SELECT centro_id INTO v_ref_centro FROM vehiculo WHERE id = p_vehiculo_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Vehicle % not found', p_vehiculo_id USING ERRCODE = 'P0002';
    END IF;
    IF v_ref_centro <> p_centro_id THEN
        RAISE EXCEPTION 'Vehicle % does not belong to center %', p_vehiculo_id, p_centro_id
            USING ERRCODE = '23514';
    END IF;

    SELECT array_agg(x) INTO v_bad FROM unnest(p_paquetes) AS x
     WHERE NOT EXISTS (SELECT 1 FROM paquete WHERE id = x);
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'Packages not found: %', v_bad USING ERRCODE = 'P0002';
    END IF;

    -- Lock in id order so concurrent assignments of the same package serialize.
    PERFORM 1 FROM paquete WHERE id = ANY (p_paquetes) ORDER BY id FOR UPDATE;

    SELECT array_agg(id) INTO v_bad FROM paquete
     WHERE id = ANY (p_paquetes) AND (estado NOT IN ('Registrado', 'En centro', 'Reprogramado') OR centro_id <> p_centro_id OR fecha_compromiso IS NULL);
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'Packages not assignable in their current state: %', v_bad
            USING ERRCODE = '23514';
    END IF;

    SELECT array_agg(DISTINCT paquete_id) INTO v_bad FROM ruta_paquete
     WHERE paquete_id = ANY (p_paquetes) AND resultado = 'Pendiente';
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'Packages already pending on a route: %', v_bad USING ERRCODE = '23514';
    END IF;

    INSERT INTO ruta (centro_id, repartidor_id, vehiculo_id, fecha)
    VALUES (p_centro_id, p_repartidor_id, p_vehiculo_id, COALESCE(p_fecha, now()))
    RETURNING id INTO p_ruta_id;

    -- The capacity trigger fires per row and aborts the whole call when exceeded.
    INSERT INTO ruta_paquete (ruta_id, paquete_id)
    SELECT p_ruta_id, x FROM unnest(p_paquetes) WITH ORDINALITY AS t(x, n) ORDER BY n;

    UPDATE paquete
       SET estado = 'En reparto', fecha_reprogramada = NULL, despachado_at = COALESCE(despachado_at, now())
     WHERE id = ANY (p_paquetes);

    INSERT INTO evento_seguimiento (paquete_id, estado, descripcion, fecha)
    SELECT x, 'En reparto', 'Asignado a la ruta ' || p_ruta_id,
           clock_timestamp()
      FROM unnest(p_paquetes) AS x;
END;
$$ LANGUAGE plpgsql;

-- SP2: record a failed delivery attempt on a pending route assignment.
-- Rule: more than 3 failed attempts (intentos > 3) returns the package to origin
-- and creates a linked return package at the fourth failure.
-- fecha_compromiso is never modified.
CREATE OR REPLACE PROCEDURE registrar_entrega_fallida(
    p_paquete_id INT, p_ruta_id INT, p_motivo VARCHAR DEFAULT NULL)
AS $$
DECLARE
    v_resultado VARCHAR;
    v_pkg paquete%ROWTYPE;
    v_centro_dir VARCHAR;
    v_nuevo_id INT;
    v_motivo TEXT;
BEGIN
    PERFORM 1 FROM paquete WHERE id = p_paquete_id FOR UPDATE;
    SELECT resultado INTO v_resultado FROM ruta_paquete
     WHERE ruta_id = p_ruta_id AND paquete_id = p_paquete_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Package % is not assigned to route %', p_paquete_id, p_ruta_id
            USING ERRCODE = 'P0002';
    END IF;
    IF v_resultado <> 'Pendiente' THEN
        RAISE EXCEPTION 'Assignment of package % on route % is already %',
            p_paquete_id, p_ruta_id, v_resultado USING ERRCODE = '23514';
    END IF;

    SELECT * INTO v_pkg FROM paquete WHERE id = p_paquete_id FOR UPDATE;
    v_motivo := left(COALESCE(NULLIF(trim(p_motivo), ''), 'sin motivo informado'), 100);

    UPDATE ruta_paquete SET resultado = 'Fallido', fecha_resultado = now()
     WHERE ruta_id = p_ruta_id AND paquete_id = p_paquete_id;

    v_pkg.intentos := v_pkg.intentos + 1;

    IF v_pkg.intentos > 3 THEN
        UPDATE paquete SET intentos = v_pkg.intentos, estado = 'Devuelto a origen', fecha_reprogramada = NULL
         WHERE id = p_paquete_id;
        INSERT INTO evento_seguimiento (paquete_id, estado, descripcion, fecha)
        VALUES (p_paquete_id, 'Devuelto a origen',
                'Intento fallido ' || v_pkg.intentos || ' (' || v_motivo
                || '); se superaron 3 intentos, devuelto a origen', clock_timestamp());

        SELECT direccion INTO v_centro_dir FROM centro_distribucion WHERE id = v_pkg.centro_id;
        INSERT INTO paquete (estado, peso, volumen, direccion_destino, fecha_compromiso,
                             cliente_id, centro_id, paquete_origen_id)
        VALUES ('Registrado', v_pkg.peso, v_pkg.volumen, v_centro_dir, now() + interval '3 days',
                v_pkg.cliente_id, v_pkg.centro_id, p_paquete_id)
        RETURNING id INTO v_nuevo_id;
        INSERT INTO evento_seguimiento (paquete_id, estado, descripcion, fecha)
        VALUES (v_nuevo_id, 'Registrado',
                'Paquete de devolucion del envio ' || v_pkg.codigo, clock_timestamp());
    ELSE
        UPDATE paquete SET intentos = v_pkg.intentos, estado = 'Reprogramado', fecha_reprogramada = now() + interval '1 day'
         WHERE id = p_paquete_id;
        INSERT INTO evento_seguimiento (paquete_id, estado, descripcion, fecha)
        VALUES (p_paquete_id, 'Reprogramado',
                'Intento fallido ' || v_pkg.intentos || ' (' || v_motivo || '); reprogramado',
                clock_timestamp());
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Extra procedure used by the API: mark a pending assignment as delivered.
CREATE OR REPLACE PROCEDURE registrar_entrega_exitosa(p_paquete_id INT, p_ruta_id INT)
AS $$
DECLARE
    v_resultado VARCHAR;
BEGIN
    PERFORM 1 FROM paquete WHERE id = p_paquete_id FOR UPDATE;
    SELECT resultado INTO v_resultado FROM ruta_paquete
     WHERE ruta_id = p_ruta_id AND paquete_id = p_paquete_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Package % is not assigned to route %', p_paquete_id, p_ruta_id
            USING ERRCODE = 'P0002';
    END IF;
    IF v_resultado <> 'Pendiente' THEN
        RAISE EXCEPTION 'Assignment of package % on route % is already %',
            p_paquete_id, p_ruta_id, v_resultado USING ERRCODE = '23514';
    END IF;

    UPDATE ruta_paquete SET resultado = 'Entregado', fecha_resultado = now()
     WHERE ruta_id = p_ruta_id AND paquete_id = p_paquete_id;
    UPDATE paquete SET estado = 'Entregado', entregado_at = now() WHERE id = p_paquete_id;
    INSERT INTO evento_seguimiento (paquete_id, estado, descripcion, fecha)
    VALUES (p_paquete_id, 'Entregado', 'Entregado al destinatario', clock_timestamp());
END;
$$ LANGUAGE plpgsql;


-- CRUD de asignaciones: conserva estado y eventos en una sola operación.
CREATE OR REPLACE PROCEDURE agregar_paquete_ruta(p_ruta integer,p_paquete integer)
LANGUAGE plpgsql AS $$
BEGIN
 PERFORM 1 FROM paquete WHERE id=p_paquete FOR UPDATE;
 PERFORM 1 FROM ruta WHERE id=p_ruta FOR UPDATE;
 INSERT INTO ruta_paquete(ruta_id,paquete_id) VALUES(p_ruta,p_paquete);
 UPDATE paquete SET estado='En reparto',despachado_at=coalesce(despachado_at,now()),fecha_reprogramada=NULL WHERE id=p_paquete;
 INSERT INTO evento_seguimiento(paquete_id,estado,descripcion) VALUES(p_paquete,'En reparto','Asignado a ruta '||p_ruta);
END $$;
CREATE OR REPLACE PROCEDURE quitar_paquete_ruta(p_ruta integer,p_paquete integer)
LANGUAGE plpgsql AS $$
BEGIN
 PERFORM 1 FROM paquete WHERE id=p_paquete FOR UPDATE;
 PERFORM 1 FROM ruta WHERE id=p_ruta FOR UPDATE;
 DELETE FROM ruta_paquete WHERE ruta_id=p_ruta AND paquete_id=p_paquete;
 IF NOT FOUND THEN RAISE EXCEPTION 'Asignación inexistente' USING ERRCODE='P0002'; END IF;
 UPDATE paquete SET estado='En centro' WHERE id=p_paquete;
 INSERT INTO evento_seguimiento(paquete_id,estado,descripcion) VALUES(p_paquete,'En centro','Retirado de ruta '||p_ruta);
END $$;


