# 📌 Guía de Acceso a Endpoints - CryptoCollector

## ⚠️ IMPORTANTE: Puertos y URLs Correctos

### 🔴 Error Común
**NO usar**: `http://localhost:8080/api/crypto/...`  
El puerto 8080 es del API Gateway que actualmente no está configurado para enrutar.

### ✅ URLs Correctas

| Servicio | Puerto | URL Base | Descripción |
|----------|--------|----------|-------------|
| **Auth Service** | 8081 | `http://localhost:8081` | Registro y Login |
| **Crypto Collector** | 8092 | `http://localhost:8092` | Microservicio de criptomonedas |
| **Config Server** | 8888 | `http://localhost:8888` | Configuración centralizada |
| **Discovery Server** | 8761 | `http://localhost:8761` | Eureka Registry |
| **PostgreSQL** | 5432 | `localhost:5432` | Base de datos |
| **Redis** | 6379 | `localhost:6379` | Cache |

---

## 🔐 Autenticación

### 1. Registrar Usuario

```bash
curl -X POST http://localhost:8081/api/auth/register \
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

### 2. Login (Obtener Token JWT)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Password123"
  }'
```

**Respuesta exitosa**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqdWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNjk..."
}
```

⚠️ **Guarda el token** para usarlo en las siguientes peticiones.

---

## 📊 Endpoints del Crypto Collector (Puerto 8092)

### 🔓 Endpoints PÚBLICOS (Sin Autenticación)

#### 1. Información de Redis Cache

```bash
curl http://localhost:8092/api/public/cache/redis-info
```

**Respuesta**:
```json
{
  "cacheNames": ["crypto-list", "crypto-stats", "crypto-details", "coingecko-api", "scheduler-status"],
  "redisConfigured": true,
  "totalCaches": 5,
  "status": "Redis is working! ✅"
}
```

#### 2. Demo sin Caché

```bash
curl http://localhost:8092/api/public/cache/demo
```

---

### 🔒 Endpoints PROTEGIDOS (Requieren JWT)

**Formato del header de autorización**:
```
Authorization: Bearer <tu_token_jwt>
```

#### 1. Estadísticas Generales (CACHEADO - 1 min TTL)

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/crypto/stats
```

**Respuesta**:
```json
{
  "lastUpdated": "2025-10-23T00:00:00.524Z",
  "total": 1016,
  "hasSyncedData": true
}
```

#### 2. Detalles de una Criptomoneda (CACHEADO - 2 min TTL)

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/crypto/bitcoin
```

**Respuesta**:
```json
{
  "id": 1,
  "coinId": "bitcoin",
  "name": "Bitcoin",
  "symbol": "btc",
  "marketCapRank": 1,
  "currentPrice": 67890.50,
  "marketCap": 1325000000000,
  "totalVolume": 32500000000,
  "lastUpdated": "2025-10-23T00:00:00.524Z"
}
```

**Otras criptomonedas disponibles**:
- `/api/crypto/ethereum`
- `/api/crypto/tether`
- `/api/crypto/binancecoin`
- `/api/crypto/cardano`
- etc.

#### 3. Lista Paginada de Criptomonedas (Sin caché)

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8092/api/crypto/list?page=0&size=10&sort=marketCapRank,asc"
```

**Parámetros**:
- `page`: Número de página (0-indexed)
- `size`: Elementos por página
- `sort`: Ordenamiento (ej: `marketCapRank,asc` o `name,desc`)

**Respuesta**:
```json
{
  "content": [
    {
      "id": 1,
      "coinId": "bitcoin",
      "name": "Bitcoin",
      ...
    },
    ...
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    ...
  },
  "totalElements": 1016,
  "totalPages": 102,
  "last": false,
  "first": true
}
```

#### 4. Búsqueda de Criptomonedas (Sin caché)

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  "http://localhost:8092/api/crypto/list?query=bitcoin&page=0&size=5"
```

Busca por nombre o símbolo (case-insensitive).

#### 5. Estado del Scheduler (CACHEADO - 1 min TTL)

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/crypto/scheduler/status
```

**Respuesta**:
```json
{
  "enabled": true,
  "frequency": "Every 6 hours",
  "schedule": "00:00, 06:00, 12:00, 18:00 UTC",
  "cronExpression": "0 0 */6 * * *",
  "lastSync": "2025-10-23T00:00:00.524Z",
  "totalCryptos": 1016,
  "nextSync": "2025-10-23T06:00:00Z",
  "nextSyncDescription": "06:00:00 UTC (today)",
  "minutesUntilNext": 123
}
```

#### 6. Sincronizar Manualmente con CoinGecko

```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/crypto/sync
```

⚠️ **Limpia todos los cachés** automáticamente después de sincronizar.

---

## 🗂️ Gestión de Caché (Puerto 8092)

### 1. Información de Cachés

```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/cache/info
```

### 2. Limpiar TODOS los Cachés

```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/cache/clear-all
```

### 3. Limpiar un Caché Específico

```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/cache/clear/crypto-stats
```

