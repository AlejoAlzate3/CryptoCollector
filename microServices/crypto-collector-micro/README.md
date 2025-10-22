# Crypto Collector Microservice

Microservicio para recolectar y almacenar datos de criptomonedas desde la API pública de CoinGecko.

## Descripción

Este microservicio forma parte de la arquitectura de **CryptoCollector** y es responsable de:

- 🔄 Sincronizar datos de hasta 1000 criptomonedas desde CoinGecko API
- 💾 Almacenar información actualizada de mercado (precio, capitalización, volumen, etc.)
- 📊 Proporcionar endpoints para consultar los datos almacenados
- ⏰ Sincronización automática cada 6 horas

## Tecnologías

- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Cloud 2025.0.0** (Config Client, Eureka Client)
- **Spring WebFlux** (para llamadas reactivas a CoinGecko)
- **Spring Data JPA** (con PostgreSQL)
- **Liquibase** (migración de base de datos)
- **Docker** (contenedorización)

## Arquitectura

El microservicio sigue una arquitectura reactiva sobre JPA:
- Usa **WebClient** para consumir la API de CoinGecko de forma no bloqueante
- Envuelve operaciones JPA en **Mono/Flux** ejecutados en `boundedElastic` scheduler
- Implementa patrón **upsert** (insert or update) para evitar duplicados

## Variables de Entorno

| Variable | Descripción | Valor por Defecto | Requerida |
|----------|-------------|-------------------|-----------|
| `CONFIG_SERVER_URI` | URL del Config Server | `http://localhost:8888` | ✅ |
| `SPRING_DATASOURCE_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/crypto_collector_db` | ✅ |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL | `postgres` | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de PostgreSQL | - | ✅ |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | URL del servidor Eureka | `http://localhost:8761/eureka/` | ✅ |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring activo | `dev` | ⚠️ |

### Notas Importantes

- ⚠️ **No se requiere API Key** para CoinGecko (usa API pública)
- ⚠️ La API pública tiene **rate limiting** (50 llamadas/minuto aprox.)
- ⚠️ El scheduler sincroniza cada 6 horas para evitar limits
- ⚠️ La configuración de seguridad actual es **solo para desarrollo** (acceso sin autenticación)

## Endpoints

### POST /api/crypto/sync

Sincroniza manualmente hasta 1000 criptomonedas desde CoinGecko.

**Request:**
```bash
curl -X POST http://localhost:8092/api/crypto/sync
```

**Response (200 OK):**
```json
{
  "status": "OK",
  "synced": 1000
}
```

**Response (500 Error):**
```json
{
  "error": "429 Too Many Requests from GET https://api.coingecko.com/api/v3/coins/markets"
}
```

---

### GET /api/crypto/list

Lista criptomonedas con paginación, búsqueda y ordenamiento.

**Parámetros de Query:**
- `query` (opcional): Búsqueda por nombre o símbolo (case insensitive)
- `page` (opcional, default: 0): Número de página
- `size` (opcional, default: 20, max: 100): Tamaño de página
- `sortBy` (opcional, default: marketCapRank): Campo de ordenamiento
  - Opciones: `marketCapRank`, `currentPrice`, `marketCap`, `totalVolume`, `name`, `symbol`
- `dir` (opcional, default: asc): Dirección de ordenamiento (`asc` o `desc`)

**Ejemplos:**

```bash
# Listar primeras 20 criptos ordenadas por ranking
curl "http://localhost:8092/api/crypto/list"

# Buscar "bitcoin" con paginación
curl "http://localhost:8092/api/crypto/list?query=bitcoin&page=0&size=10"

# Top 50 por capitalización de mercado (descendente)
curl "http://localhost:8092/api/crypto/list?size=50&sortBy=marketCap&dir=desc"

# Buscar por símbolo "eth"
curl "http://localhost:8092/api/crypto/list?query=eth"
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "coinId": "bitcoin",
      "name": "Bitcoin",
      "symbol": "btc",
      "marketCapRank": 1,
      "currentPrice": 67500.50,
      "marketCap": 1320000000000,
      "totalVolume": 28000000000,
      "lastUpdated": "2025-10-21T15:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalPages": 50,
  "totalElements": 1000,
  "last": false,
  "size": 20,
  "number": 0,
  "first": true,
  "numberOfElements": 20,
  "empty": false
}
```

---

### GET /api/crypto/{coinId}

Obtiene los detalles de una criptomoneda específica por su `coinId` de CoinGecko.

**Parámetros de Path:**
- `coinId`: Identificador de CoinGecko (ej: `bitcoin`, `ethereum`, `cardano`)

**Ejemplos:**

```bash
# Obtener detalles de Bitcoin
curl "http://localhost:8092/api/crypto/bitcoin"

# Obtener detalles de Ethereum
curl "http://localhost:8092/api/crypto/ethereum"
```

**Response (200 OK):**
```json
{
  "id": 1,
  "coinId": "bitcoin",
  "name": "Bitcoin",
  "symbol": "btc",
  "marketCapRank": 1,
  "currentPrice": 67500.50,
  "marketCap": 1320000000000,
  "totalVolume": 28000000000,
  "lastUpdated": "2025-10-21T15:30:00Z"
}
```

**Response (404 Not Found):**
```
(Sin cuerpo - cabecera HTTP 404)
```

---

## Base de Datos

### Configuración

- **Motor:** PostgreSQL 17
- **Base de datos:** `crypto_collector_db`
- **Tabla principal:** `cryptocurrency`

### Esquema de Tabla

