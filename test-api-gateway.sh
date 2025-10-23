#!/bin/bash

echo "��� TEST API GATEWAY - Puerto 8080"
echo "=================================="
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "��� Paso 1: Registrando usuario..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Gateway","lastName":"Test","email":"gateway@test.com","password":"Test1234"}')

if echo "$REGISTER_RESPONSE" | grep -q "email"; then
  echo -e "${GREEN}✅ Usuario registrado (o ya existe)${NC}"
else
  echo -e "${RED}❌ Error en registro: $REGISTER_RESPONSE${NC}"
fi
echo ""

echo "��� Paso 2: Login a través del API Gateway..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"gateway@test.com","password":"Test1234"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Error al obtener token${NC}"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi
echo -e "${GREEN}✅ Token obtenido: ${TOKEN:0:50}...${NC}"
echo ""

echo "��� Paso 3: Consultando stats a través del Gateway..."
echo "URL: http://localhost:8080/api/crypto/stats"
STATS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats)

if echo "$STATS_RESPONSE" | grep -q "total"; then
  echo -e "${GREEN}✅ Stats obtenidas correctamente${NC}"
  echo "Response: $STATS_RESPONSE"
else
  echo -e "${RED}❌ Error: $STATS_RESPONSE${NC}"
fi
echo ""

echo "��� Paso 4: Consultando Bitcoin a través del Gateway..."
echo "URL: http://localhost:8080/api/crypto/bitcoin"
BTC_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin)

if echo "$BTC_RESPONSE" | grep -q "bitcoin"; then
  echo -e "${GREEN}✅ Bitcoin obtenido correctamente${NC}"
  NAME=$(echo $BTC_RESPONSE | grep -oP '"name":"[^"]*' | cut -d'"' -f4)
  SYMBOL=$(echo $BTC_RESPONSE | grep -oP '"symbol":"[^"]*' | cut -d'"' -f4)
  echo "Nombre: $NAME"
  echo "Símbolo: $SYMBOL"
else
  echo -e "${RED}❌ Error: $BTC_RESPONSE${NC}"
fi
echo ""

echo "��� Paso 5: Consultando lista a través del Gateway..."
echo "URL: http://localhost:8080/api/crypto/list?page=0&size=3"
LIST_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=0&size=3")

if echo "$LIST_RESPONSE" | grep -q "content"; then
  echo -e "${GREEN}✅ Lista obtenida correctamente${NC}"
  TOTAL=$(echo $LIST_RESPONSE | grep -oP '"totalElements":[0-9]+' | grep -oP '[0-9]+')
  echo "Total de criptomonedas: $TOTAL"
else
  echo -e "${RED}❌ Error: $LIST_RESPONSE${NC}"
fi
echo ""

echo "��� Paso 6: Consultando endpoint PÚBLICO (sin auth)..."
echo "URL: http://localhost:8080/api/public/cache/redis-info"
PUBLIC_RESPONSE=$(curl -s http://localhost:8080/api/public/cache/redis-info)

if echo "$PUBLIC_RESPONSE" | grep -q "redisConfigured"; then
  echo -e "${GREEN}✅ Endpoint público accesible${NC}"
  echo "Response: $PUBLIC_RESPONSE"
else
  echo -e "${RED}❌ Error: $PUBLIC_RESPONSE${NC}"
fi
echo ""

echo "��� Paso 7: Verificando acceso a Swagger a través del Gateway..."
echo "URL: http://localhost:8080/crypto/swagger-ui/index.html"
SWAGGER_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/crypto/swagger-ui/index.html)

if [ "$SWAGGER_STATUS" = "200" ]; then
  echo -e "${GREEN}✅ Swagger accesible (HTTP $SWAGGER_STATUS)${NC}"
else
  echo -e "${RED}❌ Swagger no accesible (HTTP $SWAGGER_STATUS)${NC}"
fi
echo ""

echo "═══════════════════════════════════════════════"
echo -e "${GREEN}✅ TEST API GATEWAY COMPLETADO${NC}"
echo "═══════════════════════════════════════════════"
echo ""
echo "��� URLs del API Gateway (puerto 8080):"
echo "   Auth - Register:     http://localhost:8080/api/auth/register"
echo "   Auth - Login:        http://localhost:8080/api/auth/login"
echo "   Crypto - Stats:      http://localhost:8080/api/crypto/stats"
echo "   Crypto - Details:    http://localhost:8080/api/crypto/{coinId}"
echo "   Crypto - List:       http://localhost:8080/api/crypto/list"
echo "   Redis Info:          http://localhost:8080/api/public/cache/redis-info"
echo "   Swagger Crypto:      http://localhost:8080/crypto/swagger-ui/index.html"
echo "   Swagger Auth:        http://localhost:8080/auth/swagger-ui/index.html"
echo ""
echo "⚠️  NOTA: Ahora usa el puerto 8080 (API Gateway) en lugar del 8092"
