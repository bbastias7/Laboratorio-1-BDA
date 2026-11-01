# Plataforma de Logística y Seguimiento de Envíos (Grupo 5)

Aplicación web y API REST para gestionar la logística de última milla: centros de distribución, paquetes, vehículos, repartidores, rutas de reparto y eventos de seguimiento. Un cliente registra paquetes y consulta su seguimiento, un despachador asigna rutas y controla el desempeño, y un repartidor registra entregas exitosas o fallidas. Las reglas de negocio críticas (capacidad de vehículos, reintentos de entrega, auditoría de cambios de destino) se aplican en la base de datos con procedimientos almacenados y triggers.

## Arquitectura y tecnologías

```
Frontend React (Vite, :5173) / curl
        |  HTTP + JWT (Bearer)        ^ login (código + PKCE)
        v                             |
+-----------------------------------------------+        +------------------+
| Backend Spring Boot 4.1.1 (Java 21)           |  JWKS  | Keycloak 26      |
| controllers -> services -> repositories       |<------>| realm logistica-g5|
| (JdbcTemplate, sin ORM)                       |        +------------------+
+----------------------+------------------------+
                       | JDBC
                       v
              PostgreSQL (logistica_g5)
   tablas, procedimientos, triggers, vista materializada
```

- Frontend: React 18 + Vite + React Router, `keycloak-js` (flujo de código con PKCE, cliente `logistica-web`) y GSAP para la animación del seguimiento. Un solo diseño responsive con modo claro y oscuro para los tres roles.
- Backend: Spring Boot 4.1.1, Java 21, Spring JDBC (`JdbcTemplate`), Bean Validation, Lombok.
- Seguridad: Spring Security como Resource Server OAuth2. Valida los JWT de Keycloak (firma por JWKS, `issuer` y audiencia `logistica-api`). El `sub` del token se vincula a `usuario.keycloak_id` y el rol del token debe coincidir con el rol del perfil local.
- Base de datos: PostgreSQL.
- Identidad: Keycloak (realm `logistica-g5`).

Estructura del repositorio:

| Ruta | Contenido |
|---|---|
| `backend/` | Código fuente Spring Boot, `mvnw`, `pom.xml` y `tests/` (pruebas de reglas e integración) |
| `database/` | Scripts SQL por etapas (`01` esquema, `02` vista materializada, `03` procedimientos, `04` triggers, `05` datos iniciales) |
| `createDatabase.sql` | Instalación completa: equivale a `database/01` a `05` en una sola transacción |
| `frontend/` | Aplicación web React + Vite (`src/`, `design/` con el sistema de diseño, `tokens/`, `.env.example`) |
| `keycloak/` | Export del realm (`logistica-g5-realm.json`) y script para Windows (`start-keycloak.ps1`) |

La API permite el origen del frontend mediante la variable `CORS_ORIGINS` (por defecto `http://localhost:5173`).

## Requisitos previos

| Herramienta | Versión | Uso |
|---|---|---|
| JDK | 21 o superior | Compilar y ejecutar el backend (Maven se descarga con `mvnw`) |
| PostgreSQL | 14 o superior (verificado con 16) | Base de datos. Los procedimientos usan parámetros `INOUT` con valor por defecto |
| Docker | Cualquier versión reciente | Keycloak y, opcionalmente, PostgreSQL |
| `psql` | Cliente de PostgreSQL | Cargar el script (no es necesario si PostgreSQL corre en Docker; ver paso 3) |
| `curl` | Cualquiera | Verificación rápida |
| Node.js | 18, 20 o 22 y superiores (el campo `engines` de Vite 6 exige `^18 || ^20 || >=22`); verificado con 25 | Ejecutar y compilar el frontend (incluye `npm`) |
| Python 3 | 3.8 o superior | Solo para las pruebas `backend/tests/verify_api.py` y `concurrencia.py` |

Puertos usados por defecto: PostgreSQL 5432, backend 8080, Keycloak 8081, frontend 5173.

## Manual de instalación y despliegue

### 1. Clonar el repositorio

```bash
git clone https://github.com/bbastias7/Laboratorio-1-BDA.git
cd Laboratorio-1-BDA
```

### 2. Crear la base de datos y el usuario

