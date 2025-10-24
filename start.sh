#!/bin/bash

# ========================================
# CryptoCollector - Script de Inicio
# ========================================
# Este script facilita el inicio de toda la aplicación
# con Docker Compose y valida que todo funcione correctamente

set -e  # Salir si algún comando falla

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Banner
echo -e "${BLUE}========================================"
echo "  CryptoCollector - Inicio Automático  "
echo -e "========================================${NC}"
echo ""

# Función para mostrar mensajes
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# 1. Verificar requisitos previos
log_info "Verificando requisitos previos..."

if ! command -v docker &> /dev/null; then
    log_error "Docker no está instalado. Por favor, instálalo primero."
    exit 1
fi
log_success "Docker instalado: $(docker --version)"

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    log_error "Docker Compose no está instalado. Por favor, instálalo primero."
    exit 1
fi
log_success "Docker Compose instalado"

# 2. Verificar archivo .env
log_info "Verificando archivo .env..."
if [ ! -f .env ]; then
    log_error "Archivo .env no encontrado."
    log_info "Creando archivo .env desde template..."
    
    cat > .env << EOF
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
EOF
    log_success "Archivo .env creado. Por favor, revisa y edita las configuraciones."
else
    log_success "Archivo .env encontrado"
fi

# 3. Compilar el proyecto
log_info "Compilando el proyecto con Maven..."
if mvn clean package -DskipTests; then
    log_success "Proyecto compilado exitosamente"
else
    log_error "Error al compilar el proyecto"
    exit 1
fi

# 4. Detener servicios existentes
log_info "Deteniendo servicios existentes (si existen)..."
docker-compose down 2>/dev/null || true
log_success "Servicios detenidos"

# 5. Construir imágenes Docker
log_info "Construyendo imágenes Docker..."
if docker-compose build; then
    log_success "Imágenes construidas exitosamente"
else
    log_error "Error al construir las imágenes"
    log_info "Intenta ejecutar: docker-compose build --no-cache"
    exit 1
fi

# 6. Iniciar servicios
log_info "Iniciando servicios con Docker Compose..."
docker-compose up -d

# 7. Esperar a que los servicios estén saludables
log_info "Esperando a que los servicios inicien..."
echo ""

sleep 10

# Función para verificar salud de un servicio
check_health() {
    local service=$1
    local max_attempts=30
    local attempt=1
    
    log_info "Verificando $service..."
    while [ $attempt -le $max_attempts ]; do
        status=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null || echo "starting")
        
        if [ "$status" = "healthy" ]; then
            log_success "$service está saludable"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_warning "$service tardó más de lo esperado en iniciar"
    return 1
}

# Verificar servicios en orden
echo ""
log_info "Verificando estado de los servicios..."
echo ""

check_health "crypto-postgres"
check_health "crypto-redis"
check_health "crypto-config-server"
check_health "crypto-discovery-server"
check_health "crypto-api-gateway"
check_health "crypto-auth-service"
check_health "crypto-collector-micro"
check_health "crypto-frontend"

echo ""
log_success "Todos los servicios están ejecutándose"

# 8. Mostrar información de acceso
echo ""
log_info "Información de acceso:"
echo -e "${GREEN}========================================"
echo "  Frontend:          http://localhost:4201"
echo "  API Gateway:       http://localhost:8080"
echo "  Eureka Dashboard:  http://localhost:8761"
echo "  Config Server:     http://localhost:8888"
echo "  Swagger Auth:      http://localhost:8081/swagger-ui/index.html"
echo "  Swagger Crypto:    http://localhost:8092/swagger-ui/index.html"
echo "  PostgreSQL:        localhost:5432"
echo "  Redis:             localhost:6379"
echo -e "========================================${NC}"
echo ""

# 9. Prueba rápida de endpoints
log_info "Realizando prueba rápida de endpoints..."
echo ""

# Esperar un poco más para que los servicios estén completamente listos
sleep 10

# Probar Frontend
if curl -s -f http://localhost:4201 > /dev/null 2>&1; then
    log_success "Frontend respondiendo correctamente"
else
    log_warning "Frontend no está respondiendo aún"
fi

# Probar API Gateway
if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    log_success "API Gateway respondiendo correctamente"
else
    log_warning "API Gateway no está respondiendo aún"
fi

# Probar Eureka
if curl -s -f http://localhost:8761 > /dev/null 2>&1; then
    log_success "Eureka Dashboard accesible"
else
    log_warning "Eureka Dashboard no está accesible aún"
fi

# Probar Config Server
if curl -s -f http://localhost:8888/actuator/health > /dev/null 2>&1; then
    log_success "Config Server respondiendo correctamente"
else
    log_warning "Config Server no está respondiendo aún"
fi

echo ""
log_info "Para ver los logs en tiempo real, ejecuta:"
echo -e "${YELLOW}  docker-compose logs -f${NC}"
echo ""
log_info "Para detener todos los servicios, ejecuta:"
echo -e "${YELLOW}  docker-compose down${NC}"
echo ""
log_info "Para ver el estado de los servicios, ejecuta:"
echo -e "${YELLOW}  docker-compose ps${NC}"
echo ""

log_success "¡Aplicación iniciada exitosamente! 🚀"
echo ""
