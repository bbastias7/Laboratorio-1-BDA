"""Pruebas de integración. Usar SOLO con una BD aislada instalada con createDatabase.sql.
Variables API_URL y KEYCLOAK_ISSUER permiten elegir puertos de prueba.
No imprime tokens ni contraseñas. Requiere Python 3, sin dependencias.
"""
import base64
import json
import os
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta

BASE = os.getenv("API_URL", "http://localhost:8080")
ISSUER = os.getenv("KEYCLOAK_ISSUER", "http://localhost:8081/realms/logistica-g5")
checks = 0

def request(method, path, body=None, token=None, status=200):
    global checks
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=None if body is None else json.dumps(body).encode(),
                                 headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as res:
            code, data = res.status, res.read()
    except urllib.error.HTTPError as exc:
        code, data = exc.code, exc.read()
    assert code == status, (method, path, code, status, data.decode()[:500])
    checks += 1
    return json.loads(data) if data else None

def request_headers(method, path, body=None, token=None, status=200):
    """Like request() but returns (body, headers); used for X-Total-Count checks."""
    global checks
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=None if body is None else json.dumps(body).encode(),
                                 headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as res:
            code, data, hdrs = res.status, res.read(), res.headers
    except urllib.error.HTTPError as exc:
        code, data, hdrs = exc.code, exc.read(), exc.headers
    assert code == status, (method, path, code, status, data.decode()[:500])
    checks += 1
    return (json.loads(data) if data else None), hdrs

def login(name):
    body = urllib.parse.urlencode({"client_id": "logistica-cli", "grant_type": "password",
                                  "username": name + "@grupo5.cl", "password": "Grupo5Demo!"}).encode()
    req = urllib.request.Request(ISSUER + "/protocol/openid-connect/token", data=body)
    with urllib.request.urlopen(req, timeout=20) as res:
        return json.load(res)["access_token"]

