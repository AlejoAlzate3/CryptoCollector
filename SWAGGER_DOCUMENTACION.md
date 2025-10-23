# 📚 Documentación Swagger/OpenAPI - CryptoCollector

## ✅ Estado: CONFIGURADO Y FUNCIONAL

Swagger/OpenAPI está completamente configurado en ambos microservicios con documentación mejorada y soporte para autenticación JWT.

---

## 🌐 URLs de Acceso

### 🔐 Auth Service (Puerto 8081)
- **Swagger UI**: http://localhost:8081/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs
- **OpenAPI YAML**: http://localhost:8081/v3/api-docs.yaml

### 💰 Crypto Collector Service (Puerto 8092)
- **Swagger UI**: http://localhost:8092/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8092/v3/api-docs
- **OpenAPI YAML**: http://localhost:8092/v3/api-docs.yaml

---

## 🚀 Guía Rápida de Uso

### Paso 1: Probar Auth Service

1. Abre: **http://localhost:8081/swagger-ui/index.html**
2. Expande **POST /api/auth/register**
3. Haz clic en **"Try it out"**
4. Ingresa los datos del usuario:
```json
{
  "username": "testuser",
  "firstName": "Test",
  "lastName": "User",
  "email": "test@crypto.com",
  "password": "password123"
}
```
5. Haz clic en **"Execute"**
6. Deberías recibir HTTP 200 con los datos del usuario creado

### Paso 2: Obtener Token JWT

1. Expande **POST /api/auth/login**
2. Haz clic en **"Try it out"**
3. Ingresa las credenciales:
```json
{
  "email": "test@crypto.com",
  "password": "password123"
}
```
4. Haz clic en **"Execute"**
5. **Copia el token** de la respuesta (sin las comillas):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGNyeXB0by5jb20i..."
}
```

### Paso 3: Autenticar en Crypto Collector

1. Abre: **http://localhost:8092/swagger-ui/index.html**
2. Haz clic en el botón **"Authorize"** (candado verde en la parte superior derecha)
3. En el campo **Value**, pega el token JWT completo
4. Haz clic en **"Authorize"** y luego **"Close"**
5. Ahora el candado debería aparecer cerrado 🔒

### Paso 4: Probar Endpoints Protegidos

#### 📊 Obtener Estadísticas
1. Expande **GET /api/crypto/stats**
2. Haz clic en **"Try it out"**
3. Haz clic en **"Execute"**
4. Deberías ver:
```json
{
  "lastUpdated": "2025-10-22T23:00:00Z",
  "total": 1016,
  "hasSyncedData": true
}
```

#### 📋 Listar Criptomonedas
1. Expande **GET /api/crypto/list**
2. Haz clic en **"Try it out"**
3. Configura los parámetros:
   - `page`: 0
   - `size`: 10
   - `sortBy`: marketCapRank
   - `dir`: asc
4. Haz clic en **"Execute"**
5. Deberías ver las top 10 criptomonedas

#### 🔍 Buscar Criptomoneda Específica
1. Expande **GET /api/crypto/{coinId}**
2. Haz clic en **"Try it out"**
3. Ingresa `coinId`: **bitcoin**
4. Haz clic en **"Execute"**
5. Deberías ver todos los detalles de Bitcoin

---

## 📋 Endpoints Documentados

### Auth Service

| Método | Endpoint | Descripción | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Registrar nuevo usuario | ❌ No |
| POST | `/api/auth/login` | Iniciar sesión y obtener JWT | ❌ No |

### Crypto Collector Service

| Método | Endpoint | Descripción | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/crypto/sync` | Sincronizar cryptos desde CoinGecko | ✅ Sí (JWT) |
| GET | `/api/crypto/list` | Listar cryptos con paginación | ✅ Sí (JWT) |
| GET | `/api/crypto/{coinId}` | Obtener detalles de una crypto | ✅ Sí (JWT) |
| GET | `/api/crypto/stats` | Obtener estadísticas de la DB | ✅ Sí (JWT) |
| GET | `/api/crypto/scheduler/status` | Estado del scheduler | ✅ Sí (JWT) |

---

## 🎨 Características de Swagger Implementadas

### 1. Información General
- ✅ Título personalizado de cada API
- ✅ Descripción detallada
- ✅ Versión (1.0.0)
- ✅ Información de contacto
- ✅ Licencia (Apache 2.0)

