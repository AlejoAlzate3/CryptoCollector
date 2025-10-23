# 🚀 Implementación de Redis Cache - CryptoCollector

## 📋 Resumen

Se ha implementado exitosamente **Redis Cache** en el microservicio `crypto-collector-micro` para mejorar el rendimiento y reducir la carga en la base de datos PostgreSQL.

### ✅ Resultados Demostrados

- **Mejora de rendimiento**: 91% más rápido (888ms → 73ms)
- **Reducción de consultas**: De N consultas a 1 consulta por TTL
- **Estado**: ✅ **COMPLETAMENTE FUNCIONAL**

---

## 🏗️ Arquitectura

### Componentes Implementados

```
┌─────────────────────────────────────────────────────────────┐
│                    CRYPTO COLLECTOR MICRO                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Controllers  │───▶│   Service    │───▶│ Repository   │ │
│  │              │    │  @Cacheable  │    │              │ │
│  └──────────────┘    └──────┬───────┘    └──────┬───────┘ │
│                              │                    │         │
│                              │                    │         │
│                              ▼                    ▼         │
│                      ┌──────────────┐    ┌──────────────┐  │
│                      │ Redis Cache  │    │  PostgreSQL  │  │
│                      │  (256 MB)    │    │   Database   │  │
│                      └──────────────┘    └──────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Dependencias Agregadas

En `pom.xml`:

```xml
<!-- Redis Cache -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Spring Cache Abstraction -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<!-- Jedis Client -->
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

**Incremento de tamaño**: JAR de 96MB → 102MB (6MB adicionales)

---

## ⚙️ Configuración de Redis

### Docker Compose (`docker-compose.yml`)

```yaml
redis:
  image: redis:7-alpine
  container_name: crypto-redis
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
  command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - crypto-network

volumes:
  redis-data:
```

**Características**:
- **Imagen**: Redis 7 Alpine (ligera)
- **Persistencia**: AOF (Append-Only File) habilitada
- **Memoria máxima**: 256MB
- **Política de evicción**: `allkeys-lru` (Least Recently Used)
- **Healthcheck**: Ping cada 10 segundos

### Application Configuration (`application.yml`)

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      timeout: 2000ms
      jedis:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutos default
      cache-null-values: false
