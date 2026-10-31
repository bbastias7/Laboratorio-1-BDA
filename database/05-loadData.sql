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

