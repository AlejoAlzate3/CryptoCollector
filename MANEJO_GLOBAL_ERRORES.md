# 🔧 Sistema de Manejo Global de Errores - CryptoCollector

## 📋 Resumen

Se ha implementado un **sistema robusto de manejo global de errores** en ambos microservicios (`auth-microServices` y `crypto-collector-micro`) utilizando **`@RestControllerAdvice`** y excepciones personalizadas.

---

## 🏗️ Arquitectura Implementada

### 1. Componentes Creados

#### **A. DTOs (Data Transfer Objects)**

**📄 ErrorResponse.java** (en ambos microservicios)
```java
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;    // Fecha y hora del error
    private int status;                  // Código HTTP
    private String error;                // Nombre del error HTTP
    private String message;              // Mensaje descriptivo
    private String path;                 // Ruta del endpoint
    private List<FieldError> fieldErrors; // Errores de validación de campos
    
    @Data
    @Builder
    public static class FieldError {
        private String field;           // Campo con error
        private String message;         // Mensaje de error
        private Object rejectedValue;   // Valor rechazado
    }
}
```

#### **B. Excepciones Personalizadas**

**auth-microServices:**
- ✅ `ResourceNotFoundException` - Recurso no encontrado (404)
- ✅ `ConflictException` - Conflicto de datos (409)
- ✅ `InvalidCredentialsException` - Credenciales inválidas (401)

**crypto-collector-micro:**
- ✅ `ResourceNotFoundException` - Recurso no encontrado (404)
- ✅ `NoDataAvailableException` - Datos no disponibles (503)
- ✅ `ExternalApiException` - Error en API externa (502)

#### **C. Manejador Global de Excepciones**

**📄 GlobalExceptionHandler.java** (en ambos microservicios)

Captura automáticamente todas las excepciones y devuelve respuestas JSON estandarizadas.

---

## 🎯 Tipos de Errores Manejados

### 1️⃣ **400 Bad Request - Solicitud Incorrecta**

**Casos:**
- ❌ Validación de campos fallida (`@NotBlank`, `@Email`, etc.)
- ❌ Tipo de parámetro incorrecto
- ❌ Argumentos ilegales

**Ejemplo de uso:**
```java
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    // Spring automáticamente valida y lanza MethodArgumentNotValidException
    // GlobalExceptionHandler lo captura y retorna HTTP 400
}
```

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Errores de validación en los campos",
    "path": "/api/auth/register",
    "fieldErrors": [
        {
            "field": "email",
            "message": "must be a well-formed email address",
            "rejectedValue": "invalido"
        },
        {
            "field": "password",
            "message": "must not be blank",
            "rejectedValue": ""
        }
    ]
}
```

---

### 2️⃣ **401 Unauthorized - No Autorizado**

**Casos:**
- ❌ Credenciales inválidas (email/password incorrectos)
- ❌ Token JWT ausente o inválido
- ❌ Sesión expirada

**Ejemplo de uso:**
```java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    var opt = userService.findByEmail(req.getEmail());
    if (opt.isEmpty()) {
        throw new InvalidCredentialsException("Credenciales inválidas");
    }
    // ...
}
```

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Credenciales inválidas",
    "path": "/api/auth/login"
}
```

---

### 3️⃣ **403 Forbidden - Acceso Denegado**

**Casos:**
- ❌ Usuario autenticado pero sin permisos suficientes
- ❌ Intentando acceder a recursos restringidos

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 403,
    "error": "Forbidden",
    "message": "Acceso denegado",
    "path": "/api/crypto/admin"
}
```

---

### 4️⃣ **404 Not Found - Recurso No Encontrado**

**Casos:**
- ❌ Cryptocurrency no existe en la base de datos
- ❌ Usuario no encontrado
- ❌ ID inválido

**Ejemplo de uso:**
```java
@GetMapping("/{coinId}")
public Mono<ResponseEntity<CryptoCurrency>> getById(@PathVariable String coinId) {
    return service.findByCoinId(coinId)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                    "Cryptocurrency", "coinId", coinId)));
}
```

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "Cryptocurrency no encontrado con coinId: 'bitcoin-no-existe'",
    "path": "/api/crypto/bitcoin-no-existe"
}
```

---

### 5️⃣ **409 Conflict - Conflicto de Datos**

**Casos:**
- ❌ Email duplicado al registrar usuario
- ❌ Username duplicado
- ❌ Violación de constraint único

**Ejemplo de uso:**
```java
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    if (userService.findByEmail(req.getEmail()).isPresent()) {
        throw new ConflictException("El email '" + req.getEmail() + "' ya está registrado");
    }
    // ...
}
```

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "El email 'test@crypto.com' ya está registrado",
    "path": "/api/auth/register"
}
```

---

### 6️⃣ **500 Internal Server Error - Error Interno**

**Casos:**
- ❌ Excepción no esperada
- ❌ Error de base de datos
- ❌ Null Pointer Exception, etc.

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Error interno del servidor",
    "path": "/api/crypto/stats"
}
```

---

### 7️⃣ **502 Bad Gateway - Error en API Externa**