Opción A: PostgreSQL instalado localmente.

```bash
psql -U postgres -c "CREATE ROLE logistica LOGIN PASSWORD 'logistica';"
psql -U postgres -c "CREATE DATABASE logistica_g5 OWNER logistica;"
```

Opción B: PostgreSQL en Docker (crea el usuario y la base automáticamente).

```bash
docker run -d --name logistica-pg \
  -e POSTGRES_USER=logistica -e POSTGRES_PASSWORD=logistica -e POSTGRES_DB=logistica_g5 \
  -p 5432:5432 postgres:16-alpine
```

En PowerShell la continuación de línea es el acento grave (`` ` ``), no `\`:

```powershell
docker run -d --name logistica-pg `
  -e POSTGRES_USER=logistica -e POSTGRES_PASSWORD=logistica -e POSTGRES_DB=logistica_g5 `
  -p 5432:5432 postgres:16-alpine
```

Si el puerto 5432 está ocupado, cambie el puerto del anfitrión (por ejemplo `-p 5433:5432`) y defina `DB_PORT=5433` en el paso 5.

### 3. Cargar el esquema y los datos iniciales

Ejecute el script completo sobre la base vacía, desde la raíz del repositorio:

```bash
PGPASSWORD=logistica psql -h localhost -p 5432 -U logistica -d logistica_g5 \
  -v ON_ERROR_STOP=1 -f createDatabase.sql
```

En PowerShell:

```powershell
$env:PGPASSWORD='logistica'
psql -h localhost -p 5432 -U logistica -d logistica_g5 -v ON_ERROR_STOP=1 -f createDatabase.sql
```

La última línea de la salida debe ser `COMMIT`. Los avisos `NOTICE ... does not exist, skipping` son normales.

Si no tiene `psql` y usó la opción B:

```bash
docker exec -i logistica-pg psql -U logistica -d logistica_g5 -v ON_ERROR_STOP=1 < createDatabase.sql
```

En PowerShell el operador `<` no existe; copie el script al contenedor y ejecútelo ahí (funciona en cualquier terminal):

```powershell
docker cp createDatabase.sql logistica-pg:/tmp/createDatabase.sql
docker exec logistica-pg psql -U logistica -d logistica_g5 -v ON_ERROR_STOP=1 -f /tmp/createDatabase.sql
```

El script crea las tablas, la vista materializada, los procedimientos y los triggers, y carga datos de demostración: 5 perfiles de usuario, 2 centros de distribución, 2 repartidores, 1 vehículo y paquetes de ejemplo. Debe usarse una sola vez sobre una base vacía. Para reinstalar, elimine y vuelva a crear la base.

### 4. Iniciar Keycloak

Con Docker (Windows, macOS y Linux). Ejecute el comando desde la raíz del repositorio:

```bash
docker run -d --name logistica-kc -p 8081:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -e KC_HOSTNAME=http://localhost:8081 \
  -v "$(pwd)/keycloak/logistica-g5-realm.json:/opt/keycloak/data/import/logistica-g5-realm.json:ro" \
  quay.io/keycloak/keycloak:26.4 start-dev --import-realm
```

En PowerShell (Windows) use esta versión: la continuación de línea es el acento grave (`` ` ``) y la ruta actual es `${PWD}`. Si copia el comando con `\`, Docker responde `invalid reference format`.

```powershell
docker run -d --name logistica-kc -p 8081:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin `
  -e KC_HOSTNAME=http://localhost:8081 `
  -v "${PWD}/keycloak/logistica-g5-realm.json:/opt/keycloak/data/import/logistica-g5-realm.json:ro" `
  quay.io/keycloak/keycloak:26.4 start-dev --import-realm
```

Espere a que el contenedor termine de iniciar (unos 15 a 30 segundos). Este comando termina cuando Keycloak está listo:

```bash
until docker logs logistica-kc 2>&1 | grep -q "started in"; do sleep 2; done
```

En PowerShell:

```powershell
while (-not (docker logs logistica-kc 2>&1 | Select-String -Quiet "started in")) { Start-Sleep 2 }
```

Compruebe el realm (en PowerShell escriba `curl.exe`, porque `curl` es un alias de `Invoke-WebRequest`):

