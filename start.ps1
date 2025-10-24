# ========================================
# CryptoCollector - Script de Inicio (Windows)
# ========================================

Write-Host "========================================" -ForegroundColor Blue
Write-Host "  CryptoCollector - Inicio Automático  " -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# Función para mensajes
function Log-Info {
    param($message)
    Write-Host "[INFO] $message" -ForegroundColor Blue
}

function Log-Success {
    param($message)
    Write-Host "[✓] $message" -ForegroundColor Green
}

function Log-Warning {
    param($message)
    Write-Host "[⚠] $message" -ForegroundColor Yellow
}

function Log-Error {
    param($message)
    Write-Host "[✗] $message" -ForegroundColor Red
}

# 1. Verificar Docker
Log-Info "Verificando requisitos previos..."

if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    Log-Error "Docker no está instalado"
    exit 1
}
Log-Success "Docker instalado"

if (!(Get-Command docker-compose -ErrorAction SilentlyContinue)) {
    Log-Error "Docker Compose no está instalado"
    exit 1
}
Log-Success "Docker Compose instalado"

# 2. Verificar archivo .env
Log-Info "Verificando archivo .env..."
if (!(Test-Path .env)) {
    Log-Warning "Archivo .env no encontrado. Creando desde template..."
    
@"
# Config Server
CONFIG_SERVER_URI=http://config-server:8888

# JWT
JWT_SECRET=change_this_to_a_real_secret_minimum_256_bits
JWT_EXPIRATION=86400000

# Postgres (docker-compose)
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=cryptousers
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

# Spring Profile (dev/prod)
SPRING_PROFILES_ACTIVE=dev

# CoinGecko API (opcional)
COINGECKO_API_KEY=
"@ | Out-File -FilePath .env -Encoding UTF8
    
    Log-Success "Archivo .env creado"
} else {
    Log-Success "Archivo .env encontrado"
}

# 3. Compilar proyecto
Log-Info "Compilando el proyecto..."
mvn clean package -DskipTests
if ($LASTEXITCODE -eq 0) {
    Log-Success "Proyecto compilado"
} else {
    Log-Error "Error al compilar"
    exit 1
}

# 4. Detener servicios existentes
Log-Info "Deteniendo servicios existentes..."
docker-compose down 2>$null
Log-Success "Servicios detenidos"

# 5. Construir imágenes
Log-Info "Construyendo imágenes Docker..."
docker-compose build --no-cache
if ($LASTEXITCODE -eq 0) {
    Log-Success "Imágenes construidas"
} else {
    Log-Error "Error al construir imágenes"
    exit 1
}

# 6. Iniciar servicios
Log-Info "Iniciando servicios..."
docker-compose up -d

# 7. Esperar servicios
Log-Info "Esperando a que los servicios inicien (60 segundos)..."
Start-Sleep -Seconds 60

# 8. Verificar servicios
Write-Host ""
Log-Info "Verificando servicios..."
docker-compose ps

# 9. Información de acceso
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Información de Acceso" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "  API Gateway:       http://localhost:8080"
Write-Host "  Eureka Dashboard:  http://localhost:8761"
Write-Host "  Swagger Auth:      http://localhost:8081/swagger-ui/index.html"
Write-Host "  Swagger Crypto:    http://localhost:8092/swagger-ui/index.html"
Write-Host "  PostgreSQL:        localhost:5432"
Write-Host "  Redis:             localhost:6379"
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Log-Info "Comandos útiles:"
Write-Host "  Ver logs:          docker-compose logs -f" -ForegroundColor Yellow
Write-Host "  Detener servicios: docker-compose down" -ForegroundColor Yellow
Write-Host "  Estado servicios:  docker-compose ps" -ForegroundColor Yellow
Write-Host ""

Log-Success "¡Aplicación iniciada exitosamente! 🚀"
