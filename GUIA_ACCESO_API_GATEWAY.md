# 🌐 Guía de Acceso a Endpoints - API Gateway

## ⭐ IMPORTANTE: Usar API Gateway (Puerto 8080)

### ✅ Acceso Recomendado - API Gateway
**USAR**: `http://localhost:8080/api/...`  
El API Gateway es el punto de entrada único para toda la aplicación. Proporciona:
- ✅ Autenticación centralizada
- ✅ Load balancing automático
- ✅ CORS configurado
- ✅ Enrutamiento inteligente

### 🔧 Acceso Directo (Solo para Desarrollo)
Si necesitas debuggear o probar directamente un microservicio:
- Auth Service: `http://localhost:8081`
- Crypto Collector: `http://localhost:8092`

---

## 📊 Tabla de Servicios

| Servicio | Puerto | URL Base | Descripción |
|----------|--------|----------|-------------|
| **🌟 API Gateway** | **8080** | `http://localhost:8080` | **Punto de entrada principal** |
| Auth Service | 8081 | `http://localhost:8081` | Registro y Login (directo) |
| Crypto Collector | 8092 | `http://localhost:8092` | Criptomonedas (directo) |
| Config Server | 8888 | `http://localhost:8888` | Configuración centralizada |
| Discovery Server | 8761 | `http://localhost:8761` | Eureka Registry |
| PostgreSQL | 5432 | `localhost:5432` | Base de datos |
| Redis | 6379 | `localhost:6379` | Cache |

---

## 🔐 1. Autenticación (Endpoints Públicos)

### 1.1 Registrar Usuario

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan@example.com",
    "password": "Password123"
  }'
```

**Respuesta exitosa**:
```json
{
  "id": 1,
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com"
}
```

### 1.2 Login (Obtener Token JWT)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Password123"
  }'
```

**Respuesta exitosa**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqdWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzM3...",
  "email": "juan@example.com",
  "firstName": "Juan",
  "lastName": "Pérez"
}
```

**📝 Nota**: Guarda el token, lo necesitarás para todos los endpoints protegidos.

---

## 📊 2. Endpoints de Criptomonedas (Protegidos)

> 🔒 Estos endpoints requieren el header: `Authorization: Bearer TOKEN`

### 2.1 Obtener Estadísticas

```bash
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  http://localhost:8080/api/crypto/stats
```

**Respuesta**:
```json
{
  "total": 1016,
  "lastUpdated": "2025-10-23T00:00:00.524Z",
  "hasSyncedData": true
}
```

### 2.2 Buscar Criptomoneda por ID

```bash
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  http://localhost:8080/api/crypto/bitcoin
```

**Respuesta**:
```json
{
  "id": "bitcoin",
  "symbol": "btc",
  "name": "Bitcoin",
  "currentPrice": 67234.50,
  "marketCap": 1327234567890,
  "priceChangePercentage24h": 2.5,
  "marketCapRank": 1,
  "circulatingSupply": 19500000
}
```

**Otras criptomonedas disponibles**:
```bash
# Ethereum
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/crypto/ethereum

# Tether
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/crypto/tether

