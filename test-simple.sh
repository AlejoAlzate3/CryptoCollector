#!/bin/bash

echo "�� TEST SIMPLE DE REDIS CACHE"
echo "=============================="
echo ""

# 1. Login
echo "1. Login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testcache@demo.com","password":"Test1234"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Error al obtener token"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi
echo "✅ Token obtenido: ${TOKEN:0:50}..."
echo ""

# 2. Limpiar Redis
echo "2. Limpiando Redis..."
docker exec crypto-redis-1 redis-cli FLUSHALL > /dev/null 2>&1
echo "✅ Redis limpio"
echo ""

# 3. Primera llamada a stats
echo "3. Primera llamada a /api/crypto/stats (Cache MISS)..."
START=$(date +%s%N)
RESPONSE1=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/stats)
END=$(date +%s%N)
TIME1=$((($END - $START) / 1000000))
echo "Respuesta: $RESPONSE1"
echo "Tiempo: ${TIME1}ms"
echo ""

sleep 2

# 4. Segunda llamada a stats
echo "4. Segunda llamada a /api/crypto/stats (Cache HIT)..."
START=$(date +%s%N)
RESPONSE2=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/stats)
END=$(date +%s%N)
TIME2=$((($END - $START) / 1000000))
echo "Respuesta: $RESPONSE2"
echo "Tiempo: ${TIME2}ms"
echo ""

# 5. Comparación
IMPROVEMENT=$((100 - ($TIME2 * 100 / $TIME1)))
echo "��� COMPARACIÓN:"
echo "   Primera:  ${TIME1}ms (Base de datos)"
echo "   Segunda:  ${TIME2}ms (Redis cache)"
echo "   Mejora:   ${IMPROVEMENT}%"
echo ""

# 6. Keys en Redis
echo "5. Keys almacenadas en Redis:"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

# 7. Ver contenido de una key
echo "6. Contenido del caché crypto-stats:"
docker exec crypto-redis-1 redis-cli GET 'crypto-stats::SimpleKey []' 2>/dev/null || echo "(no encontrado)"
echo ""

# 8. Test de bitcoin
echo "7. Test de /api/crypto/bitcoin..."
START=$(date +%s%N)
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8092/api/crypto/bitcoin" > /dev/null
END=$(date +%s%N)
TIME3=$((($END - $START) / 1000000))
echo "Primera llamada: ${TIME3}ms"

sleep 1

START=$(date +%s%N)
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8092/api/crypto/bitcoin" > /dev/null
END=$(date +%s%N)
TIME4=$((($END - $START) / 1000000))
echo "Segunda llamada: ${TIME4}ms (cache hit)"
echo ""

# 9. Keys finales
echo "8. Keys finales en Redis:"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

# 10. Info de Redis
echo "9. Información de Redis:"
curl -s "http://localhost:8092/api/public/cache/redis-info"
echo ""
echo ""

echo "✅ TEST COMPLETADO"