```bash
curl -s http://localhost:8081/realms/logistica-g5/.well-known/openid-configuration
```

Debe responder un JSON con `"issuer":"http://localhost:8081/realms/logistica-g5"`. Si responde `{"error":"invalid_request","error_description":"HTTPS required"}`, aplique el paso de solución de problemas siguiente.

**Solución de problemas: "HTTPS required".** El realm exige HTTPS salvo para solicitudes que Keycloak reconoce como locales o de red privada. En Docker Desktop (macOS y Windows) las solicitudes desde el anfitrión pueden llegar con una IP no reconocida y recibir 403. Solo para desarrollo local, desactive esa exigencia en el realm ya importado (no modifica el archivo JSON):

```bash
docker exec logistica-kc /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin
docker exec logistica-kc /opt/keycloak/bin/kcadm.sh update realms/logistica-g5 -s sslRequired=NONE
```

En PowerShell, el primer comando en una sola línea:

```powershell
docker exec logistica-kc /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin
docker exec logistica-kc /opt/keycloak/bin/kcadm.sh update realms/logistica-g5 -s sslRequired=NONE
```

Esta configuración se guarda en el contenedor; si lo elimina y lo vuelve a crear, repita el paso. En la verificación de este manual (Docker Desktop en macOS) el paso fue necesario.

El `issuer` del token debe coincidir exactamente con `KEYCLOAK_ISSUER` del backend. Por eso se fija `KC_HOSTNAME` con el puerto publicado: si usa otro puerto de anfitrión, cambie ambos valores.

Alternativa en Windows sin Docker: `keycloak/start-keycloak.ps1` importa el realm y inicia Keycloak en el puerto 8081 desde una distribución local, que por defecto debe estar en `..\..\Herramientas\keycloak-26.6.1` relativo a la carpeta `keycloak/` (parámetro `-KeycloakHome` para otra ruta, `-Port` para otro puerto):

```powershell
powershell -ExecutionPolicy Bypass -File keycloak/start-keycloak.ps1 -KeycloakHome C:\ruta\keycloak-26.6.1
```

### 5. Variables de entorno del backend

Todas son opcionales; los valores por defecto sirven para el despliegue de los pasos anteriores.

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto HTTP de la API |
| `DB_HOST` | `localhost` | Servidor PostgreSQL |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `logistica_g5` | Nombre de la base |
| `DB_USER` | `logistica` | Usuario de la base |
| `DB_PASSWORD` | `logistica` | Contraseña de la base |
| `KEYCLOAK_ISSUER` | `http://localhost:8081/realms/logistica-g5` | Issuer esperado en los JWT; de él se obtienen las claves públicas |
| `KEYCLOAK_AUDIENCE` | `logistica-api` | Audiencia exigida en los JWT |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes permitidos, separados por coma |
| `REFRESH_CRON` | `0 0 3 * * *` | Cron (zona `America/Santiago`) del refresco diario de la vista materializada |

Ejemplo con puertos distintos a los de defecto (Linux/macOS):

```bash
export DB_PORT=5433 KEYCLOAK_ISSUER=http://localhost:8081/realms/logistica-g5
```

En PowerShell: `$env:DB_PORT='5433'`.

### 6. Compilar y ejecutar el backend

Desde `backend/` (requiere JDK 21 o superior; verifique con `java -version`):

```bash
cd backend
chmod +x mvnw          # solo Linux/macOS: Git puede no conservar el permiso de ejecución
./mvnw spring-boot:run
```

En Windows use `mvnw.cmd spring-boot:run`. Alternativa sin `chmod`: `sh mvnw spring-boot:run`.

Para generar y ejecutar el JAR:

```bash
./mvnw package                                  # compila y ejecuta las pruebas unitarias
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

La API queda disponible en `http://localhost:8080` (o el valor de `SERVER_PORT`). La primera ejecución descarga dependencias de Maven y puede tardar algunos minutos.

### 7. Instalar y ejecutar el frontend

Requiere Node.js (ver requisitos), Keycloak y el backend en ejecución. Desde la raíz del repositorio:

```bash
cd frontend
npm install            # o `npm ci` para instalar exactamente lo fijado en package-lock.json
cp .env.example .env   # en PowerShell: Copy-Item .env.example .env
npm run dev
```