# BNB
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/crypto/binancecoin
```

### 2.3 Listar Criptomonedas (Paginado)

```bash
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  "http://localhost:8080/api/crypto/list?page=0&size=10"
```

**Parámetros**:
- `page`: Número de página (inicia en 0)
- `size`: Cantidad de elementos por página

**Respuesta**:
```json
{
  "content": [
    {
      "id": "bitcoin",
      "symbol": "btc",
      "name": "Bitcoin",
      "currentPrice": 67234.50
    },
    ...
  ],
  "totalElements": 1016,
  "totalPages": 102,
  "size": 10,
  "number": 0
}
```

### 2.4 Estado del Scheduler

```bash
curl -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  http://localhost:8080/api/crypto/scheduler-status
```

**Respuesta**:
```json
{
  "schedulerEnabled": true,
  "lastExecution": "2025-10-23T00:00:00.524Z",
  "nextExecution": "2025-10-23T01:00:00.000Z",
  "totalRecords": 1016
}
```

---

## 🗄️ 3. Gestión de Redis Cache

### 3.1 Ver Información de Redis (Público)

```bash
curl http://localhost:8080/api/public/cache/redis-info
```

**Respuesta**:
```json
{
  "redisConfigured": true,
  "totalCaches": 5,
  "cacheNames": [
    "crypto-details",
    "crypto-stats",
    "scheduler-status",
    "coingecko-api",
    "crypto-list"
  ],
  "status": "Redis is working! ✅"
}
```

> 🔓 Este endpoint es público y no requiere autenticación.

### 3.2 Limpiar Todas las Cachés (Protegido)

```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  http://localhost:8080/api/cache/clear-all
```

### 3.3 Limpiar Caché Específica (Protegido)

```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  http://localhost:8080/api/cache/clear/crypto-details
```

**Cachés disponibles**:
- `crypto-details` (TTL: 2 minutos)
- `crypto-stats` (TTL: 1 minuto)
- `scheduler-status` (TTL: 1 minuto)
- `coingecko-api` (TTL: 30 segundos)
- `crypto-list` (Deshabilitada por problemas de serialización)

---

## 📚 4. Documentación Swagger

### 4.1 Swagger UI - Crypto Collector
```
http://localhost:8080/crypto/swagger-ui/index.html
```

### 4.2 Swagger UI - Auth Service
```
http://localhost:8080/auth/swagger-ui/index.html
```

### 4.3 OpenAPI Docs (JSON)
```
http://localhost:8080/crypto/v3/api-docs
http://localhost:8080/auth/v3/api-docs
```

---

## 🔄 5. Flujo Completo de Ejemplo

### Paso 1: Registrar usuario
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@test.com","password":"Test1234"}'
```

### Paso 2: Obtener token
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

echo "Token obtenido: $TOKEN"
```

### Paso 3: Consultar stats
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats
```

### Paso 4: Consultar Bitcoin
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin
```

### Paso 5: Listar criptomonedas
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=0&size=5"
```

---

## 🧪 6. Script de Prueba Automático

Ejecuta el script de prueba completo:

```bash
bash test-api-gateway.sh
```

Este script:
1. ✅ Registra un usuario de prueba
2. ✅ Hace login y obtiene token
3. ✅ Prueba todos los endpoints protegidos
4. ✅ Verifica endpoints públicos
5. ✅ Valida acceso a Swagger

---

## ❓ 7. Troubleshooting

### Problema: Error 401 Unauthorized

**Causa**: Token JWT inválido, expirado o no proporcionado.

**Solución**:
```bash
# Obtener nuevo token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

# Usar el token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats
```

### Problema: Error 404 Not Found

**Causa**: Ruta incorrecta o servicio no registrado en Eureka.

**Solución**:
1. Verifica que todos los servicios estén registrados en Eureka:
   ```
   http://localhost:8761
   ```

2. Revisa los logs del API Gateway:
   ```bash
   docker logs crypto-api-gateway-1 --tail 50
   ```

3. Verifica que el servicio esté en funcionamiento:
   ```bash
   docker ps
   ```

### Problema: "Connection refused" en puerto 8080

**Causa**: API Gateway no está iniciado o falló al iniciar.

**Solución**:
```bash
# Ver estado del contenedor
docker ps -a | grep api-gateway

# Ver logs
docker logs crypto-api-gateway-1

# Reiniciar servicio
docker compose restart api-gateway
```

### Problema: Gateway enruta pero el servicio devuelve error

**Causa**: El microservicio destino tiene problemas.

**Solución**:
```bash
# Probar acceso directo al microservicio
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8092/api/crypto/stats

# Si funciona directo pero no por gateway, revisar logs
docker logs crypto-api-gateway-1
docker logs crypto-config-server-1
```

### Problema: CORS Error en navegador

**Causa**: Configuración CORS incompleta (ya está configurado).

**Verificación**: El API Gateway tiene CORS habilitado para:
- Todos los orígenes: `*`
- Todos los métodos: `GET, POST, PUT, DELETE, OPTIONS`
- Todos los headers: `*`

---

## 🏗️ 8. Arquitectura del API Gateway

