#!/bin/bash

echo "��� TEST API GATEWAY - Puerto 8080 (CORREGIDO)"
echo "=============================================="
echo ""

# Login con usuario válido
echo "��� Paso 1: Login a través del API Gateway..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alejo1@gmail.com","password":"Forever03*"}')

TOKEN=$(echo $LOGIN_RESPONSE | grep -oP '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Error al obtener token"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi
echo "✅ Token obtenido: ${TOKEN:0:50}..."
echo ""

echo "��� Paso 2: Consultando stats a través del Gateway..."
STATS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/stats)
echo "Response: $STATS_RESPONSE"
echo ""

echo "��� Paso 3: Consultando Bitcoin a través del Gateway..."
BTC_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/crypto/bitcoin)

if echo "$BTC_RESPONSE" | grep -q "bitcoin"; then
  echo "✅ Bitcoin obtenido correctamente"
  NAME=$(echo $BTC_RESPONSE | grep -oP '"name":"[^"]*' | cut -d'"' -f4)
  echo "Nombre: $NAME"
else
  echo "❌ Error: $BTC_RESPONSE"
fi
echo ""

echo "��� Paso 4: Consultando lista a través del Gateway..."
LIST_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/crypto/list?page=0&size=3")

if echo "$LIST_RESPONSE" | grep -q "content"; then
  echo "✅ Lista obtenida correctamente"
  TOTAL=$(echo $LIST_RESPONSE | grep -oP '"totalElements":[0-9]+' | grep -oP '[0-9]+')
  echo "Total de criptomonedas: $TOTAL"
fi
echo ""

echo "��� Paso 5: Consultando endpoint PÚBLICO (sin auth)..."
PUBLIC_RESPONSE=$(curl -s http://localhost:8080/api/public/cache/redis-info)
echo "Response: $PUBLIC_RESPONSE"
echo ""

echo "��� Paso 6: Verificando Swagger UIs..."
SWAGGER_CRYPTO=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/crypto/swagger-ui/index.html)
SWAGGER_AUTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/auth/swagger-ui/index.html)

echo "Swagger Crypto: HTTP $SWAGGER_CRYPTO"
echo "Swagger Auth: HTTP $SWAGGER_AUTH"
echo ""

echo "��� Paso 7: Verificando configuración de Swagger..."
CONFIG_CRYPTO=$(curl -s http://localhost:8080/crypto/v3/api-docs/swagger-config | grep -o "configUrl")
CONFIG_AUTH=$(curl -s http://localhost:8080/auth/v3/api-docs/swagger-config | grep -o "configUrl")

if [ -n "$CONFIG_CRYPTO" ]; then
  echo "✅ Configuración Swagger Crypto OK"
else
  echo "❌ Configuración Swagger Crypto ERROR"
fi

if [ -n "$CONFIG_AUTH" ]; then
  echo "✅ Configuración Swagger Auth OK"
else
  echo "❌ Configuración Swagger Auth ERROR"
fi
echo ""

echo "═══════════════════════════════════════════════"
echo "✅ TEST API GATEWAY COMPLETADO"
echo "═══════════════════════════════════════════════"
echo ""
echo "��� URLs para acceder en el navegador:"
echo "   Swagger Crypto:  http://localhost:8080/crypto/swagger-ui/index.html"
echo "   Swagger Auth:    http://localhost:8080/auth/swagger-ui/index.html"
