param(
    [string]$Servidor = '127.0.0.1',
    [int]$Puerto = 5432,
    [string]$Usuario = 'postgres',
    [string]$Base = 'odapas_report',
    [string]$Psql = 'C:/Program Files/PostgreSQL/16/bin/psql.exe',
    [switch]$SinDatosPrueba
)
$ErrorActionPreference = 'Stop'
if ($Base -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'Use un nombre de base en minúsculas, números y guiones bajos.' }
if (-not (Test-Path -LiteralPath $Psql)) { throw 'No se encuentra psql. Indique su ruta con -Psql.' }
$taskPassword = Read-Host "Contraseña de PostgreSQL para $Usuario (no se mostrará ni guardará)" -AsSecureString
$taskBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskPassword)
$taskPreviousPassword = $env:PGPASSWORD
$taskPreviousEncoding = $env:PGCLIENTENCODING
try {
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskBstr)
    $env:PGCLIENTENCODING = 'UTF8'
    $taskConnection = @('-w','-h',$Servidor,'-p',"$Puerto",'-U',$Usuario,'-v','ON_ERROR_STOP=1')
    $taskExists = & $Psql @taskConnection -d postgres -At -c "SELECT 1 FROM pg_database WHERE datname='$Base'"
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible conectarse. Revise usuario, contraseña y puerto.' }
    if ($taskExists -eq '1') {
        $taskTables = & $Psql @taskConnection -d $Base -At -c "SELECT COUNT(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p')"
        if ($LASTEXITCODE -ne 0) { throw 'No fue posible inspeccionar la base.' }
        if ([int]$taskTables -gt 0) { throw "La base $Base ya contiene tablas. No se reemplazaron datos. Use otra base vacía." }
    } else {
        & $Psql @taskConnection -d postgres -c "CREATE DATABASE $Base ENCODING 'UTF8' TEMPLATE template0"
        if ($LASTEXITCODE -ne 0) { throw 'No fue posible crear la base.' }
    }
    $taskFile = if ($SinDatosPrueba) { 'odapas_report_estructura.sql' } else { 'odapas_report_completa.sql' }
    & $Psql @taskConnection -d $Base -f (Join-Path $PSScriptRoot $taskFile)
    if ($LASTEXITCODE -ne 0) { throw 'La instalación falló. La transacción revirtió los cambios del esquema.' }
    if (-not $SinDatosPrueba) {
        & $Psql @taskConnection -d $Base -f (Join-Path $PSScriptRoot '04_pruebas.sql')
        if ($LASTEXITCODE -ne 0) { throw 'La base se instaló, pero falló una prueba. Revise el mensaje anterior.' }
    }
    Write-Host "Base $Base instalada en $Servidor`:$Puerto. No se almacenó la contraseña en archivos."
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskBstr)
    $env:PGPASSWORD = $taskPreviousPassword
    $env:PGCLIENTENCODING = $taskPreviousEncoding
    $taskPassword.Dispose()
}
