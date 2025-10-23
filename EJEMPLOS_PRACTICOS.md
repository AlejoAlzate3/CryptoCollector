# 🎯 Ejemplos Prácticos - API Gateway

## 🚀 Guía Rápida de Uso

Este documento contiene ejemplos prácticos listos para copiar y pegar.

---

## 📋 Tabla de Contenidos
1. [Autenticación](#autenticación)
2. [Consultar Criptomonedas](#consultar-criptomonedas)
3. [Gestión de Caché](#gestión-de-caché)
4. [Scripts Útiles](#scripts-útiles)

---

## 🔐 1. Autenticación

### Registrar un nuevo usuario
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan.perez@example.com",
    "password": "Password123"
  }'
```

### Hacer login y guardar el token
```bash
# Login y extraer token automáticamente
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan.perez@example.com",
    "password": "Password123"
  }' | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

# Verificar que el token se guardó
echo "Token: $TOKEN"
```

### Usar el token en peticiones
```bash
# Ahora puedes usar $TOKEN en todas las peticiones
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats
```

---

## 📊 2. Consultar Criptomonedas

### Ver estadísticas generales
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats
```

**Respuesta esperada:**
```json
{
  "total": 1016,
  "lastUpdated": "2025-10-23T00:00:00.524Z",
  "hasSyncedData": true
}
```

### Consultar Bitcoin
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin
```

### Consultar Ethereum
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/ethereum
```

### Consultar Cardano
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/cardano
```

### Listar las primeras 5 criptomonedas
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=0&size=5"
```

### Listar criptomonedas de la página 2
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=1&size=10"
```

### Ver estado del scheduler
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/scheduler-status
```

---

## 🗄️ 3. Gestión de Caché

### Ver información de Redis (sin autenticación)
```bash
curl http://localhost:8080/api/public/cache/redis-info
```

**Respuesta esperada:**
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

### Limpiar todas las cachés
```bash
curl -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cache/clear-all
```

### Limpiar caché de detalles de criptomonedas
```bash
curl -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cache/clear/crypto-details
```

### Limpiar caché de estadísticas
```bash
curl -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cache/clear/crypto-stats
```

---

## 🛠️ 4. Scripts Útiles

### Script completo de prueba
```bash
#!/bin/bash

echo "🚀 Iniciando prueba completa..."

# 1. Registrar usuario
echo "📝 Registrando usuario..."
REGISTER=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "Test1234"
  }')

echo "Resultado: $REGISTER"

# 2. Login
echo "🔐 Haciendo login..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234"
  }' | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Error: No se pudo obtener el token"
  exit 1
fi

echo "✅ Token obtenido: ${TOKEN:0:50}..."

# 3. Consultar stats
echo "📊 Consultando estadísticas..."
STATS=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats)
echo "Stats: $STATS"

# 4. Consultar Bitcoin
echo "🪙 Consultando Bitcoin..."
BTC=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin)
echo "Bitcoin: $BTC"

# 5. Ver info de Redis
echo "🗄️ Consultando Redis..."
REDIS=$(curl -s http://localhost:8080/api/public/cache/redis-info)
echo "Redis: $REDIS"

echo "✅ Prueba completada!"
```

### Script para consultar múltiples criptomonedas
```bash
#!/bin/bash

# Login primero
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

# Lista de criptomonedas para consultar
CRYPTOS=("bitcoin" "ethereum" "cardano" "polkadot" "solana")

echo "💰 Consultando precios de criptomonedas..."
echo "=========================================="

for crypto in "${CRYPTOS[@]}"; do
  echo ""
  echo "📊 $crypto:"
  RESULT=$(curl -s -H "Authorization: Bearer $TOKEN" \
    "http://localhost:8080/api/crypto/$crypto")
  
  # Extraer nombre y precio
  NAME=$(echo $RESULT | grep -oP '"name":"[^"]*' | cut -d'"' -f4)
  PRICE=$(echo $RESULT | grep -oP '"currentPrice":[0-9.]+' | cut -d':' -f2)
  CHANGE=$(echo $RESULT | grep -oP '"priceChangePercentage24h":-?[0-9.]+' | cut -d':' -f2)
  
  echo "  Nombre: $NAME"
  echo "  Precio: $PRICE USD"
  echo "  Cambio 24h: $CHANGE%"
done

echo ""
echo "✅ Consulta completada!"
```

### Script para limpiar todas las cachés
```bash
#!/bin/bash

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

# Limpiar caché
echo "🗑️ Limpiando todas las cachés..."
RESULT=$(curl -s -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cache/clear-all)

echo "Resultado: $RESULT"
echo "✅ Cachés limpiadas!"
```

### Script para monitorear cambios de precio
```bash
#!/bin/bash

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

CRYPTO=${1:-bitcoin}  # Por defecto: bitcoin
INTERVAL=${2:-60}     # Por defecto: 60 segundos

echo "📊 Monitoreando $CRYPTO cada $INTERVAL segundos..."
echo "Presiona Ctrl+C para detener"
echo ""

while true; do
  RESULT=$(curl -s -H "Authorization: Bearer $TOKEN" \
    "http://localhost:8080/api/crypto/$CRYPTO")
  
  TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")
  PRICE=$(echo $RESULT | grep -oP '"currentPrice":[0-9.]+' | cut -d':' -f2)
  CHANGE=$(echo $RESULT | grep -oP '"priceChangePercentage24h":-?[0-9.]+' | cut -d':' -f2)
  
  echo "[$TIMESTAMP] $CRYPTO: \$$PRICE (24h: $CHANGE%)"
  
  sleep $INTERVAL
done
```

**Uso:**
```bash
# Monitorear Bitcoin cada 60 segundos (por defecto)
./monitor-price.sh

# Monitorear Ethereum cada 30 segundos
./monitor-price.sh ethereum 30

# Monitorear Cardano cada 2 minutos
./monitor-price.sh cardano 120
```

---

## 🧪 5. Pruebas con Postman

### Colección de Postman

1. **Crear variable de entorno**:
   - Variable: `base_url`
   - Valor: `http://localhost:8080`

2. **Crear variable para token**:
   - Variable: `auth_token`
   - Valor: (se llenará automáticamente)

### Request 1: Register
```
POST {{base_url}}/api/auth/register
Content-Type: application/json

{
  "firstName": "Test",
  "lastName": "User",
  "email": "test@example.com",
  "password": "Test1234"
}
```

### Request 2: Login (con script para guardar token)
```
POST {{base_url}}/api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "Test1234"
}

// Script (Tests tab):
var jsonData = pm.response.json();
pm.environment.set("auth_token", jsonData.token);
```

### Request 3: Get Stats
```
GET {{base_url}}/api/crypto/stats
Authorization: Bearer {{auth_token}}
```

### Request 4: Get Bitcoin
```
GET {{base_url}}/api/crypto/bitcoin
Authorization: Bearer {{auth_token}}
```

### Request 5: List Cryptos
```
GET {{base_url}}/api/crypto/list?page=0&size=10
Authorization: Bearer {{auth_token}}
```

---

## 📝 6. Casos de Uso Comunes

### Caso 1: Obtener precio actual de Bitcoin
```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

# 2. Obtener Bitcoin
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin \
  | grep -oP '"currentPrice":[0-9.]+' | cut -d':' -f2
```

### Caso 2: Comparar precios de top 10 criptomonedas
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=0&size=10" \
  | jq '.content[] | {name: .name, price: .currentPrice, change: .priceChangePercentage24h}'
```

### Caso 3: Verificar si hay datos sincronizados
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats \
  | jq '.hasSyncedData'
```

---

## 🎨 7. Ejemplos con formato bonito (usando jq)

### Ver estadísticas formateadas
```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats | jq
```

### Ver Bitcoin formateado
```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin | jq '{
    nombre: .name,
    simbolo: .symbol,
    precio: .currentPrice,
    "cambio_24h": .priceChangePercentage24h,
    "cap_mercado": .marketCap
  }'
```

### Ver top 5 por precio
```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=0&size=5" \
  | jq '.content | sort_by(.currentPrice) | reverse | .[] | {
      rank: .marketCapRank,
      nombre: .name,
      precio: .currentPrice
    }'
```

---

## 🔍 8. Debugging

### Ver headers de respuesta
```bash
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats
```

### Ver tiempo de respuesta
```bash
time curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin > /dev/null
```

### Probar conectividad sin autenticación
```bash
curl -v http://localhost:8080/api/public/cache/redis-info
```

---

## ✅ 9. Checklist de Validación

Usa estos comandos para verificar que todo funciona:

```bash
# ✅ 1. Gateway está activo
curl -s http://localhost:8080/actuator/health

# ✅ 2. Puedes hacer login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep token

# ✅ 3. Endpoints protegidos funcionan
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats

# ✅ 4. Endpoints públicos funcionan
curl -s http://localhost:8080/api/public/cache/redis-info

# ✅ 5. Swagger está accesible
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/crypto/swagger-ui/index.html
```

Si todos retornan datos correctos, ¡todo está funcionando! 🎉

---

**📚 Más información**: Ver `GUIA_ACCESO_API_GATEWAY.md` para documentación completa.
