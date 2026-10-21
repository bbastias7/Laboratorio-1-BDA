# Laboratorio 1 — Taller de Bases de Datos / BDA

Repositorio: https://github.com/bbastias7/Laboratorio-1-BDA

## Estado del proyecto

> **Estructura inicial.** Esta plantilla organiza los entregables; todavía **no** contiene una API, una aplicación web ni una base de datos ejecutables. Antes de la entrega, este README debe incluir los comandos **reales y probados** para instalar y ejecutar el sistema desde cero.

## Descripción

Proyecto de sistema de información con arquitectura multicapa:
- **Backend:** API REST con autenticación JWT y autorización basada en roles (RBAC), **sin ORM**.
- **Frontend:** interfaz web que consume la API.
- **Base de datos:** PostgreSQL, con lógica SQL y objetos programables.

**Dominio asignado:** _[COMPLETAR: grupo 1, 2, 3, 4 o 5 y nombre del sistema]_

## Estructura

```text
Laboratorio-1-BDA/
├── backend/
│   ├── routes/                # Definición de endpoints
│   ├── controllers/           # Manejo de solicitudes y respuestas
│   ├── services/              # Reglas de negocio y coordinación
│   ├── middleware/            # JWT, roles, errores, etc.
│   ├── config/                # Configuración del servidor
│   ├── db/                    # Conexión y consultas SQL directas
│   └── tests/                 # Pruebas de API
├── frontend/
│   ├── public/                # Recursos públicos
│   └── src/
│       ├── components/        # Componentes reutilizables
│       ├── pages/             # Pantallas
│       ├── services/          # Cliente HTTP de la API
│       └── assets/            # Recursos locales
├── database/
│   ├── schema/                # Tablas, relaciones y restricciones
│   ├── triggers/              # Triggers y funciones asociadas
│   ├── procedures/            # Procedimientos almacenados
│   ├── materialized_views/    # Vista materializada y refresco
│   ├── indexes/               # Índices
│   └── seeders/               # Datos de prueba
├── presentacion/              # Diapositivas de la defensa (PDF/PPTX)
├── .env.example               # Ejemplo de variables (sin claves reales)
├── .gitignore
└── README.md
```

Las carpetas vacías contienen `.gitkeep` para que Git las pueda seguir. Al comenzar a programar, esos archivos pueden eliminarse.

## Tecnologías utilizadas

| Capa | Tecnología |
| --- | --- |
| Base de datos | PostgreSQL |
| Backend | _[COMPLETAR: lenguaje y framework elegidos, sin ORM]_ |
| Frontend | _[COMPLETAR: React, Vue, Angular o Vanilla JS]_ |
| Autenticación | JWT |
| Autorización | RBAC |

## Instalación y despliegue local

> **ATENCIÓN:** Los comandos de ejecución dependen del stack que el grupo elija. No se incluyen comandos ficticios: completar y validar esta sección es obligatorio antes de entregar.

### 1. Clonar el repositorio

```bash
git clone https://github.com/bbastias7/Laboratorio-1-BDA.git
cd Laboratorio-1-BDA
```

### 2. Variables de entorno

Crear el archivo `.env` a partir de `.env.example`, y configurar los valores locales. **No subir** el `.env` ni credenciales reales al repositorio.

### 3. Iniciar PostgreSQL y preparar la base de datos

- Versión de PostgreSQL probada: _[COMPLETAR]_
- Nombre de la base de datos: _[COMPLETAR]_
- Comando o procedimiento exacto para ejecutar los scripts de `database/` en orden: _[COMPLETAR]_
- Comando para cargar datos de prueba: _[COMPLETAR]_

### 4. Levantar el backend

- Prerrequisitos y versiones: _[COMPLETAR]_
- Instalación de dependencias: _[COMPLETAR COMANDO REAL]_
- Comando para arrancar: _[COMPLETAR COMANDO REAL]_
- URL local de la API: _[COMPLETAR]_

### 5. Levantar el frontend

- Prerrequisitos y versiones: _[COMPLETAR]_
- Instalación de dependencias: _[COMPLETAR COMANDO REAL]_
- Comando para arrancar: _[COMPLETAR COMANDO REAL]_
- URL de acceso: _[COMPLETAR]_

### 6. Comprobación desde cero

- [ ] Probar la instalación en una carpeta o máquina limpia, usando solo este README.
- [ ] Comprobar conexión del backend a PostgreSQL.
- [ ] Comprobar que el frontend consume la API.
- [ ] Verificar login JWT y acceso por roles.
- [ ] Verificar operaciones CRUD y casos propios del grupo.

## Base de datos — requisitos técnicos

- [ ] Esquema relacional de PostgreSQL, tablas y relaciones.
- [ ] Datos de prueba (seeders).
- [ ] **Al menos 2 triggers:** uno de auditoría y uno de reglas de negocio.
- [ ] **Al menos 2 procedimientos almacenados:** transacciones atómicas entre varias tablas.
- [ ] **Al menos 1 vista materializada** para reportes, con lógica de refresco.
- [ ] **Índices en al menos 3 columnas críticas** para optimizar consultas.
- [ ] Scripts `.sql` incluidos en `database/` y documentados en el orden correcto.

## API REST — documentación

Documentar aquí cada endpoint implementado. Ejemplo de formato (sustituir por rutas reales):

| Método | Ruta | Descripción | Rol autorizado |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | _[COMPLETAR según implementación]_ | Público |
| `GET` | `/api/...` | _[COMPLETAR]_ | _[COMPLETAR]_ |

### Ejemplo de solicitud y respuesta JSON

_[COMPLETAR con ejemplos reales y verificables, códigos de respuesta y uso del token JWT.]_

## Presentación

Subir las diapositivas para la defensa en `presentacion/`, en formato PDF o PPTX.

## Integrantes

_[COMPLETAR nombres e información acordada por el grupo.]_

## Criterio crítico de la pauta

El proyecto debe **instalarse y ejecutarse únicamente siguiendo este README**. Si la instalación desde cero falla, el laboratorio puede recibir la nota mínima según el enunciado. Antes de enviar, reemplazar todos los marcadores _[COMPLETAR]_ y probar el procedimiento completo.