**Casos:**
- ❌ CoinGecko API no responde
- ❌ Timeout en llamada externa
- ❌ Respuesta inválida de servicio externo

**Ejemplo de uso:**
```java
@PostMapping("/sync")
public Mono<ResponseEntity<Map<String, Object>>> sync() {
    return service.syncFromRemoteReactive()
            .map(count -> ResponseEntity.ok(Map.of("status", "OK", "synced", count)))
            .onErrorResume(e -> Mono.error(new ExternalApiException(
                    "Error al sincronizar con CoinGecko: " + e.getMessage(), e)));
}
```

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 502,
    "error": "Bad Gateway",
    "message": "Error al comunicarse con el servicio externo: Connection timeout",
    "path": "/api/crypto/sync"
}
```

---

### 8️⃣ **503 Service Unavailable - Servicio No Disponible**

**Casos:**
- ❌ Base de datos vacía (no hay cryptos sincronizadas)
- ❌ Servicio en mantenimiento
- ❌ Recursos temporalmente no disponibles

**Respuesta JSON:**
```json
{
    "timestamp": "2025-10-22T19:30:00",
    "status": 503,
    "error": "Service Unavailable",
    "message": "No hay datos disponibles. Ejecute la sincronización primero.",
    "path": "/api/crypto/list"
}
```

---

## 📊 Ventajas del Sistema Implementado

### ✅ **1. Consistencia**
- Todas las respuestas de error tienen el **mismo formato JSON**
- Facilita el manejo de errores en el cliente (frontend/mobile)

### ✅ **2. Información Completa**
- **timestamp**: Momento exacto del error
- **status**: Código HTTP numérico
- **error**: Nombre del error HTTP
- **message**: Descripción del problema
- **path**: Endpoint donde ocurrió
- **fieldErrors**: Detalles de validación (cuando aplica)

### ✅ **3. Logging Apropiado**
- Errores de cliente (4xx): `log.warn()`
- Errores de servidor (5xx): `log.error()` con stack trace
- Facilita debugging y monitoreo

### ✅ **4. Seguridad**
- No expone stack traces al cliente
- Mensajes de error informativos pero seguros
- Diferencia entre errores de autenticación y otros

### ✅ **5. Mantenibilidad**
- Centralizado en `GlobalExceptionHandler`
- Fácil agregar nuevos tipos de error
- Excepciones personalizadas reutilizables

---

## 🧪 Ejemplos de Pruebas

### Test 1: Validación de Campos
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"","email":"invalido","password":""}'
```
**Resultado:** HTTP 400 con lista de fieldErrors

---

### Test 2: Email Duplicado
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"test",
    "firstName":"Test",
    "lastName":"User",
    "email":"existente@crypto.com",
    "password":"pass123"
  }'
```
**Resultado:** HTTP 409 Conflict

---

### Test 3: Credenciales Inválidas
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"fake@test.com","password":"wrong"}'
```
**Resultado:** HTTP 401 Unauthorized

---

### Test 4: Recurso No Encontrado
```bash
TOKEN="<jwt_token_valido>"
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8092/api/crypto/moneda-inexistente
```
**Resultado:** HTTP 404 Not Found

---

### Test 5: Acceso Sin JWT
```bash
curl http://localhost:8092/api/crypto/stats
```
**Resultado:** HTTP 401 Unauthorized

---

## 📁 Estructura de Archivos Creados

```
microServices/
├── auth-microServices/
│   └── src/main/java/.../auth_microServices/
│       ├── dto/
│       │   └── ErrorResponse.java          ✅ DTO de respuesta de error
│       └── exception/
│           ├── GlobalExceptionHandler.java ✅ Manejador global @RestControllerAdvice
│           ├── ConflictException.java      ✅ Excepción personalizada (409)
│           ├── InvalidCredentialsException.java ✅ Excepción personalizada (401)
│           └── ResourceNotFoundException.java   ✅ Excepción personalizada (404)
│
└── crypto-collector-micro/
    └── src/main/java/.../crypto_collector_micro/
        ├── dto/
        │   └── ErrorResponse.java          ✅ DTO de respuesta de error
        └── exception/
            ├── GlobalExceptionHandler.java ✅ Manejador global @RestControllerAdvice
            ├── ExternalApiException.java   ✅ Excepción personalizada (502)
            ├── NoDataAvailableException.java ✅ Excepción personalizada (503)
            └── ResourceNotFoundException.java ✅ Excepción personalizada (404)
```

---

## 🎓 Conclusión

El sistema de manejo global de errores implementado proporciona:

- ✅ **Respuestas consistentes y estructuradas**
- ✅ **Separación de responsabilidades** (controllers vs exception handling)
- ✅ **Facilidad de mantenimiento y extensión**
- ✅ **Mejor experiencia de debugging**
- ✅ **Seguridad mejorada** (sin exposición de detalles internos)
- ✅ **Logging apropiado** para monitoreo y análisis

Este enfoque es considerado **best practice** en aplicaciones Spring Boot modernas y facilita enormemente la integración con frontend y debugging de problemas en producción.

---

**Documentación actualizada:** 22 de octubre de 2025  
**Versión:** 1.0
