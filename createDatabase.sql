CREATE TABLE usuario (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(64) NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(16) NOT NULL
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
    CONSTRAINT fk_vehiculo_centro FOREIGN KEY (centro_id) REFERENCES centro_distribucion(id)
);


CREATE TABLE paquete (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(32) NOT NULL DEFAULT 'Registrado',
    peso NUMERIC(10, 2) NOT NULL,
    volumen NUMERIC(10, 2) NOT NULL,
    direccion_destino VARCHAR(255) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    fecha_compromiso TIMESTAMP,
    despachado_at TIMESTAMP,
    entregado_at TIMESTAMP,
    cliente_id INT NOT NULL,
    centro_id INT NOT NULL,
    paquete_origen_id INT,
    CONSTRAINT fk_paquete_cliente FOREIGN KEY (cliente_id) REFERENCES usuario(id),
    CONSTRAINT fk_paquete_centro FOREIGN KEY (centro_id) REFERENCES centro_distribucion(id),
    CONSTRAINT fk_paquete_origen FOREIGN KEY (paquete_origen_id) REFERENCES paquete(id) ON DELETE SET NULL
);


CREATE TABLE ruta (
    id SERIAL PRIMARY KEY,
    centro_id INT NOT NULL,
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
    resultado VARCHAR(16) NOT NULL DEFAULT 'Pendiente',
    fecha_resultado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ruta_paquete PRIMARY KEY (ruta_id, paquete_id),
    CONSTRAINT fk_ruta_paquete_ruta FOREIGN KEY (ruta_id) REFERENCES ruta(id) ON DELETE CASCADE,
    CONSTRAINT fk_ruta_paquete_paquete FOREIGN KEY (paquete_id) REFERENCES paquete(id) ON DELETE CASCADE
);