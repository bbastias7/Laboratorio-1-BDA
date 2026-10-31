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


