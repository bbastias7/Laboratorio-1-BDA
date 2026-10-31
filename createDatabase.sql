-- Instalación completa equivalente a database/01..05. Usar una base vacía.
BEGIN;

-- 01-dbCreate.sql
-- Esquema del grupo 5. Ejecutar en una base vacía; no elimina datos existentes.
CREATE TABLE usuario (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(64) NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    rol VARCHAR(16) NOT NULL CHECK (rol IN ('cliente','repartidor','despachador')),
    keycloak_id VARCHAR(128) UNIQUE NOT NULL
);


CREATE TABLE centro_distribucion (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(64) NOT NULL,
    direccion VARCHAR(128) NOT NULL,
    ciudad VARCHAR(64) NOT NULL
);


CREATE TABLE repartidor (
    id SERIAL PRIMARY KEY,
    usuario_id INT UNIQUE NOT NULL,
    centro_id INT NOT NULL,
    CONSTRAINT fk_repartidor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_repartidor_centro FOREIGN KEY (centro_id) REFERENCES centro_distribucion(id)
);


CREATE TABLE vehiculo (
    id SERIAL PRIMARY KEY,
    patente VARCHAR(16) UNIQUE NOT NULL,
    capacidad_peso NUMERIC(10,2) NOT NULL,
    capacidad_volumen NUMERIC(10,2) NOT NULL,
    centro_id INT NOT NULL,
    CONSTRAINT ck_vehiculo_capacidad_peso CHECK (capacidad_peso > 0),
    CONSTRAINT ck_vehiculo_capacidad_volumen CHECK (capacidad_volumen > 0),
    CONSTRAINT fk_vehiculo_centro FOREIGN KEY (centro_id) REFERENCES centro_distribucion(id)
);


CREATE TABLE paquete (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(64) NOT NULL UNIQUE DEFAULT ('G5-' || replace(gen_random_uuid()::text, '-', '')),
    estado VARCHAR(32) NOT NULL DEFAULT 'Registrado' CHECK (estado IN ('Registrado','En centro','En reparto','Entregado','Reprogramado','Devuelto a origen')),
    peso NUMERIC(10, 2) NOT NULL,
    volumen NUMERIC(10, 2) NOT NULL,
    direccion_destino VARCHAR(255) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    fecha_compromiso TIMESTAMP NOT NULL,
    fecha_reprogramada TIMESTAMP,
    despachado_at TIMESTAMP,
    entregado_at TIMESTAMP,
    cliente_id INT NOT NULL,
    centro_id INT NOT NULL, -- centro de origen del paquete
    paquete_origen_id INT,
    CONSTRAINT ck_paquete_peso CHECK (peso > 0),
    CONSTRAINT ck_paquete_volumen CHECK (volumen > 0),
    CONSTRAINT ck_paquete_intentos CHECK (intentos >= 0),
    CONSTRAINT ck_paquete_no_auto_origen CHECK (paquete_origen_id IS DISTINCT FROM id),
    CONSTRAINT fk_paquete_cliente FOREIGN KEY (cliente_id) REFERENCES usuario(id),
    CONSTRAINT fk_paquete_centro FOREIGN KEY (centro_id) REFERENCES centro_distribucion(id),
    CONSTRAINT fk_paquete_origen FOREIGN KEY (paquete_origen_id) REFERENCES paquete(id) ON DELETE RESTRICT
);


CREATE TABLE ruta (
    id SERIAL PRIMARY KEY,
    centro_id INT NOT NULL, -- centro de origen de la ruta
    repartidor_id INT NOT NULL,
    vehiculo_id INT NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ruta_centro FOREIGN KEY (centro_id) REFERENCES centro_distribucion(id),
    CONSTRAINT fk_ruta_repartidor FOREIGN KEY (repartidor_id) REFERENCES repartidor(id),
    CONSTRAINT fk_ruta_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id)
);


CREATE TABLE evento_seguimiento (
    id SERIAL PRIMARY KEY,
    paquete_id INT NOT NULL,
    estado VARCHAR(32) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evento_paquete FOREIGN KEY (paquete_id) REFERENCES paquete(id)
);


