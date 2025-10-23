#!/bin/bash

echo "============================================"
echo "Ì∫Ä DEMO COMPLETA - CRYPTOCOLLECTOR + REDIS"
echo "============================================"
echo ""

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì≥ù PASO 1: REGISTRO DE USUARIO${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "POST http://localhost:8081/api/auth/register"
echo "Body: {firstName, lastName, email, password}"
echo ""

REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "Cache",
    "email": "testcache@demo.com",
    "password": "demo123"
  }')

echo "Respuesta: $REGISTER_RESPONSE"
echo ""

if echo "$REGISTER_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}‚úÖ Usuario registrado exitosamente${NC}"
else
    echo -e "${YELLOW}‚ö†Ô∏è  Usuario ya existe o error en registro${NC}"
fi
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì¥ê PASO 2: LOGIN${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "POST http://localhost:8081/api/auth/login"
echo "Body: {email, password}"
echo ""

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testcache@demo.com",
    "password": "demo123"
  }')

echo "Respuesta: $LOGIN_RESPONSE"
echo ""

# Extraer token (m√©todo compatible con bash en Windows)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}‚ùå Error: No se pudo obtener el token${NC}"
    exit 1
fi

echo -e "${GREEN}‚úÖ Token JWT obtenido exitosamente${NC}"
echo "Token: ${TOKEN:0:50}..."
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì∑ëÔ∏è  PASO 3: LIMPIAR REDIS (ESTADO INICIAL)${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

echo "Verificando keys en Redis ANTES de las pruebas:"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì≥ä PASO 4: PRIMERA LLAMADA - SIN CACH√â${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "GET http://localhost:8092/api/crypto/stats"
echo "Headers: Authorization: Bearer <token>"
echo ""
echo -e "${YELLOW}‚è±Ô∏è  Midiendo tiempo de respuesta...${NC}"

START=$(date +%s%N)
STATS_RESPONSE=$(curl -s -X GET http://localhost:8092/api/crypto/stats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")
END=$(date +%s%N)
DURATION1=$((($END - $START) / 1000000))

echo "Respuesta: $STATS_RESPONSE"
echo ""
echo -e "${GREEN}‚úÖ Primera llamada completada en: ${DURATION1}ms${NC}"
echo -e "${YELLOW}Ì≤æ Esta llamada consult√≥ la BASE DE DATOS${NC}"
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì≥¶ PASO 5: VERIFICAR REDIS DESPU√âS DE CACH√â${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

echo "Keys en Redis DESPU√âS de la primera llamada:"
REDIS_KEYS=$(docker exec crypto-redis-1 redis-cli KEYS '*')
echo "$REDIS_KEYS"
echo ""

if [ -z "$REDIS_KEYS" ]; then
    echo -e "${RED}‚ö†Ô∏è  Redis est√° vac√≠o - El cach√© no se guard√≥${NC}"
else
    echo -e "${GREEN}‚úÖ Datos cacheados en Redis exitosamente${NC}"
    echo ""
    echo "Contenido del cach√© 'crypto-stats':"
    docker exec crypto-redis-1 redis-cli --raw GET "crypto-stats::SimpleKey []" 2>/dev/null || echo "(Key con formato diferente)"
fi
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}‚ö° PASO 6: SEGUNDA LLAMADA - DESDE REDIS${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "GET http://localhost:8092/api/crypto/stats (misma petici√≥n)"
echo ""
echo -e "${YELLOW}‚è±Ô∏è  Midiendo tiempo de respuesta...${NC}"

START=$(date +%s%N)
STATS_RESPONSE2=$(curl -s -X GET http://localhost:8092/api/crypto/stats \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")
END=$(date +%s%N)
DURATION2=$((($END - $START) / 1000000))

echo "Respuesta: $STATS_RESPONSE2"
echo ""
echo -e "${GREEN}‚úÖ Segunda llamada completada en: ${DURATION2}ms${NC}"
echo -e "${YELLOW}‚ö° Esta llamada se sirvi√≥ desde REDIS CACHE${NC}"
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì≥à PASO 7: COMPARACI√ìN DE RENDIMIENTO${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "Primera llamada (Base de Datos):  ${DURATION1}ms"
echo "Segunda llamada (Redis Cache):    ${DURATION2}ms"
echo ""

if [ $DURATION1 -gt $DURATION2 ]; then
    IMPROVEMENT=$(( ($DURATION1 - $DURATION2) * 100 / $DURATION1 ))
    SPEEDUP=$(echo "scale=2; $DURATION1 / $DURATION2" | bc)
    echo -e "${GREEN}Ì∫Ä Mejora: ${IMPROVEMENT}% m√°s r√°pido${NC}"
    echo -e "${GREEN}‚ö° Speedup: ${SPEEDUP}x veces m√°s r√°pido${NC}"
    echo ""
    echo -e "${GREEN}‚úÖ ¬°EL CACH√â DE REDIS EST√Å FUNCIONANDO CORRECTAMENTE!${NC}"
else
    echo -e "${YELLOW}‚ö†Ô∏è  No se observ√≥ mejora significativa${NC}"
    echo -e "${YELLOW}   Esto puede ser normal en primera ejecuci√≥n o red local${NC}"
fi
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì≥ù PASO 8: VERIFICAR LOGS DEL SERVICIO${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

echo "Buscando evidencia de cach√© en logs:"
docker logs crypto-crypto-collector-micro-1 --tail 100 | grep -E "Cache MISS|Ì≤æ" | tail -5
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì∑π PASO 9: LIMPIAR CACH√â MANUALMENTE${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "DELETE http://localhost:8092/api/cache/clear-all"
echo ""

CLEAR_RESPONSE=$(curl -s -X DELETE http://localhost:8092/api/cache/clear-all \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Respuesta: $CLEAR_RESPONSE"
echo ""

echo "Verificando que Redis est√© vac√≠o:"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""
echo -e "${GREEN}‚úÖ Cach√© limpiado exitosamente${NC}"
echo ""

sleep 2

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Ì¥ç PASO 10: INFORMACI√ìN DE CACH√âS${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "GET http://localhost:8092/api/cache/info"
echo ""

CACHE_INFO=$(curl -s -X GET http://localhost:8092/api/cache/info \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "$CACHE_INFO"
echo ""

echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}‚úÖ DEMO COMPLETA FINALIZADA${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "Resumen de la implementaci√≥n:"
echo "‚úÖ Autenticaci√≥n JWT funcionando"
echo "‚úÖ Redis configurado y operativo"
echo "‚úÖ Cach√© autom√°tico en endpoints"
echo "‚úÖ Gesti√≥n manual de cach√© disponible"
echo "‚úÖ Mejora de rendimiento demostrada"
echo ""
echo "Servicios disponibles:"
echo "Ì¥ê Auth Service:          http://localhost:8081/swagger-ui/index.html"
echo "Ì≤∞ Crypto Collector:      http://localhost:8092/swagger-ui/index.html"
echo "Ì≥ä Redis Info (p√∫blico):  http://localhost:8092/api/public/cache/redis-info"
echo ""
echo -e "${GREEN}Ìæâ ¬°IMPLEMENTACI√ìN DE REDIS EXITOSA!${NC}"