**Cachés disponibles**:
- `crypto-stats` (1 min TTL)
- `crypto-details` (2 min TTL)
- `scheduler-status` (1 min TTL)
- `coingecko-api` (30 seg TTL)

### 4. Pre-cargar Caché (Warmup)

```bash
curl -X POST -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/cache/warmup
```

### 5. Limpiar Caché de Listas

```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/cache/clear-lists
```

### 6. Limpiar Caché de Detalles

```bash
curl -X DELETE -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8092/api/cache/clear-details
```

---

## 📚 Documentación Swagger

### Auth Service
```
http://localhost:8081/swagger-ui/index.html
```

### Crypto Collector
```
http://localhost:8092/swagger-ui/index.html
```

**En Swagger puedes**:
- Ver todos los endpoints disponibles
- Probar endpoints directamente desde el navegador
- Ver ejemplos de request/response
- Autorizar con tu JWT token (botón "Authorize" arriba a la derecha)

---

## 🧪 Ejemplos Completos

### Ejemplo 1: Flujo Completo (Registro → Login → Consultar Bitcoin)

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","password":"Test1234"}'

# 2. Login y guardar token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

# 3. Consultar detalles de Bitcoin
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/bitcoin
```

### Ejemplo 2: Ver las Top 10 Criptomonedas por Market Cap

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/list?page=0&size=10&sort=marketCapRank,asc" \
  | jq '.content[] | {rank: .marketCapRank, name: .name, price: .currentPrice}'
```

### Ejemplo 3: Verificar Rendimiento del Caché

```bash
# Primera llamada (Cache MISS - más lenta)
time curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/stats > /dev/null

# Segunda llamada (Cache HIT - más rápida)
time curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/stats > /dev/null
```

---

## 🐛 Troubleshooting

### Error: Failed to fetch / CORS

**Problema**: Estás usando el puerto 8080 (API Gateway) que no está configurado.

**Solución**: Usa el puerto **8092** para crypto-collector:
```
❌ http://localhost:8080/api/crypto/bitcoin
✅ http://localhost:8092/api/crypto/bitcoin
```

### Error: 401 Unauthorized

**Problema**: No estás enviando el token JWT o el token expiró.

**Solución**: 
1. Haz login nuevamente para obtener un nuevo token
2. Asegúrate de incluir el header: `Authorization: Bearer <token>`

### Error: 403 Forbidden

**Problema**: El token es inválido o pertenece a un usuario que no existe.

**Solución**: Verifica que el usuario exista en la base de datos.

### Error: 500 Internal Server Error

**Problema**: Error del servidor (revisar logs).

**Solución**:
```bash
docker logs crypto-crypto-collector-micro-1 --tail 50
```

### Verificar que los servicios estén corriendo

```bash
docker compose ps
```

Todos deben mostrar "Up" y "healthy".

---

## 📊 Redis CLI - Comandos Útiles

### Ver todas las keys cacheadas

```bash
docker exec crypto-redis-1 redis-cli KEYS '*'
```

### Ver el contenido de una key

```bash
docker exec crypto-redis-1 redis-cli GET "crypto-stats::SimpleKey []"
```

### Limpiar toda la base de datos de Redis

```bash
docker exec crypto-redis-1 redis-cli FLUSHALL
```

### Ver información de memoria

```bash
docker exec crypto-redis-1 redis-cli INFO memory
```

### Monitorear comandos en tiempo real

```bash
docker exec crypto-redis-1 redis-cli MONITOR
```

---

## 🚀 Scripts de Prueba Automatizados

### Test completo de todos los endpoints

```bash
bash test-endpoints.sh
```

### Demo completa con Redis

```bash
bash demo-completa.sh
```

### Test final de Redis

```bash
bash test-redis-final.sh
```

---

## 📋 Resumen de Cachés

| Cache Name | TTL | Método | Descripción |
|------------|-----|--------|-------------|
| `crypto-stats` | 1 minuto | `getStats()` | Estadísticas generales |
| `crypto-details` | 2 minutos | `findByCoinId()` | Detalles de criptomoneda |
| `scheduler-status` | 1 minuto | `getSchedulerStatus()` | Estado del scheduler |
| `coingecko-api` | 30 segundos | Llamadas API | Respuestas externas |

**Nota**: El caché de `crypto-list` está deshabilitado porque `Page<>` no se serializa correctamente en Redis.

---

## ✅ Checklist de Verificación

- [ ] Todos los servicios están corriendo (`docker compose ps`)
- [ ] Puedes registrarte (`POST /api/auth/register`)
- [ ] Puedes hacer login (`POST /api/auth/login`)
- [ ] Obtienes un token JWT válido
- [ ] Puedes consultar stats (`GET /api/crypto/stats`)
- [ ] Puedes consultar detalles (`GET /api/crypto/bitcoin`)
- [ ] Puedes listar criptomonedas (`GET /api/crypto/list`)
- [ ] Redis tiene keys cacheadas (`docker exec crypto-redis-1 redis-cli KEYS '*'`)
- [ ] Swagger UI accesible (`http://localhost:8092/swagger-ui/index.html`)

---

**Documentación actualizada**: 23 de octubre de 2025  
**Versión**: 1.0.0