```sql
CREATE TABLE cryptocurrency (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    coin_id VARCHAR(128) UNIQUE NOT NULL,
    name VARCHAR(255),
    symbol VARCHAR(50),
    market_cap_rank INTEGER,
    current_price DOUBLE PRECISION,
    market_cap DOUBLE PRECISION,
    total_volume DOUBLE PRECISION,
    last_updated TIMESTAMP WITH TIME ZONE
);
```

### Migración con Liquibase

Las migraciones se gestionan automáticamente con Liquibase:
- **Changelog:** `src/main/resources/db/changelog/db.changelog-master.yaml`
- Se ejecuta al iniciar la aplicación

---

## Ejecutar Localmente

### Prerequisitos

- Java 21 JDK
- Maven 3.9+
- PostgreSQL 17 (con base de datos `crypto_collector_db` creada)
- Config Server corriendo en puerto 8888
- Eureka Server corriendo en puerto 8761

### Pasos

1. **Clonar el repositorio:**
```bash
git clone <repository-url>
cd Crypto/microServices/crypto-collector-micro
```

2. **Compilar el proyecto:**
```bash
mvn clean package -DskipTests
```

3. **Configurar variables de entorno:**
```bash
export CONFIG_SERVER_URI=http://localhost:8888
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/crypto_collector_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=tu_password
export EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
export SPRING_PROFILES_ACTIVE=dev
```

4. **Ejecutar la aplicación:**
```bash
java -jar target/crypto-collector-micro-0.0.1-SNAPSHOT.jar
```

5. **Verificar en Eureka:**
```
http://localhost:8761
```

---

## Ejecutar con Docker

### Usando Docker Compose (Recomendado)

1. **Desde la raíz del proyecto:**
```bash
cd Crypto
```

2. **Compilar el JAR:**
```bash
mvn -f microServices/crypto-collector-micro/pom.xml clean package -DskipTests
```

3. **Levantar todos los servicios:**
```bash
docker compose up -d
```

4. **Verificar logs:**
```bash
docker logs crypto-crypto-collector-micro-1 -f
```

5. **Verificar salud:**
```bash
curl http://localhost:8092/actuator/health
```

### Construir imagen manualmente

```bash
cd microServices/crypto-collector-micro
docker build -t cryptocollector/crypto-collector-micro:latest .
```

---

## Sincronización Automática

El microservicio tiene configurado un **scheduler** que ejecuta sincronización automática:

- **Frecuencia:** Cada 6 horas (00:00, 06:00, 12:00, 18:00)
- **Expresión Cron:** `0 0 */6 * * *`
- **Clase:** `CryptoSyncScheduler`

### Logs del Scheduler

```
=== Iniciando sincronizacion automatica de criptomonedas a las 2025-10-21 06:00:00 ===
Sincronizacion completada exitosamente a las 2025-10-21 06:02:35
  -> Total sincronizado: 1000 criptomonedas
```

### Desactivar Scheduler

Para desarrollo, puedes comentar la anotación `@EnableScheduling` en `SchedulerConfig.java`.

---

## Estructura del Proyecto

```
crypto-collector-micro/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cryptoCollector/microServices/crypto_collector_micro/
│   │   │       ├── CryptoCollectorMicroApplication.java
│   │   │       ├── config/
│   │   │       │   ├── ReactiveConfig.java
│   │   │       │   ├── SchedulerConfig.java
│   │   │       │   └── SecurityConfig.java
│   │   │       ├── controller/
│   │   │       │   └── CryptoController.java
│   │   │       ├── dto/
│   │   │       │   └── CoinGeckoCoin.java
│   │   │       ├── model/
│   │   │       │   └── CryptoCurrency.java
│   │   │       ├── repository/
│   │   │       │   └── CryptoRepository.java
│   │   │       ├── scheduler/
│   │   │       │   └── CryptoSyncScheduler.java
│   │   │       └── service/
│   │   │           ├── CryptoFetchService.java
│   │   │           └── CryptoService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── changelog/
│   │               └── db.changelog-master.yaml
│   └── test/
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Troubleshooting

### Error 429 Too Many Requests

**Problema:** CoinGecko API bloquea por rate limiting.

**Solución:**
- Esperar 1-2 minutos antes de reintentar
- El scheduler automático ya tiene delay de 6 horas
- Considerar usar API key de CoinGecko Pro (no implementado)

### Servicio no se registra en Eureka

**Verificar:**
1. Config Server está corriendo y accesible
2. Eureka Server está corriendo en puerto 8761
3. Variable `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` correcta
4. Revisar logs: `docker logs crypto-crypto-collector-micro-1`

### Error de conexión a PostgreSQL

**Verificar:**
1. Base de datos `crypto_collector_db` existe
2. Credenciales correctas en variables de entorno
3. PostgreSQL acepta conexiones desde Docker (si aplica)

```bash
# Crear base de datos manualmente si no existe
docker exec crypto-postgres-1 psql -U postgres -c "CREATE DATABASE crypto_collector_db;"
```

---

## Desarrollo y Contribución

### Ejecutar tests

```bash
mvn test
```

### Formato de código

El proyecto usa convenciones de Spring Boot:
- Indentación: 4 espacios
- Encoding: UTF-8
- Line ending: LF

### Próximas mejoras

- [ ] Implementar cache con Redis
- [ ] Añadir métricas con Micrometer/Prometheus
- [ ] Implementar circuit breaker con Resilience4j
- [ ] Añadir autenticación JWT
- [ ] Implementar API key de CoinGecko Pro
- [ ] WebSocket para datos en tiempo real

---

## Licencia

Este proyecto es parte del sistema CryptoCollector.

---

## Contacto

Para reportar problemas o sugerencias, crear un issue en el repositorio.