### 2. Servidores Configurados
- ✅ Servidor local (DEV)
- ✅ API Gateway

### 3. Seguridad
- ✅ Esquema de autenticación JWT (bearerAuth)
- ✅ Botón "Authorize" en Swagger UI
- ✅ Formato Bearer Token
- ✅ Descripción de cómo obtener el token

### 4. Documentación de Endpoints
- ✅ Operaciones con `@Operation`
- ✅ Descripciones detalladas
- ✅ Parámetros documentados con `@Parameter`
- ✅ Respuestas documentadas con `@ApiResponses`
- ✅ Códigos HTTP (200, 400, 401, 404, 409, 502)
- ✅ Schemas de ErrorResponse incluidos

### 5. Tags
- ✅ "Autenticación" para Auth Service
- ✅ "Criptomonedas" para Crypto Collector
- ✅ Agrupación lógica de endpoints

---

## 🔧 Configuración Técnica

### Dependencias (ambos servicios)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

### Configuración de Seguridad

#### Auth Service
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
    .anyRequest().authenticated()
)
```

#### Crypto Collector Service
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
    .requestMatchers("/api/crypto/**").authenticated()
    .anyRequest().denyAll()
)
```

### Filtro JWT
Ambos servicios excluyen rutas de Swagger en `shouldNotFilter()`:
```java
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator") || 
           path.startsWith("/v3/api-docs") ||
           path.startsWith("/swagger-ui");
}
```

---

## 🧪 Ejemplos de Prueba

### Ejemplo 1: Flujo Completo

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo",
    "firstName": "Demo",
    "lastName": "User",
    "email": "demo@crypto.com",
    "password": "demo123"
  }'

# 2. Login y obtener token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@crypto.com","password":"demo123"}' | \
  jq -r '.token')

# 3. Consultar stats con JWT
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/stats

# 4. Listar top 5 cryptos
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8092/api/crypto/list?page=0&size=5"

# 5. Buscar Bitcoin
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/bitcoin
```

---

## 📱 Exportar Documentación

### OpenAPI JSON
```bash
# Auth Service
curl http://localhost:8081/v3/api-docs > auth-service-openapi.json

# Crypto Collector
curl http://localhost:8092/v3/api-docs > crypto-service-openapi.json
```

### OpenAPI YAML
```bash
# Auth Service
curl http://localhost:8081/v3/api-docs.yaml > auth-service-openapi.yaml

# Crypto Collector
curl http://localhost:8092/v3/api-docs.yaml > crypto-service-openapi.yaml
```

---

## 🎯 Mejores Prácticas Implementadas

1. ✅ **Separación de responsabilidades**: Auth y Crypto en servicios separados
2. ✅ **Documentación completa**: Todos los endpoints documentados
3. ✅ **Seguridad JWT**: Integración completa con bearer tokens
4. ✅ **Códigos HTTP correctos**: 200, 400, 401, 404, 409, 502, 503
5. ✅ **Schemas compartidos**: ErrorResponse para respuestas de error
6. ✅ **Versionado**: Version 1.0.0 en la API
7. ✅ **Servidores múltiples**: Local y Gateway configurados
8. ✅ **Información de contacto**: Email y licencia incluidos

---

## 🐛 Troubleshooting

### Problema: Swagger UI devuelve 401
**Solución**: Verifica que las rutas de Swagger estén en `permitAll()` en SecurityConfig y en `shouldNotFilter()` del JwtAuthenticationFilter.

### Problema: Token JWT no funciona
**Solución**: 
1. Verifica que el token sea válido y no esté expirado (24 horas)
2. Asegúrate de usar "Bearer " + token en el header Authorization
3. En Swagger UI, solo pega el token sin "Bearer "

### Problema: No veo los endpoints en Swagger
**Solución**: 
1. Verifica que el servicio esté corriendo: `docker compose ps`
2. Revisa los logs: `docker logs crypto-auth-service-1`
3. Accede directamente a `/v3/api-docs` para ver el JSON

---

## 📖 Recursos Adicionales

- [Springdoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)

---

**Última actualización**: 22 de octubre de 2025  
**Versión**: 1.0.0  
**Estado**: ✅ Completamente funcional y documentado
