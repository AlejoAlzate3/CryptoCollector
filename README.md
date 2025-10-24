# CryptoCollector - Plataforma de Recolección y Gestión de Criptomonedas

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)

Sistema de microservicios para recolectar, almacenar y gestionar información actualizada de criptomonedas desde CoinGecko API, con autenticación JWT y arquitectura reactiva.

---

## 📋 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Arquitectura](#-arquitectura)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación y Ejecución](#-instalación-y-ejecución)
- [Endpoints y Ejemplos](#-endpoints-y-ejemplos)
- [Configuración](#-configuración)
- [Seguridad](#-seguridad)
- [Rendimiento y Optimización](#-rendimiento-y-optimización)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)

---

## 🎯 Descripción General

**CryptoCollector** es una plataforma basada en microservicios que:

- 🔐 **Autenticación segura** con JWT (JSON Web Tokens)
- 🔄 **Sincronización automática** de 1,030+ criptomonedas cada 6 horas
- 💾 **Almacenamiento persistente** en PostgreSQL
- ⚡ **Caché distribuido** con Redis
- 📊 **API REST reactiva** con Spring WebFlux
- 🎯 **Documentación interactiva** con Swagger UI
- 🔍 **Búsqueda y paginación** avanzada de criptomonedas
- 🐳 **Despliegue containerizado** con Docker Compose

---

## 🏗️ Arquitectura

### Microservicios

```
┌─────────────────┐
│   API Gateway   │ :8080
│  (Spring Cloud) │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼───────┐
│ Auth  │ │  Crypto  │
│:8081  │ │  :8092   │
└───────┘ └──────────┘
    │         │
    └────┬────┘
         │
┌────────▼─────────┐
│   PostgreSQL     │ :5432
│  2 Databases     │
│  - cryptousers   │
│  - crypto_db     │
└──────────────────┘
```

### Servicios de Infraestructura

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| **Config Server** | 8888 | Servidor de configuración centralizada (Spring Cloud Config) |
| **Discovery Server** | 8761 | Registro y descubrimiento de servicios (Eureka) |
| **API Gateway** | 8080 | Punto de entrada único, enrutamiento y seguridad |
| **Redis** | 6379 | Caché distribuido para optimizar consultas |

### Microservicios de Negocio

| Servicio | Puerto | Base de Datos | Descripción |
|----------|--------|---------------|-------------|
| **Auth Service** | 8081 | `cryptousers` | Registro, login, gestión JWT |
| **Crypto Service** | 8092 | `crypto_collector_db` | Sincronización y consulta de criptomonedas |

---

## 📦 Requisitos Previos

### Software Necesario

- **Java 21** o superior ([OpenJDK](https://openjdk.org/) o [Oracle JDK](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **Docker 24+** y **Docker Compose** ([Install Docker](https://docs.docker.com/get-docker/))
- **Git** ([Download](https://git-scm.com/downloads))

### Verificar Instalación

```bash
java -version    # Java 21+
mvn -version     # Maven 3.8+
docker --version # Docker 24+
docker compose version
```

---

## 🚀 Instalación y Ejecución

### 1. Clonar el Repositorio

```bash
git clone https://github.com/AlejoAlzate3/CryptoCollector.git
cd CryptoCollector
```

### 2. Configurar Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto:

```bash
# .env
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=cryptousers

JWT_SECRET=your_jwt_secret_minimum_256_bits
JWT_EXPIRATION=3600000

CONFIG_SERVER_URI=http://config-server:8888
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://discovery-server:8761/eureka/

SPRING_PROFILES_ACTIVE=dev
```

⚠️ **Importante:** 
- No comitees el archivo `.env` (ya está en `.gitignore`)
- En producción, usa variables de entorno del sistema o un gestor de secretos (Vault, AWS Secrets Manager)

### 3. Compilar el Proyecto

```bash
mvn clean package -DskipTests
```

### 4. Levantar los Servicios con Docker Compose

```bash
docker-compose up -d
```

Espera 40-60 segundos para que todos los servicios inicien y se registren en Eureka.

### 5. Verificar el Estado

```bash
# Verificar contenedores
docker-compose ps

# Ver logs de un servicio específico
docker logs crypto-auth-service-1 -f
docker logs crypto-crypto-collector-micro-1 -f

# Verificar salud de los servicios
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8761                   # Eureka Dashboard
```

### 6. Acceder a la Aplicación

⚠️ **IMPORTANTE:** Todos los endpoints de la aplicación deben accederse a través del **API Gateway** (puerto 8080). Los puertos de los microservicios individuales (8081, 8092) solo deben usarse para debugging en desarrollo.

- **API Gateway (Punto de entrada único):** http://localhost:8080
- **Eureka Dashboard:** http://localhost:8761
- **Swagger UI (Auth - solo dev):** http://localhost:8081/swagger-ui/index.html
- **Swagger UI (Crypto - solo dev):** http://localhost:8092/swagger-ui/index.html

#### Rutas del API Gateway

| Ruta en Gateway | Microservicio Destino | Puerto Real |
|-----------------|----------------------|-------------|
| `http://localhost:8080/api/auth/**` | Auth Service | 8081 |
| `http://localhost:8080/api/crypto/**` | Crypto Service | 8092 |

---

## 📡 Endpoints y Ejemplos

⚠️ **IMPORTANTE:** Todos los endpoints deben accederse a través del **API Gateway** en el puerto **8080**. El gateway se encarga del enrutamiento, seguridad y validación JWT.

### 🔐 Auth Service

Acceso a través de: `http://localhost:8080/api/auth/**`

#### 1. Registrar Usuario

**Endpoint:** `POST /api/auth/register`

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

**Respuesta (201 Created):**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com"
}
```

#### 2. Login

**Endpoint:** `POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

**Respuesta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 💰 Crypto Service

Acceso a través de: `http://localhost:8080/api/crypto/**`

> **Nota:** Todos los endpoints requieren autenticación JWT. El token debe incluirse en el header `Authorization: Bearer <token>`

#### 1. Estadísticas Generales

**Endpoint:** `GET /api/crypto/stats`

```bash
TOKEN="tu_token_jwt_aqui"

curl http://localhost:8080/api/crypto/stats \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta:**
```json
{
  "total": 1030,
  "hasSyncedData": true,
  "lastUpdated": "2025-10-24T00:00:00Z"
}
```

#### 2. Listar Criptomonedas (Paginado)

**Endpoint:** `GET /api/crypto/list`

**Parámetros de Query:**
- `query` (opcional): Búsqueda por nombre o símbolo
- `page` (opcional, default: 0): Número de página
- `size` (opcional, default: 20, max: 100): Tamaño de página
- `sortBy` (opcional, default: marketCapRank): Campo de ordenamiento
  - Opciones: `marketCapRank`, `currentPrice`, `marketCap`, `totalVolume`, `name`, `symbol`
- `dir` (opcional, default: asc): Dirección (`asc` o `desc`)

**Ejemplos:**

```bash
# Top 5 por ranking de mercado
curl "http://localhost:8080/api/crypto/list?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN"

# Buscar "bitcoin"
curl "http://localhost:8080/api/crypto/list?query=bitcoin" \
  -H "Authorization: Bearer $TOKEN"

# Top 10 por capitalización (descendente)
curl "http://localhost:8080/api/crypto/list?size=10&sortBy=marketCap&dir=desc" \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta:**
```json
{
  "content": [
    {
      "coinId": "bitcoin",
      "symbol": "btc",
      "name": "Bitcoin",
      "currentPrice": 110067.42,
      "marketCap": 2179234567890,
      "marketCapRank": 1,
      "totalVolume": 45678901234,
      "high24h": 112000.00,
      "low24h": 108500.00,
      "priceChange24h": 1567.42,
      "priceChangePercentage24h": 1.44
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 5
  },
  "totalElements": 1030,
  "totalPages": 206
}
```

#### 3. Buscar por Coin ID

**Endpoint:** `GET /api/crypto/{coinId}`

```bash
curl http://localhost:8080/api/crypto/bitcoin \
  -H "Authorization: Bearer $TOKEN"
```

#### 4. Estado del Scheduler

**Endpoint:** `GET /api/crypto/scheduler/status`

```bash
curl http://localhost:8080/api/crypto/scheduler/status \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta:**
```json
{
  "isRunning": true,
  "lastExecutionTime": "2025-10-24T00:00:00Z",
  "nextExecutionTime": "2025-10-24T06:00:00Z",
  "intervalHours": 6,
  "totalCryptocurrencies": 1030
}
```

#### 5. Sincronización Manual

**Endpoint:** `POST /api/crypto/sync`

```bash
curl -X POST http://localhost:8080/api/crypto/sync \
  -H "Authorization: Bearer $TOKEN"
```

⚠️ **Rate Limiting:** La API de CoinGecko tiene límite de ~50 llamadas/minuto. Usa con precaución.

---

### 🔍 Acceso Directo a Microservicios (Solo para Desarrollo)

En entorno de desarrollo, puedes acceder directamente a los microservicios:

```bash
# Auth Service directo (sin JWT validation en gateway)
curl http://localhost:8081/api/auth/register ...

# Crypto Service directo (sin JWT validation en gateway)
curl http://localhost:8092/api/crypto/stats ...
```

⚠️ **NO recomendado para producción.** En producción, el API Gateway debe ser el único punto de entrada y los puertos de microservicios deberían estar protegidos por firewall.

---

## ⚙️ Configuración

### Perfiles de Spring

La aplicación soporta dos perfiles:

#### Perfil `dev` (Desarrollo)
- `spring.jpa.hibernate.ddl-auto: update` - Actualiza schema automáticamente
- `spring.jpa.show-sql: true` - Muestra SQL en logs
- `logging.level.root: INFO` - Logging verbose
- Swagger UI habilitado
- Validaciones de seguridad relajadas

#### Perfil `prod` (Producción)
- `spring.jpa.hibernate.ddl-auto: validate` - Solo valida schema
- `spring.jpa.show-sql: false` - Sin SQL en logs
- `logging.level.root: WARN` - Logging mínimo
- Swagger UI deshabilitado
- Seguridad completa habilitada

### Cambiar de Perfil

```bash
# En Docker Compose, edita .env
SPRING_PROFILES_ACTIVE=prod

# Reinicia los servicios
docker-compose restart auth-service crypto-collector-micro
```

### Configuración de Base de Datos

Las configuraciones se gestionan centralizadamente en **Config Server**:

```yaml
# configServer/src/main/resources/config/auth-microServices-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

---

## 🔒 Seguridad

### JWT (JSON Web Tokens)

#### Generación de Token
- Algoritmo: **HS256** (HMAC con SHA-256)
- Expiración: **1 hora** (configurable vía `JWT_EXPIRATION`)
- Claims incluidos: `sub` (email), `iat` (issued at), `exp` (expiration)

#### Validación
- Verificación de firma con `JWT_SECRET`
- Validación de expiración
- Extracción de usuario desde claim `sub`

#### Almacenamiento en Cliente
```javascript
// ✅ Recomendado: HTTP-Only Cookie
document.cookie = `token=${jwt}; HttpOnly; Secure; SameSite=Strict`;

// ⚠️ Alternativa: LocalStorage (vulnerable a XSS)
localStorage.setItem('token', jwt);
```

### Protección de Endpoints

El **API Gateway** actúa como único punto de entrada y aplica las siguientes reglas de seguridad:

#### Sin Autenticación (Públicos)
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /actuator/health` (servicios de infraestructura)

#### Con Autenticación JWT (Protegidos)
- Todos los endpoints de `/api/crypto/**`
- El filtro `JwtAuthenticationFilter` en el Gateway valida el token antes de enrutar
- Si el token es inválido o está ausente: **401 Unauthorized**
- Si el token es válido: request enrutado al microservicio correspondiente

### Seguridad en Producción

```yaml
# Configuración recomendada para prod
jwt:
  secret: ${JWT_SECRET} # Variable de entorno (mínimo 256 bits)
  expiration: 3600000   # 1 hora

spring:
  security:
    csrf:
      enabled: true
    headers:
      frame-options: DENY
      content-security-policy: "default-src 'self'"
```

#### Checklist de Seguridad
- [ ] `JWT_SECRET` generado con algoritmo criptográficamente seguro (min 256 bits)
- [ ] Variables de entorno gestionadas con gestor de secretos (Vault, AWS Secrets Manager)
- [ ] HTTPS habilitado en producción
- [ ] CORS configurado con orígenes permitidos explícitos
- [ ] Rate limiting habilitado en API Gateway
- [ ] Logs de auditoría para acciones sensibles
- [ ] Contraseñas hasheadas con BCrypt (factor 12)
- [ ] Validación de entrada en todos los endpoints

---

## ⚡ Rendimiento y Optimización

### Redis Cache

#### Estrategia de Caché
- **Key Pattern:** `crypto:list:{page}:{size}:{sortBy}:{dir}:{query}`
- **TTL:** 5 minutos
- **Invalidación:** Tras cada sincronización manual o automática

#### Configuración
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

#### Métricas de Rendimiento

| Escenario | Sin Caché | Con Caché | Mejora |
|-----------|-----------|-----------|--------|
| Lista 20 items | 180ms | 12ms | **93.3%** |
| Búsqueda por query | 250ms | 15ms | **94.0%** |
| Stats endpoint | 45ms | 8ms | **82.2%** |

### Optimizaciones de Base de Datos

#### Índices Creados
```sql
CREATE INDEX idx_cryptocurrency_market_cap_rank 
  ON cryptocurrency(market_cap_rank);

CREATE INDEX idx_cryptocurrency_name 
  ON cryptocurrency(name);

CREATE INDEX idx_cryptocurrency_symbol 
  ON cryptocurrency(symbol);

CREATE INDEX idx_users_email 
  ON users(email);
```

#### Paginación
- Implementada con `Pageable` de Spring Data
- Límite máximo: 100 items por página
- Ordenamiento configurable por múltiples campos

### Rate Limiting (CoinGecko API)

```java
// Implementación en CryptoService
@Scheduled(cron = "0 0 0,6,12,18 * * *") // Cada 6 horas
public void scheduledSync() {
    // Sincroniza máximo 1000 cryptos con delay entre llamadas
    webClient.get()
        .uri(uriBuilder -> uriBuilder
            .queryParam("per_page", 250) // 4 llamadas total
            .build())
        .retrieve()
        .bodyToFlux(CryptoDTO.class)
        .delayElements(Duration.ofMillis(200)) // 5 req/seg
        .collectList()
        .subscribe(this::saveCryptos);
}
```

### Monitoreo

```bash
# Ver métricas de Redis
docker exec crypto-redis-1 redis-cli INFO stats

# Monitorear conexiones de PostgreSQL
docker exec crypto-postgres-1 psql -U postgres -d crypto_collector_db \
  -c "SELECT COUNT(*) FROM pg_stat_activity;"

# Logs de rendimiento
docker logs crypto-crypto-collector-micro-1 | grep "Execution time"
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests de un microservicio específico
mvn test -pl microServices/auth-microServices
mvn test -pl microServices/crypto-collector-micro

# Tests con cobertura
mvn test jacoco:report
```

### Cobertura de Tests

| Módulo | Tests | Cobertura | Estado |
|--------|-------|-----------|--------|
| **Auth Service** | 26 | 92% | ✅ Passing |
| **Crypto Service** | 23 | 100% | ✅ Passing |
| **Total** | **49** | **95%** | **✅ Passing** |

#### Tipos de Tests

**Unit Tests:**
- `AuthServiceTest.java` (17 tests)
- `CryptoServiceTest.java` (12 tests)
- `CryptoMapperTest.java` (10 tests)
- `JwtUtilTest.java` (8 tests)

**Integration Tests:**
- `AuthIntegrationTest.java` (9 tests)
- Base de datos H2 en memoria
- Profiles de test con configuración aislada

### Ejemplo de Test

```java
@Test
@DisplayName("Register user successfully")
void testRegisterUser_Success() {
    // Given
    RegisterRequestDTO request = new RegisterRequestDTO(
        "testuser", 
        "test@example.com", 
        "password123"
    );
    
    // When
    Mono<User> result = authService.registerUser(request);
    
    // Then
    StepVerifier.create(result)
        .assertNext(user -> {
            assertNotNull(user.getId());
            assertEquals("testuser", user.getUsername());
            assertEquals("test@example.com", user.getEmail());
        })
        .verifyComplete();
}
```

---

## 🔧 Troubleshooting

### Problemas Comunes

#### 1. Servicios no se registran en Eureka

**Síntoma:** El dashboard de Eureka (localhost:8761) muestra 0 instancias

**Solución:**
```bash
# Verificar que Config Server esté saludable
curl http://localhost:8888/actuator/health

# Verificar logs de Discovery Server
docker logs crypto-discovery-server-1

# Reiniciar servicios en orden
docker-compose restart config-server
sleep 10
docker-compose restart discovery-server
sleep 15
docker-compose restart auth-service crypto-collector-micro
```

#### 2. Error "Connection refused" a PostgreSQL

**Síntoma:** `org.postgresql.util.PSQLException: Connection refused`

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Verificar healthcheck
docker inspect crypto-postgres-1 | grep -A5 Health

# Reiniciar PostgreSQL
docker-compose restart postgres
sleep 10
docker-compose restart auth-service crypto-collector-micro
```

#### 3. Token JWT inválido o expirado

**Síntoma:** `401 Unauthorized` en endpoints protegidos

**Solución:**
```bash
# Generar nuevo token a través del API Gateway
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Usar el nuevo token en los headers
TOKEN="nuevo_token_aqui"
curl http://localhost:8080/api/crypto/stats \
  -H "Authorization: Bearer $TOKEN"
```

#### 4. Sincronización falla con "429 Too Many Requests"

**Síntoma:** Error al sincronizar criptomonedas desde CoinGecko

**Solución:**
```bash
# Esperar 1 minuto antes de reintentar
sleep 60

# Verificar estado del scheduler (a través del gateway)
curl http://localhost:8080/api/crypto/scheduler/status \
  -H "Authorization: Bearer $TOKEN"

# El scheduler se ejecuta automáticamente cada 6 horas
# Evita llamar manualmente POST /api/crypto/sync muy frecuentemente
```

#### 5. Redis no guarda caché

**Síntoma:** Todas las peticiones van a la base de datos

**Solución:**
```bash
# Verificar conexión a Redis
docker exec crypto-redis-1 redis-cli PING
# Debe responder: PONG

# Verificar claves en Redis
docker exec crypto-redis-1 redis-cli KEYS "crypto:*"

# Limpiar caché si hay problemas
docker exec crypto-redis-1 redis-cli FLUSHDB
```

### Logs Útiles

```bash
# Ver logs en tiempo real de todos los servicios
docker-compose logs -f

# Logs de un servicio específico
docker logs crypto-auth-service-1 -f --tail 100
docker logs crypto-crypto-collector-micro-1 -f --tail 100

# Buscar errores en logs
docker logs crypto-auth-service-1 2>&1 | grep ERROR
docker logs crypto-crypto-collector-micro-1 2>&1 | grep ERROR

# Ver queries SQL (solo en perfil dev)
docker logs crypto-crypto-collector-micro-1 | grep "Hibernate:"
```

### Reinicio Completo

```bash
# Parar todos los servicios
docker-compose down

# Limpiar volúmenes (⚠️ elimina datos)
docker-compose down -v

# Reconstruir imágenes
docker-compose build --no-cache

# Levantar todo de nuevo
docker-compose up -d

# Esperar a que todo esté saludable
sleep 60
docker-compose ps
```

---

### 📚 Documentación Adicional

### Swagger UI (Solo en Dev)

- **Auth Service:** http://localhost:8081/swagger-ui/index.html
- **Crypto Service:** http://localhost:8092/swagger-ui/index.html

> **Nota:** Swagger UI está disponible solo en perfil `dev` y debe accederse directamente al puerto del microservicio (no a través del Gateway).

### Arquitectura Detallada

- **Spring Cloud Gateway:** [Documentación oficial](https://spring.io/projects/spring-cloud-gateway)
- **Netflix Eureka:** [Documentación oficial](https://spring.io/projects/spring-cloud-netflix)
- **Spring Security WebFlux:** [Reference](https://docs.spring.io/spring-security/reference/reactive/index.html)
- **CoinGecko API:** [Documentación API](https://docs.coingecko.com/reference/introduction)

---

## 🤝 Contribuciones

Si deseas contribuir al proyecto:

1. Fork el repositorio
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver archivo `LICENSE` para más detalles.

---

## 👨‍💻 Autor

**Alejo Alzate**
- GitHub: [@AlejoAlzate3](https://github.com/AlejoAlzate3)
- Repositorio: [CryptoCollector](https://github.com/AlejoAlzate3/CryptoCollector)

---

## 📞 Soporte

¿Problemas o preguntas? Abre un [Issue en GitHub](https://github.com/AlejoAlzate3/CryptoCollector/issues).

---

**Última actualización:** 23 de Octubre de 2025