CREATE TABLE auditoria_direccion (
    id SERIAL PRIMARY KEY,
    paquete_id INT NOT NULL,
    direccion_anterior VARCHAR(255) NOT NULL,
    direccion_nueva VARCHAR(255) NOT NULL,
    usuario_id INT,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_paquete FOREIGN KEY (paquete_id) REFERENCES paquete(id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);


CREATE TABLE ruta_paquete (
    ruta_id INT NOT NULL,
    paquete_id INT NOT NULL,
    resultado VARCHAR(32) NOT NULL DEFAULT 'Pendiente' CHECK (resultado IN ('Pendiente','Entregado','Fallido')),
    fecha_resultado TIMESTAMP,
    CONSTRAINT ck_ruta_paquete_resultado_fecha CHECK (
        (resultado = 'Pendiente' AND fecha_resultado IS NULL)
        OR (resultado <> 'Pendiente' AND fecha_resultado IS NOT NULL)
    ),
    CONSTRAINT pk_ruta_paquete PRIMARY KEY (ruta_id, paquete_id),
    CONSTRAINT fk_ruta_paquete_ruta FOREIGN KEY (ruta_id) REFERENCES ruta(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ruta_paquete_paquete FOREIGN KEY (paquete_id) REFERENCES paquete(id) ON DELETE RESTRICT
);

-- Conserva rutas anteriores, pero solo permite una asignacion activa por paquete.
CREATE UNIQUE INDEX uq_ruta_paquete_pendiente
    ON ruta_paquete (paquete_id)
    WHERE resultado = 'Pendiente';

CREATE INDEX idx_paquete_estado ON paquete(estado);
-- El rastreo busca solo por igualdad (codigo = ?), por lo que un índice HASH es suficiente.
CREATE INDEX idx_paquete_codigo ON paquete USING HASH (codigo);
CREATE INDEX idx_evento_seguimiento_paquete_fecha ON evento_seguimiento(paquete_id,fecha,id);


-- 02-materialViews.sql
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

-- 03-storedProced.sql
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



-- 04-triggers.sql
CREATE OR REPLACE FUNCTION check_route_capacity() RETURNS TRIGGER AS $$
DECLARE
    max_weight NUMERIC;
    max_volume NUMERIC;
    total_weight NUMERIC;
    total_volume NUMERIC;
BEGIN
    SELECT v.capacidad_peso, v.capacidad_volumen
      INTO max_weight, max_volume
      FROM ruta r JOIN vehiculo v ON v.id = r.vehiculo_id
     WHERE r.id = NEW.ruta_id
     FOR UPDATE OF r, v;

    -- The foreign key reports an unknown route; avoid a misleading capacity error.
    IF NOT FOUND THEN
        RETURN NEW;
    END IF;

    SELECT COALESCE(sum(p.peso), 0), COALESCE(sum(p.volumen), 0)
      INTO total_weight, total_volume
      FROM ruta_paquete rp JOIN paquete p ON p.id = rp.paquete_id
     WHERE rp.ruta_id = NEW.ruta_id;

    SELECT total_weight + p.peso, total_volume + p.volumen
      INTO total_weight, total_volume
      FROM paquete p WHERE p.id = NEW.paquete_id;

    -- The foreign key reports an unknown package.
    IF NOT FOUND THEN
        RETURN NEW;
    END IF;

    IF total_weight > max_weight OR total_volume > max_volume THEN
        RAISE EXCEPTION 'Route % exceeds vehicle capacity (weight % / %, volume % / %)',
            NEW.ruta_id, total_weight, max_weight, total_volume, max_volume
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS route_capacity_before_insert ON ruta_paquete;
CREATE TRIGGER route_capacity_before_insert
BEFORE INSERT ON ruta_paquete
FOR EACH ROW EXECUTE FUNCTION check_route_capacity();

CREATE OR REPLACE FUNCTION audit_destination_change() RETURNS TRIGGER AS $$
DECLARE actor integer;
BEGIN
    -- A delivered or returned package has no destination left to change.
    IF OLD.estado IN ('Entregado', 'Devuelto a origen')
       AND NEW.direccion_destino IS DISTINCT FROM OLD.direccion_destino THEN
        RAISE EXCEPTION 'No se puede cambiar el destino de un paquete en estado final'
            USING ERRCODE='23514';
    END IF;
    IF OLD.despachado_at IS NOT NULL
       AND NEW.direccion_destino IS DISTINCT FROM OLD.direccion_destino THEN
        actor := NULLIF(current_setting('app.user_id',true), '')::integer;
        IF actor IS NULL THEN
            RAISE EXCEPTION 'Usuario responsable requerido' USING ERRCODE='23514';
        END IF;
        INSERT INTO auditoria_direccion
            (paquete_id, direccion_anterior, direccion_nueva, usuario_id, fecha)
        VALUES (OLD.id, OLD.direccion_destino, NEW.direccion_destino,
                actor, now());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS destination_audit_before_update ON paquete;
CREATE TRIGGER destination_audit_before_update
BEFORE UPDATE OF direccion_destino ON paquete
FOR EACH ROW EXECUTE FUNCTION audit_destination_change();

-- The audit log is append-only: rows are only created by audit_destination_change.
CREATE OR REPLACE FUNCTION proteger_auditoria() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'La auditoría de direcciones es inmutable' USING ERRCODE='23514';
END $$;
DROP TRIGGER IF EXISTS auditoria_inmutable ON auditoria_direccion;
CREATE TRIGGER auditoria_inmutable BEFORE UPDATE OR DELETE ON auditoria_direccion
FOR EACH ROW EXECUTE FUNCTION proteger_auditoria();


CREATE OR REPLACE FUNCTION validar_perfiles() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_TABLE_NAME='repartidor' THEN
  IF NOT EXISTS(SELECT 1 FROM usuario WHERE id=NEW.usuario_id AND rol='repartidor') THEN
   RAISE EXCEPTION 'El usuario debe tener rol repartidor' USING ERRCODE='23514';
  END IF;
  IF TG_OP='UPDATE' AND (NEW.usuario_id,NEW.centro_id) IS DISTINCT FROM (OLD.usuario_id,OLD.centro_id)
    AND EXISTS(SELECT 1 FROM ruta WHERE repartidor_id=OLD.id) THEN
   RAISE EXCEPTION 'No se puede reasignar un repartidor con rutas históricas' USING ERRCODE='23514';
  END IF;
 ELSE
  IF NEW.rol IS DISTINCT FROM OLD.rol AND (
    EXISTS(SELECT 1 FROM repartidor WHERE usuario_id=OLD.id) OR
    EXISTS(SELECT 1 FROM paquete WHERE cliente_id=OLD.id)) THEN
   RAISE EXCEPTION 'El rol está vinculado a operaciones existentes' USING ERRCODE='23514';
  END IF;
  IF NEW.keycloak_id IS DISTINCT FROM OLD.keycloak_id THEN
   RAISE EXCEPTION 'La identidad de un perfil es inmutable' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS perfil_repartidor ON repartidor;
CREATE TRIGGER perfil_repartidor BEFORE INSERT OR UPDATE ON repartidor FOR EACH ROW EXECUTE FUNCTION validar_perfiles();
DROP TRIGGER IF EXISTS perfil_usuario ON usuario;
CREATE TRIGGER perfil_usuario BEFORE UPDATE ON usuario FOR EACH ROW EXECUTE FUNCTION validar_perfiles();

CREATE OR REPLACE FUNCTION proteger_paquete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE id=NEW.cliente_id AND rol='cliente') THEN
  RAISE EXCEPTION 'El propietario debe ser cliente' USING ERRCODE='23514';
 END IF;
 IF NEW.fecha_compromiso IS NULL THEN RAISE EXCEPTION 'Fecha compromiso requerida' USING ERRCODE='23514'; END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.despachado_at IS NOT NULL AND
    (NEW.peso,NEW.volumen,NEW.centro_id,NEW.cliente_id,NEW.codigo,NEW.fecha_compromiso)
     IS DISTINCT FROM (OLD.peso,OLD.volumen,OLD.centro_id,OLD.cliente_id,OLD.codigo,OLD.fecha_compromiso) THEN
   RAISE EXCEPTION 'Datos de carga, propietario, tracking y compromiso inmutables tras despacho' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS proteger_ciclo ON paquete;
CREATE TRIGGER proteger_ciclo BEFORE INSERT OR UPDATE ON paquete FOR EACH ROW EXECUTE FUNCTION proteger_paquete();

CREATE OR REPLACE FUNCTION proteger_ruta() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE centro_rep integer; centro_veh integer;
BEGIN
 SELECT centro_id INTO centro_rep FROM repartidor WHERE id=NEW.repartidor_id;
 SELECT centro_id INTO centro_veh FROM vehiculo WHERE id=NEW.vehiculo_id;
 IF centro_rep IS DISTINCT FROM NEW.centro_id OR centro_veh IS DISTINCT FROM NEW.centro_id THEN
  RAISE EXCEPTION 'Vehículo y repartidor deben pertenecer al centro de la ruta' USING ERRCODE='23514';
 END IF;
 IF TG_OP='UPDATE' AND NEW IS DISTINCT FROM OLD AND EXISTS(SELECT 1 FROM ruta_paquete WHERE ruta_id=OLD.id) THEN
  RAISE EXCEPTION 'No se puede alterar una ruta con asignaciones' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS coherencia_ruta ON ruta;
CREATE TRIGGER coherencia_ruta BEFORE INSERT OR UPDATE ON ruta FOR EACH ROW EXECUTE FUNCTION proteger_ruta();

CREATE OR REPLACE FUNCTION proteger_vehiculo() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.centro_id<>OLD.centro_id AND EXISTS(SELECT 1 FROM ruta WHERE vehiculo_id=OLD.id) THEN
  RAISE EXCEPTION 'El vehículo tiene rutas en su centro original' USING ERRCODE='23514';
 END IF;
 IF EXISTS(SELECT 1 FROM ruta r JOIN ruta_paquete rp ON rp.ruta_id=r.id
    JOIN paquete p ON p.id=rp.paquete_id WHERE r.vehiculo_id=OLD.id
    GROUP BY r.id HAVING sum(p.peso)>NEW.capacidad_peso OR sum(p.volumen)>NEW.capacidad_volumen) THEN
  RAISE EXCEPTION 'La nueva capacidad es inferior a la carga de una ruta' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS capacidad_modificada ON vehiculo;
CREATE TRIGGER capacidad_modificada BEFORE UPDATE ON vehiculo FOR EACH ROW EXECUTE FUNCTION proteger_vehiculo();

CREATE OR REPLACE FUNCTION proteger_asignacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  IF OLD.resultado<>'Pendiente' THEN RAISE EXCEPTION 'No se elimina historial resuelto' USING ERRCODE='23514'; END IF;
  RETURN OLD;
 END IF;
 IF TG_OP='INSERT' AND NOT EXISTS(SELECT 1 FROM paquete p JOIN ruta r ON r.centro_id=p.centro_id
   WHERE p.id=NEW.paquete_id AND r.id=NEW.ruta_id AND p.estado IN ('Registrado','En centro','Reprogramado')) THEN
  RAISE EXCEPTION 'Paquete no elegible o de otro centro' USING ERRCODE='23514';
 END IF;
 IF TG_OP='UPDATE' AND (OLD.resultado<>'Pendiente' OR NEW.resultado='Pendiente'
  OR (NEW.ruta_id,NEW.paquete_id) IS DISTINCT FROM (OLD.ruta_id,OLD.paquete_id)) THEN
  RAISE EXCEPTION 'Una asignación resuelta no se reabre ni se mueve' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS coherencia_asignacion ON ruta_paquete;
CREATE TRIGGER coherencia_asignacion BEFORE INSERT OR UPDATE OR DELETE ON ruta_paquete FOR EACH ROW EXECUTE FUNCTION proteger_asignacion();



-- 05-loadData.sql
-- Datos locales opcionales; el realm JSON usa exactamente estos UUID.
INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('despachador','despachador@grupo5.cl','despachador','50000000-0000-4000-8000-000000000001') ON CONFLICT(email) DO NOTHING;
INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('repartidor1','repartidor1@grupo5.cl','repartidor','50000000-0000-4000-8000-000000000002') ON CONFLICT(email) DO NOTHING;
INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('repartidor2','repartidor2@grupo5.cl','repartidor','50000000-0000-4000-8000-000000000003') ON CONFLICT(email) DO NOTHING;
INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('cliente1','cliente1@grupo5.cl','cliente','50000000-0000-4000-8000-000000000004') ON CONFLICT(email) DO NOTHING;
INSERT INTO usuario(nombre,email,rol,keycloak_id) VALUES('cliente2','cliente2@grupo5.cl','cliente','50000000-0000-4000-8000-000000000005') ON CONFLICT(email) DO NOTHING;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE email='despachador@grupo5.cl' AND keycloak_id='50000000-0000-4000-8000-000000000001' AND rol='despachador') THEN RAISE EXCEPTION 'Perfil de demostración incompatible: despachador'; END IF;
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE email='repartidor1@grupo5.cl' AND keycloak_id='50000000-0000-4000-8000-000000000002' AND rol='repartidor') THEN RAISE EXCEPTION 'Perfil de demostración incompatible: repartidor1'; END IF;
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE email='repartidor2@grupo5.cl' AND keycloak_id='50000000-0000-4000-8000-000000000003' AND rol='repartidor') THEN RAISE EXCEPTION 'Perfil de demostración incompatible: repartidor2'; END IF;
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE email='cliente1@grupo5.cl' AND keycloak_id='50000000-0000-4000-8000-000000000004' AND rol='cliente') THEN RAISE EXCEPTION 'Perfil de demostración incompatible: cliente1'; END IF;
 IF NOT EXISTS(SELECT 1 FROM usuario WHERE email='cliente2@grupo5.cl' AND keycloak_id='50000000-0000-4000-8000-000000000005' AND rol='cliente') THEN RAISE EXCEPTION 'Perfil de demostración incompatible: cliente2'; END IF;