Abra `http://localhost:5173`. El puerto 5173 es obligatorio: el cliente `logistica-web` del realm solo acepta `http://localhost:5173/*` como URI de redirección y origen web, y `CORS_ORIGINS` del backend lo permite por defecto. Vite está configurado con `strictPort`, de modo que falla si el puerto está ocupado en lugar de usar otro.

Variables de `.env` (todas opcionales; los valores por defecto sirven para los pasos anteriores):

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080` | URL base del backend (sin `/api`) |
| `VITE_KEYCLOAK_URL` | `http://localhost:8081` | URL de Keycloak |
| `VITE_KEYCLOAK_REALM` | `logistica-g5` | Realm |
| `VITE_KEYCLOAK_CLIENT` | `logistica-web` | Cliente público del realm para el frontend |

Si cambió los puertos del backend o de Keycloak (por ejemplo `SERVER_PORT=58080`), ajuste `VITE_API_URL` y `VITE_KEYCLOAK_URL`; el `issuer` de Keycloak debe seguir coincidiendo con `KEYCLOAK_ISSUER` (paso 4). Las variables `VITE_*` se leen al iniciar `npm run dev` o `npm run build`: reinicie el comando después de cambiarlas.

Versión de producción (también en el puerto 5173):

```bash
npm run build          # genera frontend/dist
npm run preview        # sirve dist en http://localhost:5173
```

`dist/` es estático y puede servirse con cualquier servidor web, siempre desde el origen `http://localhost:5173` mientras el realm no se modifique. La pantalla de inicio de sesión es la página de Keycloak (en inglés, sin tema propio).

### 8. Verificación rápida

Sin token, la API rechaza la solicitud:

```bash
curl -i http://localhost:8080/api/auth/me
# HTTP/1.1 401   {"error":"No autenticado"}
```

Obtenga un token (flujo de contraseña con el cliente público `logistica-cli`) y consulte el perfil:

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/logistica-g5/protocol/openid-connect/token \
  -d grant_type=password -d client_id=logistica-cli \
  -d username=despachador@grupo5.cl -d 'password=Grupo5Demo!' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

En PowerShell:

```powershell
curl.exe -i http://localhost:8080/api/auth/me

$body = @{ grant_type = 'password'; client_id = 'logistica-cli'; username = 'despachador@grupo5.cl'; password = 'Grupo5Demo!' }
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8081/realms/logistica-g5/protocol/openid-connect/token -Body $body).access_token
Invoke-RestMethod http://localhost:8080/api/auth/me -Headers @{ Authorization = "Bearer $token" }
```

Respuesta esperada:

```json
{"usuarioId":1,"nombre":"despachador","email":"despachador@grupo5.cl","rol":"despachador","keycloakId":"50000000-0000-4000-8000-000000000001"}
```

Si obtiene 401 con un token válido, revise que `KEYCLOAK_ISSUER` sea idéntico al `issuer` del token. Si obtiene 403 al pedir el token o `HTTPS required`, vea la solución de problemas del paso 4.

## Uso del frontend

Al abrir `http://localhost:5173` se ve la página de inicio con el campo de código. «Ingresar» lleva a la página de inicio de sesión de Keycloak (el usuario es el correo completo, por ejemplo `cliente1@grupo5.cl`); al volver, cada rol llega a su pantalla inicial. Los envíos nuevos tienen un código `G5-` seguido de 32 caracteres hexadecimales; en pantalla se muestra abreviado en las listas y agrupado en bloques de cuatro en los detalles. El código real es el que se copia y se envía a la API, y el campo de seguimiento acepta códigos pegados con o sin espacios.

| Usuario de prueba | Qué puede hacer |
|---|---|
| `cliente1@grupo5.cl`, `cliente2@grupo5.cl` | Ver sus envíos, crear un envío y copiar su código, cambiar la dirección de destino (bloqueado en estados finales) y rastrear sus propios paquetes |
| `repartidor1@grupo5.cl`, `repartidor2@grupo5.cl` | Ver sus entregas agrupadas por ruta, marcar una entrega como realizada o registrar una entrega fallida (con motivo), y ver sus rutas |
| `despachador@grupo5.cl` | Panel de operación, asignar una ruta con control de capacidad, mantenedores de usuarios, centros, vehículos, repartidores, rutas, asignaciones, eventos y paquetes, auditoría de solo lectura, y reportes de rendimiento con alertas bajo el umbral y refresco de la vista materializada |

