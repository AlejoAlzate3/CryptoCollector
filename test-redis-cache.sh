#!/bin/bash

echo "============================================"
echo "🧪 DEMOSTRACIÓN DE REDIS CACHE"
echo "============================================"
echo ""

echo "📊 Verificando contenido de Redis (debería estar vacío)..."
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

echo "============================================"
echo "🔍 PRIMERA LLAMADA (sin caché)"
echo "============================================"
echo "GET http://localhost:8092/api/crypto/stats"
echo ""
echo "⏱️  Midiendo tiempo de respuesta..."
START=$(date +%s%N)
curl -s http://localhost:8092/api/crypto/stats > /dev/null
END=$(date +%s%N)
DURATION1=$((($END - $START) / 1000000))  # Convertir a milisegundos
echo "✅ Primera llamada completada en: ${DURATION1}ms"
echo ""

echo "📦 Verificando que Redis ahora tiene datos cacheados..."
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

sleep 2

echo "============================================"
echo "⚡ SEGUNDA LLAMADA (desde caché Redis)"
echo "============================================"
echo "GET http://localhost:8092/api/crypto/stats"
echo ""
echo "⏱️  Midiendo tiempo de respuesta..."
START=$(date +%s%N)
curl -s http://localhost:8092/api/crypto/stats > /dev/null
END=$(date +%s%N)
DURATION2=$((($END - $START) / 1000000))  # Convertir a milisegundos
echo "✅ Segunda llamada completada en: ${DURATION2}ms"
echo ""

echo "============================================"
echo "📊 RESULTADOS"
echo "============================================"
echo "Primera llamada (BD):    ${DURATION1}ms"
echo "Segunda llamada (Redis): ${DURATION2}ms"

if [ $DURATION1 -gt $DURATION2 ]; then
    IMPROVEMENT=$(( ($DURATION1 - $DURATION2) * 100 / $DURATION1 ))
    echo "🚀 Mejora:               ${IMPROVEMENT}% más rápido"
    echo "✅ ¡El caché está funcionando correctamente!"
else
    echo "⚠️  No se observó mejora de rendimiento"
fi

echo ""
echo "============================================"
echo "📝 Verificando logs del servicio"
echo "============================================"
docker logs crypto-crypto-collector-micro-1 --tail 50 | grep -E "Cache MISS|Cache HIT|stats"
echo ""

echo "✅ Demo completada"