```
┌─────────────────────────────────────────────┐
│         Cliente (Browser/Postman)           │
└────────────────┬────────────────────────────┘
                 │ http://localhost:8080
                 ▼
┌─────────────────────────────────────────────┐
│           API Gateway (Puerto 8080)         │
│  ┌──────────────────────────────────────┐   │
│  │   JWT Authentication Filter          │   │
│  │   - Valida tokens                    │   │
│  │   - Permite rutas públicas           │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │   Route Matcher                      │   │
│  │   - /api/auth/* → auth-service       │   │
│  │   - /api/crypto/** → crypto-service  │   │
│  │   - /api/public/** → crypto-service  │   │
│  └──────────────────────────────────────┘   │
│  ┌──────────────────────────────────────┐   │
│  │   Load Balancer (Eureka)             │   │
│  │   - lb://auth-microservices          │   │
│  │   - lb://crypto-collector-micro      │   │
│  └──────────────────────────────────────┘   │
└──────────┬───────────────────┬───────────────┘
           │                   │
           ▼                   ▼
┌──────────────────┐  ┌──────────────────────┐
│  Auth Service    │  │  Crypto Collector    │
│  (Puerto 8081)   │  │  (Puerto 8092)       │
│                  │  │                      │
│  - Register      │  │  - Stats             │
│  - Login         │  │  - Crypto Details    │
└──────────────────┘  │  - List              │
                      │  - Cache Management  │
                      └──────────────────────┘
```

---

## 📋 9. Endpoints Públicos vs Protegidos

### 🔓 Endpoints Públicos (Sin autenticación)
- `POST /api/auth/register` - Registro de usuarios
- `POST /api/auth/login` - Login
- `GET /api/public/cache/redis-info` - Info de Redis
- `GET /swagger-ui/**` - Documentación Swagger
- `GET /actuator/health` - Health check

### 🔒 Endpoints Protegidos (Requieren JWT)
- `GET /api/crypto/stats` - Estadísticas
- `GET /api/crypto/{coinId}` - Detalles de criptomoneda
- `GET /api/crypto/list` - Listar criptomonedas
- `GET /api/crypto/scheduler-status` - Estado del scheduler
- `DELETE /api/cache/clear-all` - Limpiar todas las cachés
- `DELETE /api/cache/clear/{cacheName}` - Limpiar caché específica

---

## 🎯 10. Configuración de Rutas del Gateway

Las rutas están configuradas en: `configServer/src/main/resources/config/apiGateway-dev.yml`

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth - Register (público)
        - id: auth-register
          uri: lb://auth-microservices
          predicates:
            - Path=/api/auth/register
        
        # Auth - Login (público)
        - id: auth-login
          uri: lb://auth-microservices
          predicates:
            - Path=/api/auth/login
        
        # Crypto - Endpoints protegidos
        - id: crypto-protected
          uri: lb://crypto-collector-micro
          predicates:
            - Path=/api/crypto/**
        
        # Crypto - Endpoints públicos
        - id: crypto-public
          uri: lb://crypto-collector-micro
          predicates:
            - Path=/api/public/**
        
        # Cache management (protegido)
        - id: crypto-cache
          uri: lb://crypto-collector-micro
          predicates:
            - Path=/api/cache/**
```

---

## ✅ 11. Verificación del Sistema

### 11.1 Verificar servicios en Eureka
```
http://localhost:8761
```
Deberías ver:
- ✅ AUTH-MICROSERVICES
- ✅ CRYPTO-COLLECTOR-MICRO

### 11.2 Verificar health del API Gateway
```bash
curl http://localhost:8080/actuator/health
```

### 11.3 Verificar Config Server
```bash
curl http://localhost:8888/actuator/health
```

### 11.4 Verificar Redis
```bash
docker exec crypto-redis-1 redis-cli PING
```
Debería responder: `PONG`

---

## 🚀 12. Mejoras Futuras

- [ ] Rate Limiting por usuario
- [ ] Circuit Breaker para fallos de microservicios
- [ ] Request/Response logging
- [ ] API Versioning (v1, v2)
- [ ] Gateway-level caching
- [ ] Retry logic automático
- [ ] Metrics con Micrometer/Prometheus
- [ ] Distributed tracing con Sleuth/Zipkin

---

## 📞 13. Contacto y Soporte

- **Config Server**: http://localhost:8888
- **Discovery Server**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Swagger Crypto**: http://localhost:8080/crypto/swagger-ui/index.html
- **Swagger Auth**: http://localhost:8080/auth/swagger-ui/index.html

---

**🎉 ¡API Gateway configurado y funcionando correctamente!**