Cualquier rol puede rastrear un envío por código en «Rastrear». Un cliente que abre una pantalla de otro rol ve el aviso «No tienes acceso a esta sección». Las cifras de rendimiento provienen de una vista materializada que se refresca cada día a las 03:00 o desde «Refrescar vista»; la API no informa la fecha del último refresco.

## Usuarios de prueba

Definidos en el realm y enlazados por `sub` a los perfiles creados por `createDatabase.sql`. Contraseña de todos: `Grupo5Demo!`.

| Usuario (correo) | Rol |
|---|---|
| `despachador@grupo5.cl` | despachador |
| `repartidor1@grupo5.cl` | repartidor |
| `repartidor2@grupo5.cl` | repartidor |
| `cliente1@grupo5.cl` | cliente |
| `cliente2@grupo5.cl` | cliente |

El realm define también los clientes `logistica-web` (público, flujo de código; para el frontend), `logistica-cli` (público, flujo de contraseña; para pruebas con `curl`) y `logistica-api` (audiencia). Consola de administración de Keycloak: `http://localhost:8081` (usuario `admin`, contraseña `admin` definidos en el comando Docker).

## Roles y permisos (RBAC)

Los roles son roles de realm de Keycloak (`cliente`, `repartidor`, `despachador`). Además, los datos se filtran por propietario: un cliente solo ve sus paquetes y un repartidor solo sus rutas y entregas.

| Rol | Permisos |
|---|---|
| `cliente` | Registrar paquetes propios, ver sus paquetes y su seguimiento, cambiar la dirección de destino de sus paquetes (si no están en estado final), consultar centros de distribución |
| `repartidor` | Ver sus rutas y entregas asignadas (`/api/entregas`), registrar entrega exitosa o fallida, consultar centros de distribución |
| `despachador` | Administración completa: usuarios, repartidores, vehículos, centros, rutas, asignación de rutas, paquetes, eventos, auditoría y reportes |

## Documentación de la API

URL base: `http://localhost:8080/api`. Todas las rutas requieren el encabezado `Authorization: Bearer <token>`. Los cuerpos y respuestas son JSON; las fechas usan formato ISO local (`2026-11-10T18:00:00`, zona `America/Santiago`).

