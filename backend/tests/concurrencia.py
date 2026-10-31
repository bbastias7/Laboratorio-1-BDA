"""Pruebas concurrentes con API y psql. Ejecutar SOLO en una BD de pruebas.
Requiere DB_NAME explícita; usa DB_HOST/PORT/USER/PASSWORD y PSQL_BIN opcional.
"""
import concurrent.futures
import json
import os
import shutil
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta

from verify_api import BASE, login, request


def run():
    db = os.environ.get("DB_NAME")
    if not db:
        raise RuntimeError("Defina DB_NAME con una base aislada que pueda recibir datos de prueba")
    psql = os.environ.get("PSQL_BIN") or shutil.which("psql")
    if not psql:
        raise RuntimeError("Agregue psql al PATH o defina PSQL_BIN")
    env = os.environ.copy()
    env.update(PGCLIENTENCODING="UTF8", PGAPPNAME="lab1-prueba-concurrencia",
               PGPASSWORD=os.environ.get("DB_PASSWORD", ""))
    args = [psql, "-X", "-qAt", "-v", "ON_ERROR_STOP=1", "-h", os.getenv("DB_HOST", "localhost"),
            "-p", os.getenv("DB_PORT", "5432"), "-U", os.getenv("DB_USER", "logistica"), "-d", db]

    def sql(query):
        return subprocess.run(args + ["-c", query], env=env, check=True, capture_output=True,
                              text=True, encoding="utf-8", timeout=15).stdout.strip()

    admin, cliente, courier = [login(n) for n in ["despachador", "cliente1", "repartidor1"]]
    ctr = next(c["centroDistId"] for c in request("GET", "/api/centrodistribucion", token=admin)
               if c["nombre"] == "CD Santiago G5")
    uid = request("GET", "/api/auth/me", token=courier)["usuarioId"]
    rep = next(r["repartidorId"] for r in request("GET", "/api/repartidor", token=admin) if r["usuarioId"] == uid)
    veh = next(v["vehiculoId"] for v in request("GET", "/api/vehiculos", token=admin) if v["patente"] == "G5-TEST")

    def package(weight=2):
        return request("POST", "/api/paquete", {"centroDistId": ctr, "peso": weight, "volumen": 0.5,
            "direccionDestino": "Prueba concurrente", "fechaCompromiso":
            (datetime.now() + timedelta(days=2)).isoformat(timespec="seconds")}, cliente, 201)

    p = package()
    pid = p["paqueteId"]
    route = request("POST", "/api/ruta/asignar", {"centroDistId": ctr, "repartidorId": rep,
                    "vehiculoId": veh, "paqueteIds": [pid]}, admin, 201)
    rid = route["rutaId"]
    payload = request("GET", f"/api/paquete/{pid}", token=admin)
    for field in ["estado", "intentos", "despachadoAt", "entregadoAt", "fechaReprogramada"]:
        payload.pop(field, None)
    payload["direccionDestino"] = "Editado durante entrega"

    # La entrega mantiene su cambio sin confirmar mientras comienza el PUT.
    delivery = subprocess.Popen(args + ["-c", f"BEGIN; CALL registrar_entrega_exitosa({pid},{rid}); "
                                         "SELECT pg_sleep(4); COMMIT;"], env=env,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        deadline = time.monotonic() + 10
        while sql("SELECT count(*) FROM pg_stat_activity WHERE application_name='lab1-prueba-concurrencia' "
                  "AND wait_event='PgSleep'") != "1":
            if delivery.poll() is not None or time.monotonic() > deadline:
                raise AssertionError("No se pudo sincronizar la entrega concurrente")
            time.sleep(0.05)
        result = request("PUT", f"/api/paquete/{pid}", payload, admin)
        _, errors = delivery.communicate(timeout=10)
        assert delivery.returncode == 0, errors.decode(errors="replace")
        assert result["estado"] == "Entregado" and result["entregadoAt"], result
        assignment = request("GET", f"/api/rutapaquete/{rid}/{pid}", token=admin)
        assert assignment["resultado"] == "Entregado"
    finally:
        if delivery.poll() is None:
            delivery.terminate()
            delivery.communicate(timeout=10)

    # Dos altas en una ruta con capacidad para uno solo: exactamente una confirma.
    vehicle = request("POST", "/api/vehiculos", {"patente": "C-"+os.urandom(4).hex(), "capacidadPeso": 10,
                      "capacidadVolumen": 2, "centroDistId": ctr}, admin, 201)
    route = request("POST", "/api/ruta", {"centroDistId": ctr, "repartidorId": rep,
                    "vehiculoId": vehicle["vehiculoId"]}, admin, 201)
    packages = [package(6), package(6)]
    rid = route["rutaId"]

    def assign(p):
        req = urllib.request.Request(BASE + "/api/rutapaquete", method="POST",
            data=json.dumps({"rutaId": rid, "paqueteId": p["paqueteId"]}).encode(),
            headers={"Authorization": "Bearer " + admin, "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                return response.status
        except urllib.error.HTTPError as error:
            return error.code

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        statuses = sorted(pool.map(assign, packages))
    assert statuses == [201, 409], statuses
    assert sql(f"SELECT sum(p.peso) FROM ruta_paquete rp JOIN paquete p ON p.id=rp.paquete_id "
               f"WHERE rp.ruta_id={rid}") == "6.00"
    print("Concurrencia aprobada: PUT conserva la entrega confirmada y la ruta no excede su capacidad.")


if __name__ == "__main__":
    run()
