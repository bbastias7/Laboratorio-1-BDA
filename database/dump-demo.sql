--
-- PostgreSQL database dump
--

\restrict dH3o8EQB77UAZJRMeXrEpFwWdIBLbNgh8lL8WYHbKjers1xb1kHayh0Rl2XF8Ao

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: agregar_paquete_ruta(integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.agregar_paquete_ruta(IN p_ruta integer, IN p_paquete integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
 PERFORM 1 FROM paquete WHERE id=p_paquete FOR UPDATE;
 PERFORM 1 FROM ruta WHERE id=p_ruta FOR UPDATE;
 INSERT INTO ruta_paquete(ruta_id,paquete_id) VALUES(p_ruta,p_paquete);
 UPDATE paquete SET estado='En reparto',despachado_at=coalesce(despachado_at,now()),fecha_reprogramada=NULL WHERE id=p_paquete;
 INSERT INTO evento_seguimiento(paquete_id,estado,descripcion) VALUES(p_paquete,'En reparto','Asignado a ruta '||p_ruta);
END $$;


--
-- Name: asignar_ruta(integer, integer, integer, timestamp without time zone, integer[], integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.asignar_ruta(IN p_centro_id integer, IN p_repartidor_id integer, IN p_vehiculo_id integer, IN p_fecha timestamp without time zone, IN p_paquetes integer[], INOUT p_ruta_id integer DEFAULT NULL::integer)
    LANGUAGE plpgsql
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
$$;


--
-- Name: audit_destination_change(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.audit_destination_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: check_route_capacity(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.check_route_capacity() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: proteger_asignacion(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.proteger_asignacion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: proteger_auditoria(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.proteger_auditoria() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'La auditoría de direcciones es inmutable' USING ERRCODE='23514';
END $$;


--
-- Name: proteger_paquete(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.proteger_paquete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: proteger_ruta(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.proteger_ruta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: proteger_vehiculo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.proteger_vehiculo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


--
-- Name: quitar_paquete_ruta(integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.quitar_paquete_ruta(IN p_ruta integer, IN p_paquete integer)
    LANGUAGE plpgsql
    AS $$
BEGIN
 PERFORM 1 FROM paquete WHERE id=p_paquete FOR UPDATE;
 PERFORM 1 FROM ruta WHERE id=p_ruta FOR UPDATE;
 DELETE FROM ruta_paquete WHERE ruta_id=p_ruta AND paquete_id=p_paquete;
 IF NOT FOUND THEN RAISE EXCEPTION 'Asignación inexistente' USING ERRCODE='P0002'; END IF;
 UPDATE paquete SET estado='En centro' WHERE id=p_paquete;
 INSERT INTO evento_seguimiento(paquete_id,estado,descripcion) VALUES(p_paquete,'En centro','Retirado de ruta '||p_ruta);
END $$;


--
-- Name: refrescar_mv_rendimiento(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.refrescar_mv_rendimiento() RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rendimiento_entregas;
END $$;


--
-- Name: registrar_entrega_exitosa(integer, integer); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.registrar_entrega_exitosa(IN p_paquete_id integer, IN p_ruta_id integer)
    LANGUAGE plpgsql
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
$$;


--
-- Name: registrar_entrega_fallida(integer, integer, character varying); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.registrar_entrega_fallida(IN p_paquete_id integer, IN p_ruta_id integer, IN p_motivo character varying DEFAULT NULL::character varying)
    LANGUAGE plpgsql
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
$$;


--
-- Name: validar_perfiles(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validar_perfiles() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auditoria_direccion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auditoria_direccion (
    id integer NOT NULL,
    paquete_id integer NOT NULL,
    direccion_anterior character varying(255) NOT NULL,
    direccion_nueva character varying(255) NOT NULL,
    usuario_id integer,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: auditoria_direccion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.auditoria_direccion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditoria_direccion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.auditoria_direccion_id_seq OWNED BY public.auditoria_direccion.id;


--
-- Name: centro_distribucion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.centro_distribucion (
    id integer NOT NULL,
    nombre character varying(64) NOT NULL,
    direccion character varying(128) NOT NULL,
    ciudad character varying(64) NOT NULL
);


--
-- Name: centro_distribucion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.centro_distribucion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: centro_distribucion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.centro_distribucion_id_seq OWNED BY public.centro_distribucion.id;


--
-- Name: evento_seguimiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evento_seguimiento (
    id integer NOT NULL,
    paquete_id integer NOT NULL,
    estado character varying(32) NOT NULL,
    descripcion character varying(255) NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: evento_seguimiento_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.evento_seguimiento_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: evento_seguimiento_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.evento_seguimiento_id_seq OWNED BY public.evento_seguimiento.id;


--
-- Name: paquete; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.paquete (
    id integer NOT NULL,
    codigo character varying(64) DEFAULT ('G5-'::text || replace((gen_random_uuid())::text, '-'::text, ''::text)) NOT NULL,
    estado character varying(32) DEFAULT 'Registrado'::character varying NOT NULL,
    peso numeric(10,2) NOT NULL,
    volumen numeric(10,2) NOT NULL,
    direccion_destino character varying(255) NOT NULL,
    intentos integer DEFAULT 0 NOT NULL,
    fecha_compromiso timestamp without time zone NOT NULL,
    fecha_reprogramada timestamp without time zone,
    despachado_at timestamp without time zone,
    entregado_at timestamp without time zone,
    cliente_id integer NOT NULL,
    centro_id integer NOT NULL,
    paquete_origen_id integer,
    CONSTRAINT ck_paquete_intentos CHECK ((intentos >= 0)),
    CONSTRAINT ck_paquete_no_auto_origen CHECK ((paquete_origen_id IS DISTINCT FROM id)),
    CONSTRAINT ck_paquete_peso CHECK ((peso > (0)::numeric)),
    CONSTRAINT ck_paquete_volumen CHECK ((volumen > (0)::numeric)),
    CONSTRAINT paquete_estado_check CHECK (((estado)::text = ANY ((ARRAY['Registrado'::character varying, 'En centro'::character varying, 'En reparto'::character varying, 'Entregado'::character varying, 'Reprogramado'::character varying, 'Devuelto a origen'::character varying])::text[])))
);


--
-- Name: repartidor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.repartidor (
    id integer NOT NULL,
    usuario_id integer NOT NULL,
    centro_id integer NOT NULL
);


--
-- Name: ruta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ruta (
    id integer NOT NULL,
    centro_id integer NOT NULL,
    repartidor_id integer NOT NULL,
    vehiculo_id integer NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: ruta_paquete; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ruta_paquete (
    ruta_id integer NOT NULL,
    paquete_id integer NOT NULL,
    resultado character varying(32) DEFAULT 'Pendiente'::character varying NOT NULL,
    fecha_resultado timestamp without time zone,
    CONSTRAINT ck_ruta_paquete_resultado_fecha CHECK (((((resultado)::text = 'Pendiente'::text) AND (fecha_resultado IS NULL)) OR (((resultado)::text <> 'Pendiente'::text) AND (fecha_resultado IS NOT NULL)))),
    CONSTRAINT ruta_paquete_resultado_check CHECK (((resultado)::text = ANY ((ARRAY['Pendiente'::character varying, 'Entregado'::character varying, 'Fallido'::character varying])::text[])))
);


--
-- Name: usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario (
    id integer NOT NULL,
    nombre character varying(64) NOT NULL,
    email character varying(128) NOT NULL,
    rol character varying(16) NOT NULL,
    keycloak_id character varying(128) NOT NULL,
    CONSTRAINT usuario_rol_check CHECK (((rol)::text = ANY ((ARRAY['cliente'::character varying, 'repartidor'::character varying, 'despachador'::character varying])::text[])))
);


--
-- Name: mv_rendimiento_entregas; Type: MATERIALIZED VIEW; Schema: public; Owner: -
--

CREATE MATERIALIZED VIEW public.mv_rendimiento_entregas AS
 SELECT r.centro_id,
    c.nombre AS centro_nombre,
    r.repartidor_id,
    u.nombre AS repartidor_nombre,
    (count(*) FILTER (WHERE ((rp.resultado)::text = 'Entregado'::text)))::integer AS entregas_totales,
    (count(*) FILTER (WHERE (((rp.resultado)::text = 'Entregado'::text) AND (rp.fecha_resultado <= p.fecha_compromiso))))::integer AS entregas_a_tiempo,
    (count(*))::integer AS intentos_totales,
    (count(DISTINCT p.id))::integer AS paquetes_atendidos,
    array_agg(DISTINCT p.id) AS paquete_ids,
    round(((100.0 * (count(*) FILTER (WHERE (((rp.resultado)::text = 'Entregado'::text) AND (rp.fecha_resultado <= p.fecha_compromiso))))::numeric) / (NULLIF(count(*) FILTER (WHERE ((rp.resultado)::text = 'Entregado'::text)), 0))::numeric), 2) AS pct_a_tiempo,
    round(((count(*))::numeric / (NULLIF(count(DISTINCT p.id), 0))::numeric), 2) AS intentos_promedio
   FROM (((((public.ruta_paquete rp
     JOIN public.ruta r ON ((r.id = rp.ruta_id)))
     JOIN public.paquete p ON ((p.id = rp.paquete_id)))
     JOIN public.centro_distribucion c ON ((c.id = r.centro_id)))
     JOIN public.repartidor rep ON ((rep.id = r.repartidor_id)))
     JOIN public.usuario u ON ((u.id = rep.usuario_id)))
  WHERE ((rp.resultado)::text = ANY ((ARRAY['Entregado'::character varying, 'Fallido'::character varying])::text[]))
  GROUP BY r.centro_id, c.nombre, r.repartidor_id, u.nombre
  WITH NO DATA;


--
-- Name: paquete_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.paquete_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: paquete_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.paquete_id_seq OWNED BY public.paquete.id;


--
-- Name: repartidor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.repartidor_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: repartidor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.repartidor_id_seq OWNED BY public.repartidor.id;


--
-- Name: ruta_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ruta_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ruta_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ruta_id_seq OWNED BY public.ruta.id;


--
-- Name: usuario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.usuario_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.usuario_id_seq OWNED BY public.usuario.id;


--
-- Name: vehiculo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vehiculo (
    id integer NOT NULL,
    patente character varying(16) NOT NULL,
    capacidad_peso numeric(10,2) NOT NULL,
    capacidad_volumen numeric(10,2) NOT NULL,
    centro_id integer NOT NULL,
    CONSTRAINT ck_vehiculo_capacidad_peso CHECK ((capacidad_peso > (0)::numeric)),
    CONSTRAINT ck_vehiculo_capacidad_volumen CHECK ((capacidad_volumen > (0)::numeric))
);


--
-- Name: vehiculo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vehiculo_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vehiculo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vehiculo_id_seq OWNED BY public.vehiculo.id;


--
-- Name: auditoria_direccion id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_direccion ALTER COLUMN id SET DEFAULT nextval('public.auditoria_direccion_id_seq'::regclass);


--
-- Name: centro_distribucion id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.centro_distribucion ALTER COLUMN id SET DEFAULT nextval('public.centro_distribucion_id_seq'::regclass);


--
-- Name: evento_seguimiento id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evento_seguimiento ALTER COLUMN id SET DEFAULT nextval('public.evento_seguimiento_id_seq'::regclass);


--
-- Name: paquete id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paquete ALTER COLUMN id SET DEFAULT nextval('public.paquete_id_seq'::regclass);


--
-- Name: repartidor id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.repartidor ALTER COLUMN id SET DEFAULT nextval('public.repartidor_id_seq'::regclass);


--
-- Name: ruta id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta ALTER COLUMN id SET DEFAULT nextval('public.ruta_id_seq'::regclass);


--
-- Name: usuario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario ALTER COLUMN id SET DEFAULT nextval('public.usuario_id_seq'::regclass);


--
-- Name: vehiculo id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehiculo ALTER COLUMN id SET DEFAULT nextval('public.vehiculo_id_seq'::regclass);


--
-- Data for Name: auditoria_direccion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auditoria_direccion (id, paquete_id, direccion_anterior, direccion_nueva, usuario_id, fecha) FROM stdin;
\.


--
-- Data for Name: centro_distribucion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.centro_distribucion (id, nombre, direccion, ciudad) FROM stdin;
1	CD Santiago G5	Alameda 1000	Santiago
2	CD Valparaiso G5	Errazuriz 700	Valparaiso
\.


--
-- Data for Name: evento_seguimiento; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.evento_seguimiento (id, paquete_id, estado, descripcion, fecha) FROM stdin;
1	1	Registrado	Alta de demostración	2026-11-02 02:42:02.846737
2	1	En reparto	Asignado a la ruta 1	2026-11-02 02:42:02.916508
3	1	Entregado	Entregado al destinatario	2026-11-02 02:42:02.917346
4	2	Registrado	Alta de demostración	2026-11-02 02:42:02.846737
5	2	En reparto	Asignado a la ruta 2	2026-11-02 02:42:02.919546
6	2	Entregado	Entregado al destinatario	2026-11-02 02:42:02.919964
7	3	Registrado	Alta de demostración	2026-11-02 02:42:02.846737
8	3	En reparto	Asignado a la ruta 3	2026-11-02 02:42:02.921581
9	3	Entregado	Entregado al destinatario	2026-11-02 02:42:02.922229
10	4	Registrado	Alta de demostración	2026-11-02 02:42:02.846737
11	4	En reparto	Asignado a la ruta 4	2026-11-02 02:42:02.923717
12	4	Entregado	Entregado al destinatario	2026-11-02 02:42:02.924007
13	5	Registrado	Alta de demostración	2026-11-02 02:42:02.846737
14	5	En reparto	Asignado a la ruta 5	2026-11-02 02:42:02.925859
\.


--
-- Data for Name: paquete; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.paquete (id, codigo, estado, peso, volumen, direccion_destino, intentos, fecha_compromiso, fecha_reprogramada, despachado_at, entregado_at, cliente_id, centro_id, paquete_origen_id) FROM stdin;
1	G5-DEMO-1	Entregado	2.00	0.50	Destino demostración 1	0	2026-11-03 02:42:02.846737	\N	2026-11-02 02:42:02.846737	2026-11-02 02:42:02.846737	4	1	\N
2	G5-DEMO-2	Entregado	2.00	0.50	Destino demostración 2	0	2026-11-01 02:42:02.846737	\N	2026-11-02 02:42:02.846737	2026-11-02 02:42:02.846737	4	1	\N
3	G5-DEMO-3	Entregado	2.00	0.50	Destino demostración 3	0	2026-11-03 02:42:02.846737	\N	2026-11-02 02:42:02.846737	2026-11-02 02:42:02.846737	4	1	\N
4	G5-DEMO-4	Entregado	2.00	0.50	Destino demostración 4	0	2026-11-01 02:42:02.846737	\N	2026-11-02 02:42:02.846737	2026-11-02 02:42:02.846737	4	1	\N
5	G5-DEMO-5	En reparto	2.00	0.50	Destino demostración 5	0	2026-11-03 02:42:02.846737	\N	2026-11-02 02:42:02.846737	\N	4	1	\N
\.


--
-- Data for Name: repartidor; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.repartidor (id, usuario_id, centro_id) FROM stdin;
1	2	1
2	3	1
\.


--
-- Data for Name: ruta; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ruta (id, centro_id, repartidor_id, vehiculo_id, fecha) FROM stdin;
1	1	1	1	2026-11-02 02:42:02.846737
2	1	1	1	2026-11-02 02:42:02.846737
3	1	2	1	2026-11-02 02:42:02.846737
4	1	2	1	2026-11-02 02:42:02.846737
5	1	2	1	2026-11-02 02:42:02.846737
\.


--
-- Data for Name: ruta_paquete; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ruta_paquete (ruta_id, paquete_id, resultado, fecha_resultado) FROM stdin;
1	1	Entregado	2026-11-02 02:42:02.846737
2	2	Entregado	2026-11-02 02:42:02.846737
3	3	Entregado	2026-11-02 02:42:02.846737
4	4	Entregado	2026-11-02 02:42:02.846737
5	5	Pendiente	\N
\.


--
-- Data for Name: usuario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.usuario (id, nombre, email, rol, keycloak_id) FROM stdin;
1	despachador	despachador@grupo5.cl	despachador	50000000-0000-4000-8000-000000000001
2	repartidor1	repartidor1@grupo5.cl	repartidor	50000000-0000-4000-8000-000000000002
3	repartidor2	repartidor2@grupo5.cl	repartidor	50000000-0000-4000-8000-000000000003
4	cliente1	cliente1@grupo5.cl	cliente	50000000-0000-4000-8000-000000000004
5	cliente2	cliente2@grupo5.cl	cliente	50000000-0000-4000-8000-000000000005
\.


--
-- Data for Name: vehiculo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vehiculo (id, patente, capacidad_peso, capacidad_volumen, centro_id) FROM stdin;
1	G5-TEST	100.00	10.00	1
\.


--
-- Name: auditoria_direccion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auditoria_direccion_id_seq', 1, false);


--
-- Name: centro_distribucion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.centro_distribucion_id_seq', 2, true);


--
-- Name: evento_seguimiento_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.evento_seguimiento_id_seq', 14, true);


--
-- Name: paquete_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.paquete_id_seq', 5, true);


--
-- Name: repartidor_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.repartidor_id_seq', 2, true);


--
-- Name: ruta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ruta_id_seq', 5, true);


--
-- Name: usuario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.usuario_id_seq', 5, true);


--
-- Name: vehiculo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.vehiculo_id_seq', 1, true);


--
-- Name: auditoria_direccion auditoria_direccion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_direccion
    ADD CONSTRAINT auditoria_direccion_pkey PRIMARY KEY (id);


--
-- Name: centro_distribucion centro_distribucion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.centro_distribucion
    ADD CONSTRAINT centro_distribucion_pkey PRIMARY KEY (id);


--
-- Name: evento_seguimiento evento_seguimiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evento_seguimiento
    ADD CONSTRAINT evento_seguimiento_pkey PRIMARY KEY (id);


--
-- Name: paquete paquete_codigo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paquete
    ADD CONSTRAINT paquete_codigo_key UNIQUE (codigo);


--
-- Name: paquete paquete_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paquete
    ADD CONSTRAINT paquete_pkey PRIMARY KEY (id);


--
-- Name: ruta_paquete pk_ruta_paquete; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta_paquete
    ADD CONSTRAINT pk_ruta_paquete PRIMARY KEY (ruta_id, paquete_id);


--
-- Name: repartidor repartidor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.repartidor
    ADD CONSTRAINT repartidor_pkey PRIMARY KEY (id);


--
-- Name: repartidor repartidor_usuario_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.repartidor
    ADD CONSTRAINT repartidor_usuario_id_key UNIQUE (usuario_id);


--
-- Name: ruta ruta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta
    ADD CONSTRAINT ruta_pkey PRIMARY KEY (id);


--
-- Name: usuario usuario_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_email_key UNIQUE (email);


--
-- Name: usuario usuario_keycloak_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_keycloak_id_key UNIQUE (keycloak_id);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);


--
-- Name: vehiculo vehiculo_patente_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehiculo
    ADD CONSTRAINT vehiculo_patente_key UNIQUE (patente);


--
-- Name: vehiculo vehiculo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehiculo
    ADD CONSTRAINT vehiculo_pkey PRIMARY KEY (id);


--
-- Name: idx_evento_seguimiento_paquete_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_evento_seguimiento_paquete_fecha ON public.evento_seguimiento USING btree (paquete_id, fecha, id);


--
-- Name: idx_mv_rendimiento_centro_repartidor; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_mv_rendimiento_centro_repartidor ON public.mv_rendimiento_entregas USING btree (centro_id, repartidor_id);


--
-- Name: idx_paquete_codigo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paquete_codigo ON public.paquete USING hash (codigo);


--
-- Name: idx_paquete_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_paquete_estado ON public.paquete USING btree (estado);


--
-- Name: uq_ruta_paquete_pendiente; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_ruta_paquete_pendiente ON public.ruta_paquete USING btree (paquete_id) WHERE ((resultado)::text = 'Pendiente'::text);


--
-- Name: auditoria_direccion auditoria_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER auditoria_inmutable BEFORE DELETE OR UPDATE ON public.auditoria_direccion FOR EACH ROW EXECUTE FUNCTION public.proteger_auditoria();


--
-- Name: vehiculo capacidad_modificada; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER capacidad_modificada BEFORE UPDATE ON public.vehiculo FOR EACH ROW EXECUTE FUNCTION public.proteger_vehiculo();


--
-- Name: ruta_paquete coherencia_asignacion; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER coherencia_asignacion BEFORE INSERT OR DELETE OR UPDATE ON public.ruta_paquete FOR EACH ROW EXECUTE FUNCTION public.proteger_asignacion();


--
-- Name: ruta coherencia_ruta; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER coherencia_ruta BEFORE INSERT OR UPDATE ON public.ruta FOR EACH ROW EXECUTE FUNCTION public.proteger_ruta();


--
-- Name: paquete destination_audit_before_update; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER destination_audit_before_update BEFORE UPDATE OF direccion_destino ON public.paquete FOR EACH ROW EXECUTE FUNCTION public.audit_destination_change();


--
-- Name: repartidor perfil_repartidor; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER perfil_repartidor BEFORE INSERT OR UPDATE ON public.repartidor FOR EACH ROW EXECUTE FUNCTION public.validar_perfiles();


--
-- Name: usuario perfil_usuario; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER perfil_usuario BEFORE UPDATE ON public.usuario FOR EACH ROW EXECUTE FUNCTION public.validar_perfiles();


--
-- Name: paquete proteger_ciclo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER proteger_ciclo BEFORE INSERT OR UPDATE ON public.paquete FOR EACH ROW EXECUTE FUNCTION public.proteger_paquete();


--
-- Name: ruta_paquete route_capacity_before_insert; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER route_capacity_before_insert BEFORE INSERT ON public.ruta_paquete FOR EACH ROW EXECUTE FUNCTION public.check_route_capacity();


--
-- Name: auditoria_direccion fk_auditoria_paquete; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_direccion
    ADD CONSTRAINT fk_auditoria_paquete FOREIGN KEY (paquete_id) REFERENCES public.paquete(id);


--
-- Name: auditoria_direccion fk_auditoria_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auditoria_direccion
    ADD CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


--
-- Name: evento_seguimiento fk_evento_paquete; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evento_seguimiento
    ADD CONSTRAINT fk_evento_paquete FOREIGN KEY (paquete_id) REFERENCES public.paquete(id);


--
-- Name: paquete fk_paquete_centro; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paquete
    ADD CONSTRAINT fk_paquete_centro FOREIGN KEY (centro_id) REFERENCES public.centro_distribucion(id);


--
-- Name: paquete fk_paquete_cliente; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paquete
    ADD CONSTRAINT fk_paquete_cliente FOREIGN KEY (cliente_id) REFERENCES public.usuario(id);


--
-- Name: paquete fk_paquete_origen; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.paquete
    ADD CONSTRAINT fk_paquete_origen FOREIGN KEY (paquete_origen_id) REFERENCES public.paquete(id) ON DELETE RESTRICT;


--
-- Name: repartidor fk_repartidor_centro; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.repartidor
    ADD CONSTRAINT fk_repartidor_centro FOREIGN KEY (centro_id) REFERENCES public.centro_distribucion(id);


--
-- Name: repartidor fk_repartidor_usuario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.repartidor
    ADD CONSTRAINT fk_repartidor_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);


--
-- Name: ruta fk_ruta_centro; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta
    ADD CONSTRAINT fk_ruta_centro FOREIGN KEY (centro_id) REFERENCES public.centro_distribucion(id);


--
-- Name: ruta_paquete fk_ruta_paquete_paquete; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta_paquete
    ADD CONSTRAINT fk_ruta_paquete_paquete FOREIGN KEY (paquete_id) REFERENCES public.paquete(id) ON DELETE RESTRICT;


--
-- Name: ruta_paquete fk_ruta_paquete_ruta; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta_paquete
    ADD CONSTRAINT fk_ruta_paquete_ruta FOREIGN KEY (ruta_id) REFERENCES public.ruta(id) ON DELETE RESTRICT;


--
-- Name: ruta fk_ruta_repartidor; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta
    ADD CONSTRAINT fk_ruta_repartidor FOREIGN KEY (repartidor_id) REFERENCES public.repartidor(id);


--
-- Name: ruta fk_ruta_vehiculo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ruta
    ADD CONSTRAINT fk_ruta_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES public.vehiculo(id);


--
-- Name: vehiculo fk_vehiculo_centro; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vehiculo
    ADD CONSTRAINT fk_vehiculo_centro FOREIGN KEY (centro_id) REFERENCES public.centro_distribucion(id);


--
-- Name: mv_rendimiento_entregas; Type: MATERIALIZED VIEW DATA; Schema: public; Owner: -
--

REFRESH MATERIALIZED VIEW public.mv_rendimiento_entregas;


--
-- PostgreSQL database dump complete
--

\unrestrict dH3o8EQB77UAZJRMeXrEpFwWdIBLbNgh8lL8WYHbKjers1xb1kHayh0Rl2XF8Ao

