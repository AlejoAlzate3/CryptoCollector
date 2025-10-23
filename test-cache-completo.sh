#!/bin/bash

echo "��� TEST COMPLETO DE REDIS CACHE"
echo "================================"
echo ""

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. Obtener token
echo -e "${BLUE}1. Obteniendo token JWT...${NC}"
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testcache@demo.com","password":"Test1234"}' \
  | jq -r '.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "❌ Error al obtener token"
  exit 1
fi
echo -e "${GREEN}✅ Token obtenido${NC}"
echo ""

# 2. Limpiar Redis
echo -e "${BLUE}2. Limpiando Redis...${NC}"
docker exec crypto-redis-1 redis-cli FLUSHALL > /dev/null 2>&1
echo -e "${GREEN}✅ Redis limpio${NC}"
echo ""

# 3. Test de /api/crypto/stats
echo -e "${BLUE}3. Testing /api/crypto/stats${NC}"
echo "   Primera llamada (Cache MISS)..."
RESPONSE1=$(curl -s -w "\nTime: %{time_total}s" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/stats)
echo "$RESPONSE1"

sleep 2

echo ""
echo "   Segunda llamada (Cache HIT)..."
RESPONSE2=$(curl -s -w "\nTime: %{time_total}s" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/stats)
echo "$RESPONSE2"
echo ""

# 4. Verificar keys en Redis
echo -e "${BLUE}4. Keys en Redis:${NC}"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

# 5. Test de /api/crypto/list
echo -e "${BLUE}5. Testing /api/crypto/list (primera página)${NC}"
echo "   Primera llamada..."
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/list?page=0&size=5" \
  | jq '.content[] | {name: .name, symbol: .symbol, price: .currentPrice}' | head -10
echo ""

sleep 1

echo "   Segunda llamada (debe ser más rápida)..."
curl -s -w "Time: %{time_total}s\n" -o /dev/null \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/list?page=0&size=5"
echo ""

# 6. Keys actuales en Redis
echo -e "${BLUE}6. Keys después de múltiples requests:${NC}"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

# 7. Test de caché de detalles
echo -e "${BLUE}7. Testing /api/crypto/bitcoin (detalles)${NC}"
echo "   Primera llamada..."
curl -s -w "Time: %{time_total}s\n" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/bitcoin" \
  | jq '{name: .name, symbol: .symbol, price: .currentPrice, marketCap: .marketCap}'
echo ""

sleep 1

echo "   Segunda llamada..."
curl -s -w "Time: %{time_total}s\n" \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/bitcoin" \
  | jq '{name: .name, symbol: .symbol, price: .currentPrice, marketCap: .marketCap}'
echo ""

# 8. Info de cachés
echo -e "${BLUE}8. Información de cachés configurados:${NC}"
curl -s "http://localhost:8092/api/public/cache/redis-info" | jq '.'
echo ""

# 9. Limpiar caché específico
echo -e "${BLUE}9. Limpiando caché 'crypto-stats'...${NC}"
curl -s -X DELETE -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/cache/clear/crypto-stats"
echo -e "${GREEN}✅ Caché limpiado${NC}"
echo ""

# 10. Verificar que se limpió
echo -e "${BLUE}10. Verificando limpieza (solo crypto-stats debe desaparecer):${NC}"
docker exec crypto-redis-1 redis-cli KEYS '*'
echo ""

echo -e "${GREEN}✅ TEST COMPLETO FINALIZADO${NC}"
