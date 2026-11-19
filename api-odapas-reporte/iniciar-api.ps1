param(
    [string]$Url = 'jdbc:postgresql://127.0.0.1:5432/odapas_report',
    [string]$Usuario = 'postgres',
    [int]$PuertoApi = 8080
)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $taskJdkRoots = @(Get-ChildItem 'C:/Program Files/Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending)
    $taskJdk = $taskJdkRoots | Where-Object { Test-Path (Join-Path $_.FullName 'bin/java.exe') } | Select-Object -First 1
    if (-not $taskJdk) { throw 'Se requiere JDK 17 o superior. Configure JAVA_HOME o agregue Java al PATH.' }
    $env:JAVA_HOME = $taskJdk.FullName
}
$taskOriginalUrl = $env:DB_URL
$taskOriginalUser = $env:DB_USER
$taskOriginalPassword = $env:DB_PASSWORD
$taskOriginalPort = $env:SERVER_PORT
$taskPassword = $null
$taskBstr = [IntPtr]::Zero
try {
    $env:DB_URL = $Url
    $env:DB_USER = $Usuario
    $env:SERVER_PORT = "$PuertoApi"
    if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
        $taskPassword = Read-Host "Contrasena de PostgreSQL para $Usuario (no se guardara en archivos)" -AsSecureString
        $taskBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskPassword)
        $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskBstr)
    }
    Write-Host "Iniciando ODAPAS Report en http://localhost:$PuertoApi"
    Push-Location $PSScriptRoot
    try {
        & (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'La API no pudo iniciarse. Revise la conexion y el mensaje anterior.' }
    } finally { Pop-Location }
} finally {
    if ($taskBstr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskBstr) }
    if ($null -ne $taskPassword) { $taskPassword.Dispose() }
    $env:DB_URL = $taskOriginalUrl
    $env:DB_USER = $taskOriginalUser
    $env:DB_PASSWORD = $taskOriginalPassword
    $env:SERVER_PORT = $taskOriginalPort
}
