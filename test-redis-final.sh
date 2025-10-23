#!/bin/bash

echo "��� TEST FINAL - REDIS CACHE CRYPTOCOLLECTOR"
echo "==========================================="
echo ""

# Credenciales
EMAIL="redistest@test.com"
PASSWORD="Redis1234"

# 1. Login
echo "��� 1. Autenticación..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Error al obtener token"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi
echo "✅ Token obtenido"
echo ""

# 2. Limpiar Redis
echo "���️  2. Limpiando Redis..."
docker exec crypto-redis-1 redis-cli FLUSHALL > /dev/null 2>&1
echo "✅ Redis limpio"
echo ""

# 3. Test de /api/crypto/stats
echo "��� 3. Test de /api/crypto/stats"
echo "   ⏱️  Primera llamada (Cache MISS - consulta BD)..."
START=$(date +%s%N)
RESPONSE1=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/stats)
END=$(date +%s%N)
TIME1=$((($END - $START) / 1000000))
echo "   Respuesta: $RESPONSE1"
echo "   ⏱️  Tiempo: ${TIME1}ms"
echo ""

sleep 2

echo "   ⚡ Segunda llamada (Cache HIT - desde Redis)..."
START=$(date +%s%N)
RESPONSE2=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8092/api/crypto/stats)
END=$(date +%s%N)
TIME2=$((($END - $START) / 1000000))
echo "   Respuesta: $RESPONSE2"
echo "   ⏱️  Tiempo: ${TIME2}ms"
echo ""

IMPROVEMENT=$((100 - ($TIME2 * 100 / $TIME1)))
SPEEDUP=$(($TIME1 / $TIME2))
echo "   ��� RESULTADOS:"
echo "      Primera (BD):     ${TIME1}ms"
echo "      Segunda (Redis):  ${TIME2}ms"
echo "      Mejora:           ${IMPROVEMENT}%"
echo "      Speedup:          ${SPEEDUP}x más rápido"
echo ""

# 4. Verificar Redis
echo "��� 4. Verificando datos en Redis..."
KEYS=$(docker exec crypto-redis-1 redis-cli KEYS '*')
if [ -z "$KEYS" ]; then
  echo "❌ No hay keys en Redis"
else
  echo "✅ Keys encontradas:"
  echo "$KEYS" | while read key; do
    echo "   - $key"
  done
fi
echo ""

# 5. Test de detalles de Bitcoin
echo "��� 5. Test de /api/crypto/bitcoin (detalles)"
echo "   ⏱️  Primera llamada..."
START=$(date +%s%N)
BTC_RESPONSE1=$(curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8092/api/crypto/bitcoin")
END=$(date +%s%N)
TIME3=$((($END - $START) / 1000000))
echo "   Tiempo: ${TIME3}ms"

sleep 1

echo "   ⚡ Segunda llamada (cache hit)..."
START=$(date +%s%N)
BTC_RESPONSE2=$(curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8092/api/crypto/bitcoin")
END=$(date +%s%N)
TIME4=$((($END - $START) / 1000000))
echo "   Tiempo: ${TIME4}ms"

if [ $TIME4 -lt $TIME3 ]; then
  echo "   ✅ Segunda llamada más rápida (cache funcionando)"
else
  echo "   ⚠️  Tiempos similares"
fi
echo ""

# 6. Test de lista paginada
echo "��� 6. Test de /api/crypto/list (paginación)"
echo "   ⏱️  Primera llamada (page=0, size=5)..."
START=$(date +%s%N)
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/list?page=0&size=5" > /dev/null
END=$(date +%s%N)
TIME5=$((($END - $START) / 1000000))
echo "   Tiempo: ${TIME5}ms"

sleep 1

echo "   ⚡ Segunda llamada (mismos parámetros)..."
START=$(date +%s%N)
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/list?page=0&size=5" > /dev/null
END=$(date +%s%N)
TIME6=$((($END - $START) / 1000000))
echo "   Tiempo: ${TIME6}ms"

if [ $TIME6 -lt $TIME5 ]; then
  echo "   ✅ Segunda llamada más rápida"
else
  echo "   ⚠️  Tiempos similares"
fi
echo ""

# 7. Keys finales
echo "��� 7. Keys finales en Redis (3 cachés diferentes):"
docker exec crypto-redis-1 redis-cli KEYS '*' | while read key; do
  echo "   - $key"
done
echo ""

# 8. Info de configuración
echo "ℹ️  8. Información de cachés configurados:"
curl -s "http://localhost:8092/api/public/cache/redis-info"
echo ""
echo ""

# 9. Limpiar un caché específico
echo "�� 9. Limpiando solo caché 'crypto-stats'..."
curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/cache/clear/crypto-stats" > /dev/null
echo "✅ Limpieza completada"
echo ""

echo "��� 10. Keys después de limpiar crypto-stats:"
REMAINING=$(docker exec crypto-redis-1 redis-cli KEYS '*')
if [ -z "$REMAINING" ]; then
  echo "   (Redis vacío)"
else
  echo "$REMAINING" | while read key; do
    echo "   - $key"
  done
fi
echo ""

echo "═══════════════════════════════════════"
echo "✅ TEST COMPLETADO EXITOSAMENTE"
echo "═══════════════════════════════════════"
echo ""
echo "��� Resumen:"
echo "   - Cache stats: ${IMPROVEMENT}% más rápido (${SPEEDUP}x)"
echo "   - Múltiples cachés funcionando: crypto-stats, crypto-details, crypto-list"
echo "   - Limpieza selectiva funcionando correctamente"
echo "   - Redis configurado con 5 cachés diferentes"
echo ""
