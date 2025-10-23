#!/bin/bash

echo "��� TEST DE TODOS LOS ENDPOINTS"
echo "==============================="
echo ""

# Obtener token
echo "1. Obteniendo token JWT..."
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"redistest@test.com","password":"Redis1234"}' \
  | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Error al obtener token"
  exit 1
fi
echo "✅ Token obtenido"
echo ""

# Test 1: Endpoint público de Redis info
echo "2. Test de endpoint PÚBLICO (sin auth):"
echo "   GET /api/public/cache/redis-info"
RESPONSE=$(curl -s http://localhost:8092/api/public/cache/redis-info)
if echo "$RESPONSE" | grep -q "redisConfigured"; then
  echo "   ✅ Funcionando - Redis info disponible"
else
  echo "   ❌ Error: $RESPONSE"
fi
echo ""

# Test 2: Stats
echo "3. Test de /api/crypto/stats (protegido con JWT):"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/stats)
if echo "$RESPONSE" | grep -q "total"; then
  echo "   ✅ Funcionando - Stats obtenidas"
  echo "   Respuesta: $RESPONSE"
else
  echo "   ❌ Error: $RESPONSE"
fi
echo ""

# Test 3: Bitcoin details
echo "4. Test de /api/crypto/bitcoin (detalles con cache):"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/bitcoin)
if echo "$RESPONSE" | grep -q "bitcoin"; then
  echo "   ✅ Funcionando - Detalles de Bitcoin"
  echo "   Nombre: $(echo $RESPONSE | grep -oP '"name":"[^"]*' | cut -d'"' -f4)"
  echo "   Símbolo: $(echo $RESPONSE | grep -oP '"symbol":"[^"]*' | cut -d'"' -f4)"
else
  echo "   ❌ Error: $RESPONSE"
fi
echo ""

# Test 4: Lista paginada
echo "5. Test de /api/crypto/list (lista paginada):"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8092/api/crypto/list?page=0&size=3")
if echo "$RESPONSE" | grep -q "content"; then
  echo "   ✅ Funcionando - Lista obtenida"
  TOTAL=$(echo $RESPONSE | grep -oP '"totalElements":[0-9]+' | grep -oP '[0-9]+')
  echo "   Total de criptomonedas: $TOTAL"
else
  echo "   ❌ Error: $RESPONSE"
fi
echo ""

# Test 5: Búsqueda
echo "6. Test de /api/crypto/list?query=bitcoin (búsqueda):"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8092/api/crypto/list?query=bitcoin&page=0&size=5")
if echo "$RESPONSE" | grep -q "bitcoin"; then
  echo "   ✅ Funcionando - Búsqueda de Bitcoin"
else
  echo "   ❌ Error: $RESPONSE"
fi
echo ""

# Test 6: Scheduler status
echo "7. Test de /api/crypto/scheduler/status:"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/scheduler/status)
if echo "$RESPONSE" | grep -q "enabled"; then
  echo "   ✅ Funcionando - Estado del scheduler"
else
  echo "   ❌ Error: $RESPONSE"
fi
echo ""

# Test 7: Swagger UI
echo "8. Test de acceso a Swagger UI:"
SWAGGER_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8092/swagger-ui/index.html)
if [ "$SWAGGER_RESPONSE" = "200" ]; then
  echo "   ✅ Swagger UI accesible en http://localhost:8092/swagger-ui/index.html"
else
  echo "   ❌ Error: HTTP $SWAGGER_RESPONSE"
fi
echo ""

# Verificar cache en Redis
echo "9. Verificando datos en Redis:"
KEYS=$(docker exec crypto-redis-1 redis-cli KEYS '*' 2>/dev/null | wc -l)
echo "   Keys en Redis: $KEYS"
if [ $KEYS -gt 0 ]; then
  echo "   ✅ Cache funcionando"
  docker exec crypto-redis-1 redis-cli KEYS '*' 2>/dev/null | head -5 | while read key; do
    echo "      - $key"
  done
else
  echo "   ⚠️  No hay keys cacheadas (esperado en primera ejecución)"
fi
echo ""

echo "═══════════════════════════════════════"
echo "✅ TEST COMPLETADO"
echo "═══════════════════════════════════════"
echo ""
echo "��� URLs para acceder:"
echo "   Auth Swagger:    http://localhost:8081/swagger-ui/index.html"
echo "   Crypto Swagger:  http://localhost:8092/swagger-ui/index.html"
echo "   Eureka:          http://localhost:8761"
echo "   Redis Info:      http://localhost:8092/api/public/cache/redis-info"
echo ""
echo "⚠️  NOTA: Usar puerto 8092 (no 8080) para crypto-collector"
echo "   El puerto 8080 es para API Gateway (actualmente no configurado para enrutar)"
