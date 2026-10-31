param(
 [string]$KeycloakHome = (Join-Path $PSScriptRoot '../../Herramientas/keycloak-26.6.1'),
 [int]$Port = 8081
)
$ErrorActionPreference = 'Stop'
$runtime = Join-Path $PSScriptRoot 'runtime'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
$env:KC_DB = 'dev-file'
$env:KC_DB_URL = 'jdbc:h2:file:' + (Join-Path $runtime 'logistica').Replace('\','/') + ';NON_KEYWORDS=VALUE'
$env:KC_HOSTNAME = "http://localhost:$Port"
# Se usa un almacén H2 distinto del ramo anterior.
# Import explícito del nuevo realm; no sobrescribe un realm ya existente.
& (Join-Path $KeycloakHome 'bin/kc.bat') import --file (Join-Path $PSScriptRoot 'logistica-g5-realm.json') --override=false
if ($LASTEXITCODE -ne 0) { throw 'No se pudo importar el realm. Detenga la instancia que utiliza este almacén.' }
& (Join-Path $KeycloakHome 'bin/kc.bat') start-dev --http-host=127.0.0.1 --http-port=$Port
exit $LASTEXITCODE