### Endpoints principales

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/auth/me` | autenticado | Perfil del usuario del token |
| GET | `/paquete` | autenticado | Lista paquetes visibles para el rol. Filtros opcionales `estado`, `centroId` |
| GET | `/paquete/{id}` | autenticado | Detalle de un paquete visible |
| POST | `/paquete` | cliente, despachador | Registra un paquete (estado inicial `Registrado`; el código lo genera la base) |
| PUT | `/paquete/{id}` | despachador | Actualiza un paquete |
| DELETE | `/paquete/{id}` | despachador | Elimina un paquete |
| PATCH | `/paquete/{id}/direccion` | cliente, despachador | Cambia la dirección de destino (queda auditada) |
| POST | `/paquete/{id}/entrega-fallida` | repartidor | Registra un intento fallido (procedimiento `registrar_entrega_fallida`) |
| PATCH | `/paquete/{id}/entregado` | repartidor | Registra la entrega exitosa |
| GET | `/tracking/{codigo}` | autenticado | Línea de tiempo del paquete por código de seguimiento |
| GET | `/ruta`, `/ruta/{id}` | despachador, repartidor | Lista o detalle de rutas visibles |
| POST | `/ruta/asignar` | despachador | Crea una ruta y asigna paquetes (procedimiento `asignar_ruta`) |
| POST, PUT, DELETE | `/ruta`, `/ruta/{id}` | despachador | CRUD de rutas |
| GET | `/entregas?soloPendientes=false` | repartidor | Entregas del repartidor autenticado |
| GET | `/centrodistribucion`, `/centrodistribucion/{id}` | autenticado | Consulta de centros |
| POST, PUT, DELETE | `/centrodistribucion`, `/centrodistribucion/{id}` | despachador | CRUD de centros |
| GET, POST, PUT, DELETE | `/vehiculos`, `/vehiculos/{id}` | despachador | CRUD de vehículos |
| GET, POST, PUT, DELETE | `/repartidor`, `/repartidor/{id}` | despachador | CRUD de repartidores |
| GET, POST, PUT, DELETE | `/usuarios`, `/usuarios/{id}` | despachador | CRUD de usuarios. Además `GET /usuarios/email?correo=`, `/usuarios/find?nombre=`, `/usuarios/rol/{rol}` |
| GET, POST, PUT, DELETE | `/eventoseguimiento`, `/eventoseguimiento/{id}` | despachador | CRUD de eventos de seguimiento |
| GET, POST, PUT, DELETE | `/rutapaquete`, `/rutapaquete/{rutaId}/{paqueteId}` | despachador | CRUD de la relación ruta-paquete |
| GET | `/auditoria`, `/auditoria/{id}` | despachador | Auditoría de cambios de dirección (solo lectura; inmutable) |
| GET | `/reportes/rendimiento` | despachador | Rendimiento por centro y repartidor (vista materializada) |
| GET | `/reportes/alertas?umbral=85` | despachador | Repartidores y centros bajo el umbral de entregas a tiempo (%) |
| POST | `/reportes/refrescar` | despachador | Refresca la vista materializada |

### Ejemplos

Las respuestas siguientes provienen de una ejecución real con los datos de `createDatabase.sql`; los identificadores y códigos varían. Se asume `TOKEN` obtenido como en el paso 8. Cada ejemplo indica el usuario que debe autenticarse.

**Registrar un paquete** (`cliente1@grupo5.cl`):

```bash
curl -s -X POST http://localhost:8080/api/paquete \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"centroDistId":1,"peso":2.5,"volumen":0.4,"direccionDestino":"Av. Providencia 1234, Santiago","fechaCompromiso":"2026-11-10T18:00:00"}'
```

```json
{"paqueteId":6,"usuarioClienteId":4,"centroDistId":1,"paqueteOrigenId":null,"codigo":"G5-6a43783816fa4e8dbc5cd6b1091cd166","estado":"Registrado","peso":2.5,"volumen":0.4,"direccionDestino":"Av. Providencia 1234, Santiago","intentos":0,"fechaCompromiso":"2026-11-10T18:00:00","despachadoAt":null,"entregadoAt":null,"fechaReprogramada":null}
```

Estado 201. Un despachador debe indicar además `usuarioClienteId`. Los campos `peso`, `volumen`, `direccionDestino`, `fechaCompromiso` y `centroDistId` son obligatorios.

**Asignar una ruta** (`despachador@grupo5.cl`). `fecha` es opcional (por defecto, ahora). La operación es atómica: si falla la capacidad del vehículo o un paquete no está asignable, no se crea nada.

```bash
curl -s -X POST http://localhost:8080/api/ruta/asignar \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"centroDistId":1,"repartidorId":1,"vehiculoId":1,"paqueteIds":[6]}'
```

```json
{"rutaId":6,"centroDistId":1,"repartidorId":1,"vehiculoId":1,"fecha":"2026-10-31T17:59:36.572791"}
```

Estado 201. Los paquetes pasan a `En reparto`.

**Cambiar la dirección de destino** (`cliente1@grupo5.cl`, dueño del paquete). Genera un registro en la auditoría:

```bash
curl -s -X PATCH http://localhost:8080/api/paquete/6/direccion \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"direccionDestino":"Av. Apoquindo 4500, Las Condes"}'
```

Devuelve el paquete actualizado (estado 200). Si el paquete está `Entregado` o `Devuelto a origen`, responde 409 `{"error":"No se puede cambiar el destino de un paquete en estado final"}`.

**Entrega fallida** (`repartidor1@grupo5.cl`, repartidor de la ruta). Incrementa los intentos y reprograma el paquete; al superar 3 intentos lo devuelve a origen y crea el paquete de retorno:

```bash
curl -s -X POST http://localhost:8080/api/paquete/6/entrega-fallida \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"rutaId":6,"motivo":"Cliente ausente"}'
```

```json
{"paqueteId":6,"usuarioClienteId":4,"centroDistId":1,"paqueteOrigenId":null,"codigo":"G5-6a43783816fa4e8dbc5cd6b1091cd166","estado":"Reprogramado","peso":2.50,"volumen":0.40,"direccionDestino":"Av. Apoquindo 4500, Las Condes","intentos":1,"fechaCompromiso":"2026-11-10T18:00:00","despachadoAt":"2026-10-31T17:59:36.574904","entregadoAt":null,"fechaReprogramada":"2026-11-01T17:59:44.6196"}
```

**Entrega exitosa** (`repartidor1@grupo5.cl`):

```bash
curl -s -X PATCH http://localhost:8080/api/paquete/7/entregado \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"rutaId":7}'
```

Devuelve el paquete con `"estado":"Entregado"` y `entregadoAt` completado.

**Seguimiento por código** (cualquier usuario autenticado con acceso al paquete):

```bash
curl -s http://localhost:8080/api/tracking/G5-6a43783816fa4e8dbc5cd6b1091cd166 -H "Authorization: Bearer $TOKEN"
```

```json
{"codigo":"G5-6a43783816fa4e8dbc5cd6b1091cd166","estado":"Reprogramado","direccionDestino":"Av. Apoquindo 4500, Las Condes","fechaCompromiso":"2026-11-10T18:00:00","entregadoAt":null,"intentos":1,"fechaReprogramada":"2026-11-01T17:59:44.6196","eventos":[{"estado":"Registrado","descripcion":"Paquete registrado","fecha":"2026-10-31T17:59:29.40388"},{"estado":"En reparto","descripcion":"Asignado a la ruta 6","fecha":"2026-10-31T17:59:36.585113"},{"estado":"Reprogramado","descripcion":"Intento fallido 1 (Cliente ausente); reprogramado","fecha":"2026-10-31T17:59:44.633192"}]}
```

**Refrescar la vista materializada** (`despachador@grupo5.cl`):

```bash
curl -s -X POST http://localhost:8080/api/reportes/refrescar -H "Authorization: Bearer $TOKEN"
```

```json
{"refrescadoEn":"2026-10-31T17:59:44"}
```

**Alertas de desempeño** (`despachador@grupo5.cl`). Lista los repartidores y centros cuyo porcentaje de entregas a tiempo (`pctATiempo`) está bajo el umbral (por defecto 85):

```bash
curl -s "http://localhost:8080/api/reportes/alertas?umbral=85" -H "Authorization: Bearer $TOKEN"
```

```json
{"umbral":85,"repartidores":[{"centroId":1,"centroNombre":"CD Santiago G5","repartidorId":2,"repartidorNombre":"repartidor2","entregasTotales":2,"entregasATiempo":1,"intentosTotales":2,"paquetesAtendidos":2,"pctATiempo":50.00,"intentosPromedio":1.00},{"centroId":1,"centroNombre":"CD Santiago G5","repartidorId":1,"repartidorNombre":"repartidor1","entregasTotales":3,"entregasATiempo":2,"intentosTotales":4,"paquetesAtendidos":4,"pctATiempo":66.67,"intentosPromedio":1.00}],"centros":[{"centroId":1,"centroNombre":"CD Santiago G5","entregasTotales":5,"entregasATiempo":3,"pctATiempo":60.00,"intentosPromedio":1.00}]}
```

### Errores

Todos los errores tienen la forma `{"error": "mensaje"}`.

| Estado | Causa | Ejemplo |
|---|---|---|
| 400 | Datos inválidos o faltantes | `{"error":"fechaCompromiso: no debe ser nulo; direccionDestino: no debe estar vacío; ..."}` |
| 401 | Token ausente, inválido, expirado, sin perfil local o con rol distinto al del perfil | `{"error":"No autenticado"}` |
| 403 | El rol no tiene permiso, o el recurso pertenece a otro usuario | `{"error":"No tiene permiso para esta operación"}` |
| 404 | Recurso inexistente | `{"error":"Paquete inexistente"}` |
| 409 | Conflicto con una regla de negocio (capacidad, estado, restricción de la base) | `{"error":"No se puede cambiar el destino de un paquete en estado final"}` |
| 500 | Error interno no clasificado | `{"error":"Error interno"}` |

## Objetos de base de datos

| Requisito | Objeto | Archivo |
|---|---|---|
| Procedimiento 1: asignar paquetes a una ruta de forma atómica | `asignar_ruta(centro, repartidor, vehiculo, fecha, paquetes[], INOUT ruta_id)` | `database/03-storedProced.sql` |
| Procedimiento 2: entrega fallida, reintentos y retorno a origen | `registrar_entrega_fallida(paquete, ruta, motivo)` | `database/03-storedProced.sql` |
| Apoyo: entrega exitosa y gestión de paquetes en rutas | `registrar_entrega_exitosa`, `agregar_paquete_ruta`, `quitar_paquete_ruta` | `database/03-storedProced.sql` |
| Trigger 1: capacidad de peso y volumen del vehículo | `check_route_capacity()` con trigger `route_capacity_before_insert` (sobre `ruta_paquete`) | `database/04-triggers.sql` |
| Trigger 2: auditoría de cambios de destino (y bloqueo en estados finales) | `audit_destination_change()` con trigger `destination_audit_before_update` (sobre `paquete`) | `database/04-triggers.sql` |
| Auditoría inmutable | `proteger_auditoria()` con trigger `auditoria_inmutable` (bloquea UPDATE y DELETE) | `database/04-triggers.sql` |
| Integridad adicional | Triggers `perfil_repartidor`, `perfil_usuario`, `proteger_ciclo`, `coherencia_ruta`, `capacidad_modificada`, `coherencia_asignacion` | `database/04-triggers.sql` |
| Vista materializada de rendimiento | `mv_rendimiento_entregas` (índice único `idx_mv_rendimiento_centro_repartidor`) y función `refrescar_mv_rendimiento()` (`REFRESH ... CONCURRENTLY`) | `database/02-materialViews.sql` |
| Refresco programado | `@Scheduled` diario a las 03:00 (`America/Santiago`) en `ReporteService`; se cambia con `REFRESH_CRON` | `backend/.../services/ReporteService.java` |
| Índices | `idx_paquete_codigo` (HASH), `idx_paquete_estado`, `idx_evento_seguimiento_paquete_fecha`, `uq_ruta_paquete_pendiente` | `database/01-dbCreate.sql` |

## Pruebas

Pruebas unitarias del backend (no requieren base de datos ni Keycloak):

```bash
cd backend
./mvnw test
```

Pruebas de reglas de negocio en SQL. Se ejecutan en transacciones que terminan en `ROLLBACK`, por lo que no dejan datos; deben imprimir tres mensajes `... aprobadas`:

```bash
PGPASSWORD=logistica psql -h localhost -p 5432 -U logistica -d logistica_g5 -f backend/tests/reglas.sql
```

Pruebas de integración HTTP (`backend/tests/verify_api.py`). Usan Python 3 sin dependencias, tokens reales de Keycloak y crean datos: úselas solo con una base recién instalada con `createDatabase.sql`, con Keycloak y el backend en ejecución:

```bash
cd backend/tests
python3 verify_api.py
# variables opcionales: API_URL (por defecto http://localhost:8080)
#                       KEYCLOAK_ISSUER (por defecto http://localhost:8081/realms/logistica-g5)
```

Resultado esperado: `Integración HTTP aprobada: 104 respuestas verificadas; JWT reales, RBAC, CRUD y procedimientos.`

Pruebas de concurrencia (`backend/tests/concurrencia.py`): una edición del paquete simultánea con su entrega, y dos asignaciones simultáneas a una ruta con capacidad para una sola. Requieren `psql` en el `PATH` (o `PSQL_BIN`), Keycloak y el backend en ejecución, y crean datos, por lo que deben usarse solo en una base de pruebas:

```bash
cd backend/tests
DB_NAME=logistica_g5 DB_PASSWORD=logistica python3 concurrencia.py
# variables opcionales: DB_HOST, DB_PORT, DB_USER, API_URL, KEYCLOAK_ISSUER
```

Resultado esperado: `Concurrencia aprobada: PUT conserva la entrega confirmada y la ruta no excede su capacidad.`
