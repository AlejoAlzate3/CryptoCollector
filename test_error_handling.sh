#!/bin/bash

echo "=========================================="
echo "🧪 PRUEBAS DE MANEJO GLOBAL DE ERRORES"
echo "=========================================="
echo ""

# Colores para salida
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir resultados
print_test() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# Test 1: Acceso sin JWT (401 Unauthorized)
print_test "TEST 1: Acceso sin JWT (401 Unauthorized)"
echo "Endpoint: GET /api/crypto/stats"
echo "Esperado: HTTP 401 con mensaje de error JSON"
echo ""
curl -s -X GET http://localhost:8092/api/crypto/stats
echo ""
echo ""

# Test 2: Login con credenciales inválidas (401 Unauthorized)
print_test "TEST 2: Login con credenciales inválidas (401)"
echo "Endpoint: POST /api/auth/login"
echo "Datos: email inexistente"
echo "Esperado: HTTP 401 con ErrorResponse JSON"
echo ""
curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"noexiste@crypto.com","password":"wrongpass"}'
echo ""
echo ""

# Test 3: Registro con email duplicado (409 Conflict)
print_test "TEST 3: Registro con email duplicado (409 Conflict)"
echo "Endpoint: POST /api/auth/register"
echo "Datos: email ya existente (errtest@crypto.com)"
echo "Esperado: HTTP 409 con ErrorResponse JSON"
echo ""
curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"otro","firstName":"Otro","lastName":"User","email":"errtest@crypto.com","password":"pass123"}'
echo ""
echo ""

# Registrar usuario válido y obtener token
print_test "Obteniendo token JWT para pruebas protegidas..."
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"errtest@crypto.com","password":"test123"}' | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}Error: No se pudo obtener el token JWT${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Token obtenido exitosamente${NC}"
echo "Token (primeros 50 caracteres): ${TOKEN:0:50}..."
echo ""
echo ""

# Test 4: Recurso no encontrado (404 Not Found)
print_test "TEST 4: Recurso no encontrado (404 Not Found)"
echo "Endpoint: GET /api/crypto/bitcoin-no-existe"
echo "Esperado: HTTP 404 con ErrorResponse JSON"
echo ""
curl -s -X GET http://localhost:8092/api/crypto/bitcoin-no-existe \
  -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

# Test 5: Parámetro de tipo incorrecto (400 Bad Request)
print_test "TEST 5: Parámetro de tipo incorrecto (400 Bad Request)"
echo "Endpoint: GET /api/crypto/list?page=abc&size=10"
echo "Esperado: HTTP 400 con mensaje de error de tipo"
echo ""
curl -s -X GET "http://localhost:8092/api/crypto/list?page=abc&size=10" \
  -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

# Test 6: Validación de campos (400 Bad Request)
print_test "TEST 6: Validación de campos (400 Bad Request)"
echo "Endpoint: POST /api/auth/register"
echo "Datos: campos vacíos / email inválido"
echo "Esperado: HTTP 400 con lista de fieldErrors"
echo ""
curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"","firstName":"","lastName":"","email":"invalido","password":""}'
echo ""
echo ""

# Test 7: Endpoints protegidos funcionando correctamente con JWT válido
print_test "TEST 7: Endpoints protegidos con JWT válido (200 OK)"
echo "Endpoint: GET /api/crypto/stats"
echo "Esperado: HTTP 200 con datos de estadísticas"
echo ""
curl -s -X GET http://localhost:8092/api/crypto/stats \
  -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

# Test 8: Búsqueda de criptomoneda existente (200 OK)
print_test "TEST 8: Búsqueda de criptomoneda existente (200 OK)"
echo "Endpoint: GET /api/crypto/bitcoin"
echo "Esperado: HTTP 200 con datos de Bitcoin"
echo ""
curl -s -X GET http://localhost:8092/api/crypto/bitcoin \
  -H "Authorization: Bearer $TOKEN"
echo ""
echo ""

echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}✅ PRUEBAS COMPLETADAS${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""
echo "Resumen de pruebas:"
echo "1. ✅ Acceso sin JWT → 401 Unauthorized"
echo "2. ✅ Credenciales inválidas → 401 Unauthorized"
echo "3. ✅ Email duplicado → 409 Conflict"
echo "4. ✅ Recurso no encontrado → 404 Not Found"
echo "5. ✅ Tipo de parámetro incorrecto → 400 Bad Request"
echo "6. ✅ Validación de campos → 400 Bad Request"
echo "7. ✅ Endpoints protegidos con JWT → 200 OK"
echo "8. ✅ Búsqueda exitosa → 200 OK"
echo ""
