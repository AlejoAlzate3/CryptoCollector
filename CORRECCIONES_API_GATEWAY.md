# 🔧 Correcciones Aplicadas al API Gateway

## 📋 Problema Identificado

El usuario reportó dos problemas:
1. **Error 404** al intentar acceder a `/auth/api/auth/login` (URL incorrecta)
2. **"Failed to load remote configuration"** en Swagger UI

## 🔍 Análisis del Problema

El problema principal era que **Swagger UI no sabía dónde encontrar sus archivos de configuración** cuando está detrás del API Gateway. 

Springdoc (la librería que genera Swagger) genera automáticamente URLs relativas como `/v3/api-docs`, pero cuando el servicio está detrás del API Gateway con prefijos como `/crypto` o `/auth`, Swagger intenta cargar `/v3/api-docs` en lugar de `/crypto/v3/api-docs`.

## ✅ Soluciones Implementadas

### 1. Configuración de Headers en API Gateway

**Archivo**: `configServer/src/main/resources/config/apiGateway-dev.yml`

Se agregaron headers `X-Forwarded-Prefix` y `PreserveHostHeader` para que Springdoc sepa que está detrás de un proxy:

```yaml
# Crypto - Swagger UI
- id: crypto-swagger-ui
  uri: lb://crypto-collector-micro
  predicates:
    - Path=/crypto/swagger-ui/**
  filters:
    - RewritePath=/crypto/(?<segment>.*), /$\{segment}
    - PreserveHostHeader
    - AddRequestHeader=X-Forwarded-Prefix, /crypto

# Crypto - API Docs  
- id: crypto-api-docs
  uri: lb://crypto-collector-micro
  predicates:
    - Path=/crypto/v3/api-docs/**
  filters:
    - RewritePath=/crypto/(?<segment>.*), /$\{segment}
    - AddRequestHeader=X-Forwarded-Prefix, /crypto
```

Lo mismo para las rutas de Auth:
```yaml
# Auth - Swagger UI
- id: auth-swagger-ui
  uri: lb://auth-microservices
  predicates:
    - Path=/auth/swagger-ui/**
  filters:
    - RewritePath=/auth/(?<segment>.*), /$\{segment}
    - PreserveHostHeader
    - AddRequestHeader=X-Forwarded-Prefix, /auth
```

### 2. Configuración de Springdoc en Microservicios

**Archivos modificados**:
- `microServices/crypto-collector-micro/src/main/resources/application.yml`
- `microServices/auth-microServices/src/main/resources/application.yml`

Se agregó configuración para que Springdoc use correctamente los headers forwarded:

```yaml
# Configuración de Springdoc/Swagger para trabajar detrás del API Gateway
springdoc:
    api-docs:
        enabled: true
    swagger-ui:
        enabled: true
    use-management-port: false
    use-root-path: false
server:
    forward-headers-strategy: framework
```

**Explicación**:
- `forward-headers-strategy: framework` → Spring usa los headers `X-Forwarded-*` para generar URLs correctas
- Springdoc ahora detecta el prefijo `/crypto` o `/auth` y genera las URLs apropiadas

## 📊 Resultado

### Antes (❌ Error)
- Swagger cargaba: `/v3/api-docs` → **404 Not Found**
- configUrl: `/v3/api-docs/swagger-config` → **404 Not Found**

### Después (✅ Funcionando)
- Swagger carga: `/crypto/v3/api-docs` → **200 OK**
- configUrl: `/crypto/v3/api-docs/swagger-config` → **200 OK**

## 🧪 Validación

Todas las pruebas pasaron exitosamente:

```bash
✅ Login a través del API Gateway
✅ Stats obtenidas correctamente
✅ Bitcoin obtenido correctamente
✅ Lista de 1028 criptomonedas obtenida
✅ Endpoint público de Redis funcionando
✅ Swagger Crypto: HTTP 200
✅ Swagger Auth: HTTP 200
✅ Configuración Swagger Crypto OK
✅ Configuración Swagger Auth OK
```

## 🌐 URLs Correctas

### Login (Público)
```
POST http://localhost:8080/api/auth/login
```

### Swagger UIs
```
Crypto: http://localhost:8080/crypto/swagger-ui/index.html
Auth:   http://localhost:8080/auth/swagger-ui/index.html
```

### API Docs (JSON)
```
Crypto: http://localhost:8080/crypto/v3/api-docs
Auth:   http://localhost:8080/auth/v3/api-docs
```

## 📝 Comandos de Compilación y Despliegue

```bash
# 1. Recompilar proyecto
mvn clean package -DskipTests

# 2. Reconstruir imágenes Docker
docker build -t cryptocollector/config-server:latest configServer/
docker build -t cryptocollector/api-gateway:latest apiGateWay/
docker build -t cryptocollector/auth-service:latest microServices/auth-microServices/
docker build -t cryptocollector/crypto-collector-micro:latest microServices/crypto-collector-micro/

# 3. Reiniciar servicios
docker compose up -d --force-recreate config-server api-gateway auth-service crypto-collector-micro
```

## 🎯 Aclaración de URL

La URL que el usuario probó estaba mal:
- ❌ `{{gateway_url}}/auth/api/auth/login` → **INCORRECTA** (duplica `/auth`)
- ✅ `http://localhost:8080/api/auth/login` → **CORRECTA**

## 🔑 Conceptos Técnicos Aplicados

1. **X-Forwarded-Prefix**: Header estándar que indica el prefijo de path agregado por el proxy
2. **PreserveHostHeader**: Preserva el header `Host` original en la petición
3. **forward-headers-strategy**: Estrategia de Spring Boot para procesar headers de proxy
4. **RewritePath**: Filter de Spring Cloud Gateway para reescribir paths
5. **Springdoc context path**: Springdoc usa el contexto del servlet para generar URLs

## 📚 Referencias

- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Springdoc OpenAPI: https://springdoc.org/
- X-Forwarded Headers: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For

---

**Estado**: ✅ **RESUELTO Y FUNCIONANDO**  
**Fecha**: 23 de octubre de 2025  
**Validado con**: Usuario `alejo1@gmail.com`
