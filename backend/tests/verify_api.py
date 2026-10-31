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

def login(name):
    body = urllib.parse.urlencode({"client_id": "logistica-cli", "grant_type": "password",
                                  "username": name + "@grupo5.cl", "password": "Grupo5Demo!"}).encode()
    req = urllib.request.Request(ISSUER + "/protocol/openid-connect/token", data=body)
    with urllib.request.urlopen(req, timeout=20) as res:
        return json.load(res)["access_token"]

def run():
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
    a = request("POST", "/api/auditoria", {"paqueteId": pid, "direccionAnterior": "A", "direccionNueva": "B"}, admin, 201)
    a["direccionNueva"] = "C"
    request("PUT", f'/api/auditoria/{a["auditoriaId"]}', a, admin)
    assert request("GET", f'/api/auditoria/{a["auditoriaId"]}', token=admin)["direccionNueva"] == "C"
    request("DELETE", f'/api/auditoria/{a["auditoriaId"]}', token=admin, status=204)
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
    print(f"Integración HTTP aprobada: {checks} respuestas verificadas; JWT reales, RBAC, CRUD y procedimientos.")

if __name__ == "__main__":
    run()