```

---

## 🗄️ Configuración de Cachés

### RedisConfig.java

Se configuraron **5 cachés** con diferentes TTL (Time To Live):

| Cache Name        | TTL       | Propósito                                    |
|-------------------|-----------|----------------------------------------------|
| `crypto-list`     | 5 minutos | Listas paginadas de criptomonedas            |
| `crypto-details`  | 2 minutos | Detalles individuales de una criptomoneda    |
| `crypto-stats`    | 1 minuto  | Estadísticas generales (total, última sync)  |
| `scheduler-status`| 1 minuto  | Estado del scheduler de sincronización       |
| `coingecko-api`   | 30 seg    | Respuestas de la API externa de CoinGecko   |

### Estrategia de Serialización

- **Keys**: `StringRedisSerializer` - Claves como strings simples
- **Values**: `GenericJackson2JsonRedisSerializer` - Objetos serializados en JSON

```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("crypto-list", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("crypto-details", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("crypto-stats", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("scheduler-status", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("coingecko-api", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
```

---

## 🎯 Métodos Cacheados

### CryptoService.java

#### 1. Lista de Criptomonedas

```java
@Cacheable(value = "crypto-list", 
           key = "#query + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
public Mono<Page<CryptoCurrency>> listCryptos(String query, Pageable pageable)
```

**Key Strategy**: `query-pageNumber-pageSize-sort`  
**Ejemplo**: `bitcoin-0-10-name: ASC`

#### 2. Detalles de Criptomoneda

```java
@Cacheable(value = "crypto-details", key = "#coinId")
public Mono<CryptoCurrency> findByCoinId(String coinId)
```

**Key Strategy**: `coinId`  
**Ejemplo**: `bitcoin`, `ethereum`

#### 3. Estadísticas Generales

```java
@Cacheable(value = "crypto-stats")
public Mono<Map<String, Object>> getStats()
```

**Key Strategy**: `SimpleKey []` (sin parámetros)  
**Respuesta**: `{ total: 1016, lastUpdated: "2025-10-23T00:00:00.524Z", hasSyncedData: true }`

#### 4. Estado del Scheduler

```java
@Cacheable(value = "scheduler-status")
public Mono<Map<String, Object>> getSchedulerStatus()
```

**Key Strategy**: `SimpleKey []`  
**Respuesta**: Información sobre próxima sincronización, última sync, etc.

---

## 🔄 Invalidación de Caché

### Invalidación Automática

```java
@CacheEvict(value = {"crypto-list", "crypto-details", "crypto-stats", "scheduler-status"}, 
            allEntries = true)
public Mono<Long> syncFromRemoteReactive()
```

Cuando se ejecuta una sincronización con CoinGecko API, **todos los cachés** se invalidan automáticamente para garantizar datos frescos.

### Invalidación Manual

Se creó un `CacheController` con endpoints para gestión manual:

| Método | Endpoint                          | Descripción                         |
|--------|-----------------------------------|-------------------------------------|
| GET    | `/api/cache/info`                 | Información de todos los cachés     |
| DELETE | `/api/cache/clear-all`            | Limpiar todos los cachés            |
| DELETE | `/api/cache/clear/{cacheName}`    | Limpiar un caché específico         |
| DELETE | `/api/cache/clear-lists`          | Limpiar solo `crypto-list`          |
| DELETE | `/api/cache/clear-details`        | Limpiar solo `crypto-details`       |
| POST   | `/api/cache/warmup`               | Pre-cargar caché con datos comunes  |

**Autenticación**: Requiere JWT token válido

---

## 📊 Pruebas y Resultados

### Script de Demo Completo

Se creó `demo-completa.sh` que realiza:

1. ✅ Registro de usuario
2. ✅ Login y obtención de JWT
3. ✅ Limpieza de Redis (estado inicial)
4. ✅ Primera llamada (Cache MISS) - **888ms**
5. ✅ Verificación de datos en Redis
6. ✅ Segunda llamada (Cache HIT) - **73ms**
7. ✅ Comparación de rendimiento: **91% más rápido**
8. ✅ Verificación de logs (solo 1 Cache MISS)
9. ✅ Limpieza manual de caché
10. ✅ Información de configuración

### Resultados Reales

```bash
Primera llamada (Base de Datos):  888ms  ← Consulta a PostgreSQL
Segunda llamada (Redis Cache):     73ms  ← Servida desde Redis
────────────────────────────────────────
Mejora de rendimiento: 91% más rápido
Speedup: 12x veces más rápido
```

### Evidencia en Logs

```
2025-10-23T04:33:11.770Z INFO - 💾 Cache MISS - Consultando estadísticas de BD
                                  ↑ Solo aparece UNA VEZ

(La segunda llamada NO genera log porque fue cache hit)
```

### Verificación en Redis CLI

```bash
# Ver keys cacheadas
$ docker exec crypto-redis-1 redis-cli KEYS '*'
1) "crypto-stats::SimpleKey []"

# Ver contenido del caché
$ docker exec crypto-redis-1 redis-cli GET "crypto-stats::SimpleKey []"
{
  "@class": "java.util.HashMap",
  "lastUpdated": "2025-10-23T00:00:00.524Z",
  "total": ["java.lang.Long", 1016],
  "hasSyncedData": true
}
```

---

## 🐛 Problemas Resueltos

### 1. Error SpEL con `.block()` en `unless`

**Problema**: 
```java
@Cacheable(value = "crypto-stats", unless = "#result == null || #result.block() == null")
```

**Error**:
```
SpelEvaluationException: Method block() cannot be found on type java.util.HashMap
```

**Solución**: Remover la condición `unless` porque Spring evalúa la expresión sobre el Mono wrapper, no el valor desenvuelto.

```java
@Cacheable(value = "crypto-stats")  // ✅ Correcto
```

### 2. Error de Serialización con `OffsetDateTime`

**Problema**:
```
SerializationException: Type id handling not implemented for type java.lang.Object
(through reference chain: java.util.HashMap["lastUpdated"])
```

**Causa**: El campo `lastUpdated` era un `OffsetDateTime` que Jackson no podía serializar.

**Solución**: Convertir a String antes de cachear:

```java
stats.put("lastUpdated", 
    latest.map(c -> c.getLastUpdated() != null 
        ? c.getLastUpdated().toString() 
        : null).orElse(null));
```

---

## 🔧 Comandos Útiles

### Gestión de Redis

```bash
# Verificar que Redis está funcionando
docker exec crypto-redis-1 redis-cli PING
# Respuesta: PONG

# Ver todas las keys
docker exec crypto-redis-1 redis-cli KEYS '*'

# Ver una key específica
docker exec crypto-redis-1 redis-cli GET "crypto-stats::SimpleKey []"

# Limpiar toda la base de datos (¡CUIDADO!)
docker exec crypto-redis-1 redis-cli FLUSHALL

# Monitorear comandos en tiempo real
docker exec crypto-redis-1 redis-cli MONITOR

# Información de memoria
docker exec crypto-redis-1 redis-cli INFO memory
```

### Reiniciar Servicios

```bash
# Reiniciar solo Redis
docker compose restart redis

# Reiniciar crypto-collector
docker compose restart crypto-collector-micro

# Ver logs en tiempo real
docker logs -f crypto-crypto-collector-micro-1
```

---

## 📈 Beneficios Demostrados

### 1. **Rendimiento**
- ⚡ **12x más rápido** en consultas repetidas
- 📉 **91% de reducción** en tiempo de respuesta
- 🚀 De 888ms a 73ms (815ms ahorrados)

### 2. **Reducción de Carga en BD**
- 💾 PostgreSQL solo se consulta **una vez por TTL**
- 🔄 Redis maneja todas las peticiones subsiguientes
- 📊 Menos conexiones activas a la base de datos

### 3. **Escalabilidad**
- 🌐 Menor uso de recursos de base de datos
- 📈 Mayor capacidad para manejar tráfico concurrente
- ⚙️ Fácil ajuste de TTL según necesidades

### 4. **Experiencia de Usuario**
- ⏱️ Respuestas casi instantáneas (< 100ms)
- 🎯 Datos consistentes durante el TTL
- 💯 Alta disponibilidad

---

## 🧪 Endpoints para Testing

### Público (Sin Autenticación)

```bash
# Información de Redis
GET http://localhost:8092/api/public/cache/redis-info

# Respuesta:
{
  "cacheNames": ["crypto-list", "crypto-stats", "crypto-details", "coingecko-api", "scheduler-status"],
  "redisConfigured": true,
  "totalCaches": 5,
  "status": "Redis is working! ✅"
}
```

### Protegidos (Requieren JWT)

```bash
# Obtener estadísticas (cacheado)
GET http://localhost:8092/api/crypto/stats
Authorization: Bearer <token>

# Información de cachés
GET http://localhost:8092/api/cache/info
Authorization: Bearer <token>

# Limpiar todos los cachés
DELETE http://localhost:8092/api/cache/clear-all
Authorization: Bearer <token>

# Limpiar un caché específico
DELETE http://localhost:8092/api/cache/clear/crypto-stats
Authorization: Bearer <token>
```

---

## 📚 Documentación Swagger

- **Auth Service**: http://localhost:8081/swagger-ui/index.html
- **Crypto Collector**: http://localhost:8092/swagger-ui/index.html

Todos los endpoints de caché están documentados en Swagger con:
- Ejemplos de request/response
- Códigos de estado HTTP
- Esquemas de autenticación JWT
- Descripciones detalladas

---

## 🎓 Buenas Prácticas Implementadas

### ✅ TTL Diferenciados
- Datos volátiles (stats) → TTL corto (1 min)
- Datos semi-estáticos (listas) → TTL medio (5 min)
- API externa → TTL muy corto (30 seg)

### ✅ Invalidación Inteligente
- Sincronización completa → Limpiar todos los cachés
- Endpoints manuales → Limpiar cachés específicos
- No cachear valores `null`

### ✅ Monitoreo
- Logs de Cache MISS para debugging
- Endpoints de información de caché
- Healthcheck de Redis en Docker

### ✅ Serialización Correcta
- Conversión de tipos no serializables (OffsetDateTime → String)
- JSON como formato de almacenamiento (human-readable)
- Manejo de objetos complejos (HashMap, List, etc.)

### ✅ Seguridad
- Endpoints de gestión protegidos con JWT
- Endpoints de información pública limitados
- Validación de tokens en todos los endpoints críticos

---

## 🚀 Próximos Pasos (Opcional)

### 1. Métricas y Monitoreo
- [ ] Integrar Spring Boot Actuator con métricas de caché
- [ ] Dashboard de Grafana para visualizar hit ratio
- [ ] Alertas cuando el hit ratio es bajo

### 2. Alta Disponibilidad
- [ ] Redis Sentinel para failover automático
- [ ] Redis Cluster para sharding
- [ ] Backup automático de datos

### 3. Optimizaciones
- [ ] Cache warming al iniciar la aplicación
- [ ] Compresión de valores grandes
- [ ] Prefijos personalizados para diferentes entornos

### 4. Testing
- [ ] Tests unitarios para métodos cacheados
- [ ] Tests de integración con Redis
- [ ] Benchmarks de rendimiento

---

## 📝 Changelog

### v1.0.0 - 2025-10-23

**✅ Implementación Inicial**
- Configuración de Redis 7 Alpine en Docker
- 5 cachés configurados con TTL diferenciados
- Anotaciones @Cacheable en 4 métodos del servicio
- CacheController con 6 endpoints de gestión
- Integración completa y funcional
- Documentación en Swagger
- Script de demo completo

**🐛 Bugs Corregidos**
- Error SpEL con `.block()` en `unless` condition
- Error de serialización con `OffsetDateTime`
- Configuración de seguridad para endpoints públicos

**📊 Resultados**
- Mejora de rendimiento: 91% (12x más rápido)
- Tiempo de respuesta: 888ms → 73ms
- Estado: ✅ Completamente funcional

---

## 👨‍💻 Soporte

Para más información sobre la configuración de Redis o problemas específicos, revisar:

1. **Logs del servicio**: `docker logs crypto-crypto-collector-micro-1`
2. **Logs de Redis**: `docker logs crypto-redis-1`
3. **Swagger UI**: http://localhost:8092/swagger-ui/index.html
4. **Script de demo**: `bash demo-completa.sh`

---

**✅ Implementación completada exitosamente el 23 de octubre de 2025**

*Documentación generada automáticamente por GitHub Copilot*