END $$;

INSERT INTO centro_distribucion(nombre,direccion,ciudad)
SELECT 'CD Santiago G5','Alameda 1000','Santiago' WHERE NOT EXISTS(SELECT 1 FROM centro_distribucion WHERE nombre='CD Santiago G5');
INSERT INTO centro_distribucion(nombre,direccion,ciudad)
SELECT 'CD Valparaiso G5','Errazuriz 700','Valparaiso' WHERE NOT EXISTS(SELECT 1 FROM centro_distribucion WHERE nombre='CD Valparaiso G5');
INSERT INTO repartidor(usuario_id,centro_id)
SELECT u.id,c.id FROM usuario u JOIN centro_distribucion c ON c.nombre='CD Santiago G5'
WHERE u.email IN ('repartidor1@grupo5.cl','repartidor2@grupo5.cl') ON CONFLICT(usuario_id) DO NOTHING;
INSERT INTO vehiculo(patente,capacidad_peso,capacidad_volumen,centro_id)
SELECT 'G5-TEST',100,10,id FROM centro_distribucion WHERE nombre='CD Santiago G5' ON CONFLICT(patente) DO NOTHING;
DO $$ DECLARE actor integer; cli integer; centro integer; rep integer; veh integer; pid integer; rid integer; i integer;
BEGIN
 SELECT id INTO actor FROM usuario WHERE email='despachador@grupo5.cl';
 PERFORM set_config('app.user_id',actor::text,true);
 SELECT id INTO cli FROM usuario WHERE email='cliente1@grupo5.cl';
 SELECT id INTO centro FROM centro_distribucion WHERE nombre='CD Santiago G5';
 SELECT id INTO veh FROM vehiculo WHERE patente='G5-TEST';
 FOR i IN 1..5 LOOP
  IF EXISTS(SELECT 1 FROM paquete WHERE codigo='G5-DEMO-'||i) THEN CONTINUE; END IF;
  INSERT INTO paquete(codigo,peso,volumen,direccion_destino,fecha_compromiso,cliente_id,centro_id)
  VALUES('G5-DEMO-'||i,2,0.5,'Destino demostración '||i,
   now() + CASE WHEN i IN (2,4) THEN interval '-1 day' ELSE interval '1 day' END,cli,centro) RETURNING id INTO pid;
  INSERT INTO evento_seguimiento(paquete_id,estado,descripcion) VALUES(pid,'Registrado','Alta de demostración');
  SELECT r.id INTO rep FROM repartidor r JOIN usuario u ON u.id=r.usuario_id
   WHERE u.email=CASE WHEN i<=2 THEN 'repartidor1@grupo5.cl' ELSE 'repartidor2@grupo5.cl' END;
  CALL asignar_ruta(centro,rep,veh,now()::timestamp,ARRAY[pid],rid);
  IF i<5 THEN CALL registrar_entrega_exitosa(pid,rid); END IF;
 END LOOP;
END $$;
REFRESH MATERIALIZED VIEW mv_rendimiento_entregas;

COMMIT;