def run():
    global checks
    admin, c1, c2, r1, r2 = [login(n) for n in
        ["despachador", "cliente1", "cliente2", "repartidor1", "repartidor2"]]
    request("GET", "/api/paquete", status=401)
    request("GET", "/api/paquete", token="invalid", status=401)
    parts = c1.split(".")
    claims = json.loads(base64.urlsafe_b64decode(parts[1] + "=="))
    claims["rol"], claims["uid"] = "despachador", 1
    parts[1] = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
    request("GET", "/api/usuarios", token=".".join(parts), status=401)
    request("GET", "/api/usuarios", token=c1, status=403)
    request("GET", "/api/reportes/alertas", token=r1, status=403)
    profile = request("GET", "/api/auth/me", token=admin)
    assert request("GET", "/api/auth/me", token=c1)["rol"] == "cliente"
    assert request("GET", "/api/auth/me", token=r1)["rol"] == "repartidor"
    request("GET", "/api/entregas", token=c1, status=403)
    request("GET", "/api/entregas", token=admin, status=403)
    own_routes = {x["rutaId"] for x in request("GET", "/api/ruta", token=r1)}
    own_deliveries = request("GET", "/api/entregas?soloPendientes=true", token=r1)
    assert all(x["rutaId"] in own_routes for x in own_deliveries)
    centers = request("GET", "/api/centrodistribucion", token=admin)
    center = next(c["centroDistId"] for c in centers if c["nombre"] == "CD Santiago G5")
    users = request("GET", "/api/usuarios", token=admin)
    uid1 = next(u["usuarioId"] for u in users if u["email"] == "cliente1@grupo5.cl")
    profile2 = next(u for u in users if u["email"] == "cliente2@grupo5.cl")
    profile2["rol"] = "despachador"
    request("PUT", f'/api/usuarios/{profile2["usuarioId"]}', profile2, admin)
    request("GET", "/api/auth/me", token=c2, status=401)
    profile2["rol"] = "cliente"
    request("PUT", f'/api/usuarios/{profile2["usuarioId"]}', profile2, admin)
    uidrep1 = next(u["usuarioId"] for u in users if u["email"] == "repartidor1@grupo5.cl")
    reps = request("GET", "/api/repartidor", token=admin)
    rep1 = next(r["repartidorId"] for r in reps if r["usuarioId"] == uidrep1)
    veh = next(v["vehiculoId"] for v in request("GET", "/api/vehiculos", token=admin) if v["patente"] == "G5-TEST")
    promise = (datetime.now() + timedelta(days=2)).isoformat(timespec="seconds")
    def package(weight=2):
        return request("POST", "/api/paquete", {"centroDistId": center, "peso": weight, "volumen": 0.5,
             "direccionDestino": "Destino prueba API", "fechaCompromiso": promise,
             "usuarioClienteId": profile["usuarioId"], "estado": "Entregado", "intentos": 99,
             "fechaReprogramada": "2100-01-01T00:00:00"}, c1, 201)
    def assign(pid, status=201):
        return request("POST", "/api/ruta/asignar", {"centroDistId": center, "repartidorId": rep1,
            "vehiculoId": veh, "paqueteIds": [pid]}, admin, status)
    p = package()
    assert p["usuarioClienteId"] == uid1 and p["estado"] == "Registrado" and p["intentos"] == 0
    assert p["fechaReprogramada"] is None
    pid = p["paqueteId"]
    invalid = dict(p, codigo="X" * 65)
    request("PUT", f"/api/paquete/{pid}", invalid, admin, 400)
    request("GET", "/api/paquete/" + str(pid), token=c2, status=403)
    request("GET", "/api/tracking/" + p["codigo"], token=c2, status=403)
    request("POST", "/api/ruta/asignar", {"centroDistId": center, "repartidorId": rep1,
            "vehiculoId": veh, "paqueteIds": [pid]}, c1, 403)
    request("POST", "/api/paquete", {"peso": -1}, c1, 400)
    for n in range(1, 5):
        route = assign(pid)
        rid = route["rutaId"]
        request("POST", f"/api/paquete/{pid}/entrega-fallida", {"rutaId": rid}, r2, 403)
        if n == 1:
            request("PATCH", f"/api/paquete/{pid}/direccion", {"direccionDestino": "Cambio PATCH"}, c1)
            full = request("GET", f"/api/paquete/{pid}", token=admin)
            full["direccionDestino"] = "Cambio PUT"
            request("PUT", f"/api/paquete/{pid}", full, admin)
            full["peso"] = 1000
            request("PUT", f"/api/paquete/{pid}", full, admin, 409)
        result = request("POST", f"/api/paquete/{pid}/entrega-fallida", {"rutaId": rid, "motivo": "Ausente"}, r1)
        assert result["intentos"] == n
        if n < 4:
            assert result["estado"] == "Reprogramado" and result["fechaReprogramada"]
        request("POST", f"/api/paquete/{pid}/entrega-fallida", {"rutaId": rid}, r1, 409)
    assert result["estado"] == "Devuelto a origen"
    returns = [x for x in request("GET", "/api/paquete", token=c1) if x["paqueteOrigenId"] == pid]
    assert len(returns) == 1
    assert all(x["usuarioClienteId"] == uid1 for x in request("GET", "/api/paquete", token=c1))
    assert all(x["paqueteId"] != pid for x in request("GET", "/api/paquete", token=c2))
    audits = [x for x in request("GET", "/api/auditoria", token=admin) if x["paqueteId"] == pid]
    assert len(audits) == 2 and all(x["usuarioId"] is not None for x in audits)
    timeline = request("GET", "/api/tracking/" + p["codigo"], token=c1)["eventos"]
    assert len(timeline) == 9 and timeline[-1]["estado"] == "Devuelto a origen"
    assert [x["fecha"] for x in timeline] == sorted(x["fecha"] for x in timeline)
    # Rollback de asignación por capacidad.
    big = package(101)
    before = len(request("GET", "/api/ruta", token=admin))
    assign(big["paqueteId"], 409)
    assert len(request("GET", "/api/ruta", token=admin)) == before
    assert request("GET", f'/api/paquete/{big["paqueteId"]}', token=c1)["estado"] == "Registrado"
    request("DELETE", f'/api/paquete/{big["paqueteId"]}', token=admin, status=204)
    request("GET", f'/api/paquete/{big["paqueteId"]}', token=admin, status=404)
    ok = package()
    rid = assign(ok["paqueteId"])["rutaId"]
    request("PATCH", f'/api/paquete/{ok["paqueteId"]}/entregado', {"rutaId": rid}, r1)
    request("PATCH", f'/api/paquete/{ok["paqueteId"]}/entregado', {"rutaId": rid}, r1, 409)
    # El destino de un paquete en estado final no se modifica.
    request("PATCH", f'/api/paquete/{ok["paqueteId"]}/direccion', {"direccionDestino": "Tras entrega"}, c1, 409)
    request("PATCH", f"/api/paquete/{pid}/direccion", {"direccionDestino": "Tras retorno"}, c1, 409)
    # CRUD de centros, vehículos y perfiles sin referencias históricas.
    tag = uuid.uuid4().hex[:8]
    c = request("POST", "/api/centrodistribucion", {"nombre": tag, "direccion": "A", "ciudad": "Santiago"}, admin, 201)
    cid = c["centroDistId"]
    c["nombre"] += " editado"
    request("PUT", f"/api/centrodistribucion/{cid}", c, admin)
    request("GET", f"/api/centrodistribucion/{cid}", token=admin)
    v = request("POST", "/api/vehiculos", {"patente": tag, "capacidadPeso": 10, "capacidadVolumen": 1, "centroDistId": cid}, admin, 201)
    v["capacidadPeso"] = 12
    request("PUT", f'/api/vehiculos/{v["vehiculoId"]}', v, admin)
    request("DELETE", f'/api/vehiculos/{v["vehiculoId"]}', token=admin, status=204)
    u = request("POST", "/api/usuarios", {"nombre": tag, "email": tag+"@example.invalid", "rol": "repartidor",
                                       "keycloakId": str(uuid.uuid4())}, admin, 201)
    u["nombre"] += " editado"
    request("PUT", f'/api/usuarios/{u["usuarioId"]}', u, admin)
    rep = request("POST", "/api/repartidor", {"usuarioId": u["usuarioId"], "centroDistId": cid}, admin, 201)
    request("PUT", f'/api/repartidor/{rep["repartidorId"]}', rep, admin)
    request("DELETE", f'/api/repartidor/{rep["repartidorId"]}', token=admin, status=204)
    request("DELETE", f'/api/usuarios/{u["usuarioId"]}', token=admin, status=204)
    request("DELETE", f"/api/centrodistribucion/{cid}", token=admin, status=204)
    # Asociación CRUD mantiene paquete y eventos sincronizados.
    p = package()
    route = request("POST", "/api/ruta", {"centroDistId": center, "repartidorId": rep1, "vehiculoId": veh}, admin, 201)
    rid, pid = route["rutaId"], p["paqueteId"]
    route["fecha"] = promise
    request("PUT", f"/api/ruta/{rid}", route, admin)
    assert request("GET", f"/api/ruta/{rid}", token=admin)["fecha"] == promise
    request("POST", "/api/rutapaquete", {"rutaId": rid, "paqueteId": pid}, admin, 201)
    assert request("GET", f"/api/rutapaquete/{rid}/{pid}", token=admin)["resultado"] == "Pendiente"
    request("DELETE", f"/api/rutapaquete/{rid}/{pid}", token=admin, status=204)
    request("GET", f"/api/rutapaquete/{rid}/{pid}", token=admin, status=404)
    assert request("GET", f"/api/paquete/{pid}", token=c1)["estado"] == "En centro"
    request("DELETE", f"/api/ruta/{rid}", token=admin, status=204)
    request("GET", f"/api/ruta/{rid}", token=admin, status=404)
    event = request("POST", "/api/eventoseguimiento", {"paqueteId": pid, "estado": "En centro", "descripcion": "Nota"}, admin, 201)
    event["descripcion"] = "Corrección"
    request("PUT", f'/api/eventoseguimiento/{event["eventoId"]}', event, admin)
    assert request("GET", f'/api/eventoseguimiento/{event["eventoId"]}', token=admin)["descripcion"] == "Corrección"
    request("DELETE", f'/api/eventoseguimiento/{event["eventoId"]}', token=admin, status=204)
    # La auditoría es solo lectura: no hay POST/PUT/DELETE y la fila no cambia.
    aid = audits[0]["auditoriaId"]
    request("POST", "/api/auditoria", {"paqueteId": pid, "direccionAnterior": "A", "direccionNueva": "B"}, admin, 405)
    request("PUT", f"/api/auditoria/{aid}", audits[0], admin, 405)
    request("DELETE", f"/api/auditoria/{aid}", token=admin, status=405)
    assert request("GET", f"/api/auditoria/{aid}", token=admin) == audits[0]
    # PUT de la asociación utiliza el procedimiento y no permite reabrir historial.
    rid = assign(pid)["rutaId"]
    request("PUT", f"/api/rutapaquete/{rid}/{pid}", {"resultado": "Entregado"}, admin)
    assert request("GET", f"/api/paquete/{pid}", token=c1)["estado"] == "Entregado"
    request("PUT", f"/api/rutapaquete/{rid}/{pid}", {"resultado": "Pendiente"}, admin, 400)
    request("DELETE", f"/api/rutapaquete/{rid}/{pid}", token=admin, status=409)
    request("POST", "/api/reportes/refrescar", {}, admin)
    rendimiento = request("GET", "/api/reportes/rendimiento", token=admin)
    alerts = request("GET", "/api/reportes/alertas?umbral=85", token=admin)
    expected = {x["repartidorId"] for x in rendimiento if x["entregasTotales"] and
                100 * x["entregasATiempo"] / x["entregasTotales"] < 85}
    assert {x["repartidorId"] for x in alerts["repartidores"]} == expected
    for ctr in {x["centroId"] for x in rendimiento}:
        rows = [x for x in rendimiento if x["centroId"] == ctr]
        total = sum(x["entregasTotales"] for x in rows)
        on_time = sum(x["entregasATiempo"] for x in rows)
        is_alert = total > 0 and 100 * on_time / total < 85
        assert (ctr in {x["centroId"] for x in alerts["centros"]}) == is_alert
    request("GET", "/api/reportes/alertas?umbral=101", token=admin, status=400)
    # Paginación y filtros: con page el cuerpo es la porción y X-Total-Count el total filtrado; sin page, la lista completa.
    for path in ["/api/paquete", "/api/usuarios", "/api/ruta", "/api/vehiculos", "/api/repartidor", "/api/rutapaquete",
                 "/api/eventoseguimiento", "/api/auditoria", "/api/centrodistribucion"]:
        full, hdrs = request_headers("GET", path, token=admin)
        assert hdrs.get("X-Total-Count") is None, path
        first, hdrs = request_headers("GET", path + "?page=0&size=1", token=admin)
        assert int(hdrs["X-Total-Count"]) == len(full) and len(first) == min(1, len(full)), path
        if len(full) > 1:
            second, _ = request_headers("GET", path + "?page=1&size=1", token=admin)
            assert second and second != first, path
        past, hdrs = request_headers("GET", path + "?page=9999&size=100", token=admin)
        assert past == [] and int(hdrs["X-Total-Count"]) == len(full), path
        clamped, _ = request_headers("GET", path + "?page=0&size=100000", token=admin)
        assert len(clamped) == min(100, len(full)), path
    only_clients, hdrs = request_headers("GET", "/api/usuarios?rol=cliente&page=0&size=50", token=admin)
    assert only_clients and all(x["rol"] == "cliente" for x in only_clients)
    assert int(hdrs["X-Total-Count"]) == len(only_clients)
    found, _ = request_headers("GET", "/api/usuarios?q=CLIENTE1&page=0", token=admin)
    assert [x["email"] for x in found] == ["cliente1@grupo5.cl"]
    wildcard, hdrs = request_headers("GET", "/api/usuarios?q=%25&page=0", token=admin)
    assert wildcard == [] and hdrs["X-Total-Count"] == "0"
    request("GET", "/api/usuarios?rol=hacker&page=0", token=admin, status=400)
    request("GET", "/api/paquete?estado=Inexistente&page=0", token=admin, status=400)
    mine, hdrs = request_headers("GET", "/api/paquete?page=0&size=100", token=c1)
    assert all(x["usuarioClienteId"] == uid1 for x in mine) and int(hdrs["X-Total-Count"]) == len(mine)
    delivered, hdrs = request_headers("GET", "/api/paquete?estado=Entregado&page=0&size=100", token=admin)
    assert delivered and all(x["estado"] == "Entregado" for x in delivered) and int(hdrs["X-Total-Count"]) == len(delivered)
    own_page, hdrs = request_headers("GET", "/api/ruta?page=0&size=100", token=r1)
    assert {x["rutaId"] for x in own_page} == {x["rutaId"] for x in request("GET", "/api/ruta", token=r1)}
    # Registro público de clientes: crea la cuenta en Keycloak, asigna el rol cliente y deja el perfil local.
    email = f"registro.{uuid.uuid4().hex[:8]}@example.invalid"
    new = request("POST", "/api/auth/registro", {"nombre": "Cuenta Registrada", "email": email,
                  "password": "claveSegura1", "rol": "despachador"}, status=201)
    assert new["rol"] == "cliente" and new["email"] == email and new["keycloakId"]
    body = urllib.parse.urlencode({"client_id": "logistica-cli", "grant_type": "password",
                                  "username": email, "password": "claveSegura1"}).encode()
    with urllib.request.urlopen(urllib.request.Request(ISSUER + "/protocol/openid-connect/token", data=body), timeout=20) as res:
        new_token = json.load(res)["access_token"]
    assert request("GET", "/api/auth/me", token=new_token)["rol"] == "cliente"
    request("GET", "/api/usuarios", token=new_token, status=403)
    request("POST", "/api/auth/registro", {"nombre": "Otra", "email": email, "password": "claveSegura1"}, status=409)
    request("POST", "/api/auth/registro", {"nombre": "Otra", "email": "despachador@grupo5.cl", "password": "claveSegura1"}, status=409)
    request("POST", "/api/auth/registro", {"nombre": " ", "email": "no-es-correo", "password": "corta"}, status=400)
    request("POST", "/api/auth/registro", {"nombre": "X", "email": "x@example.invalid", "password": "1234567"}, status=400)
    request("POST", "/api/auth/registro", {}, status=400)
    # Limpieza: borrar el perfil también elimina la cuenta de Keycloak (se comprueba más abajo con el cliente de servicio).
    request("DELETE", f'/api/usuarios/{new["usuarioId"]}', token=admin, status=204)
    cred = urllib.parse.urlencode({"grant_type": "client_credentials", "client_id": "logistica-admin",
                                  "client_secret": os.getenv("KEYCLOAK_ADMIN_CLIENT_SECRET", "logistica-admin-dev-secret")}).encode()
    with urllib.request.urlopen(urllib.request.Request(ISSUER + "/protocol/openid-connect/token", data=cred), timeout=20) as res:
        service_token = json.load(res)["access_token"]
    admin_base = ISSUER.replace("/realms/", "/admin/realms/")
    # Alta de personal desde la app: cuenta de Keycloak con contraseña temporal y rol, cambio de rol sincronizado y baja.
    def kc_get(path):
        req = urllib.request.Request(admin_base + path, headers={"Authorization": "Bearer " + service_token})
        try:
            with urllib.request.urlopen(req, timeout=20) as res:
                return res.status, json.load(res)
        except urllib.error.HTTPError as exc:
            return exc.code, None

    def kc_roles(kc_id):
        _, mappings = kc_get(f"/users/{kc_id}/role-mappings/realm")
        return {r["name"] for r in mappings} & {"cliente", "repartidor", "despachador"}

    staff_email = f"personal.{uuid.uuid4().hex[:8]}@example.invalid"
    staff = request("POST", "/api/usuarios", {"nombre": "Personal Prueba", "email": staff_email, "rol": "repartidor",
                    "password": "temporal123"}, admin, 201)
    try:
        assert staff["keycloakId"] and staff["email"] == staff_email and staff["rol"] == "repartidor"
        code, kc_user = kc_get(f'/users/{staff["keycloakId"]}')
        assert code == 200 and kc_user["enabled"] and kc_user["emailVerified"] and kc_user["username"] == staff_email
        assert "UPDATE_PASSWORD" in kc_user["requiredActions"]
        assert kc_roles(staff["keycloakId"]) == {"repartidor"}
        body = urllib.parse.urlencode({"client_id": "logistica-cli", "grant_type": "password",
                                      "username": staff_email, "password": "temporal123"}).encode()
        try:
            urllib.request.urlopen(urllib.request.Request(ISSUER + "/protocol/openid-connect/token", data=body), timeout=20).close()
            raise AssertionError("la contraseña temporal no debe permitir un token directo")
        except urllib.error.HTTPError as exc:
            assert exc.code == 400
            checks += 1
        both = {"nombre": "X", "email": "x@example.invalid", "rol": "cliente"}
        request("POST", "/api/usuarios", {**both, "password": "temporal123", "keycloakId": str(uuid.uuid4())}, admin, 400)
        request("POST", "/api/usuarios", both, admin, 400)
        request("POST", "/api/usuarios", {**both, "rol": "jefe", "password": "temporal123"}, admin, 400)
        request("POST", "/api/usuarios", {**both, "password": "corta"}, admin, 400)
        request("POST", "/api/usuarios", {**both, "email": staff_email, "password": "temporal123"}, admin, 409)
        request("POST", "/api/usuarios", {**both, "email": "cliente1@grupo5.cl", "keycloakId": str(uuid.uuid4())}, admin, 409)
        request("POST", "/api/usuarios", {**both, "password": "temporal123"}, c1, 403)
        # PUT sincroniza rol, nombre y correo con Keycloak.
        staff["rol"] = "despachador"
        staff["nombre"] = "Personal Renombrado Prueba"
        new_email = "n." + staff_email
        staff["email"] = new_email
        request("PUT", f'/api/usuarios/{staff["usuarioId"]}', staff, admin)
        assert kc_roles(staff["keycloakId"]) == {"despachador"}
        _, kc_user = kc_get(f'/users/{staff["keycloakId"]}')
        assert kc_user["email"] == new_email and kc_user["username"] == new_email
        assert kc_user["firstName"] == "Personal" and kc_user["lastName"] == "Renombrado Prueba"
        request("PUT", f'/api/usuarios/{staff["usuarioId"]}', {**staff, "keycloakId": str(uuid.uuid4())}, admin, 400)
        # Un despachador no puede eliminarse ni cambiar su propio rol.
        me = request("GET", "/api/auth/me", token=admin)
        request("DELETE", f'/api/usuarios/{me["usuarioId"]}', token=admin, status=409)
        request("PUT", f'/api/usuarios/{me["usuarioId"]}', {**me, "rol": "cliente"}, admin, 409)
    finally:
        request("DELETE", f'/api/usuarios/{staff["usuarioId"]}', token=admin, status=204)
    assert kc_get(f'/users/{staff["keycloakId"]}')[0] == 404
    assert kc_get(f'/users/{new["keycloakId"]}')[0] == 404
    checks += 2
    print(f"Integración HTTP aprobada: {checks} respuestas verificadas; JWT reales, RBAC, CRUD y procedimientos.")

if __name__ == "__main__":
    run()
