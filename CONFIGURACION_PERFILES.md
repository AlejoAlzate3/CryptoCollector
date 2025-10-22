# 🔧 Configuración de Perfiles (Dev/Prod)

## 📋 Resumen

El proyecto utiliza **Spring Profiles** para gestionar diferentes configuraciones entre entornos de desarrollo y producción.

## 🎯 Perfiles Disponibles

### 🔵 **DEV** (Desarrollo)
- Logs verbosos (`show-sql: true`, `DEBUG level`)
- Swagger UI habilitado
- Actuator con detalles completos
- Hibernate DDL: `update` (auto-creación de tablas)
- Valores por defecto permisivos

### 🔴 **PROD** (Producción)
- Logs mínimos (`show-sql: false`, `WARN level`)
- Swagger UI deshabilitado
- Actuator con detalles limitados
- Hibernate DDL: `validate` (solo valida esquema)
- Secrets obligatorios desde variables de entorno
- Connection pooling optimizado

---

## 🚀 Cómo Cambiar de Perfil

### **Método 1: Docker Compose (Recomendado)**

Edita `docker-compose.yml` y cambia `SPRING_PROFILES_ACTIVE`:

```yaml
# Para crypto-collector-micro
crypto-collector-micro:
  environment:
    - SPRING_PROFILES_ACTIVE=prod  # Cambiar a 'prod' o 'dev'
    
# Para auth-service  
auth-service:
  environment:
    - SPRING_PROFILES_ACTIVE=prod  # Cambiar a 'prod' o 'dev'
```

Luego rebuildeay reinicia:
```bash
docker compose down
docker compose up -d --build
```

---

### **Método 2: Variable de Entorno**

Agrega al archivo `.env`:
```properties
SPRING_PROFILES_ACTIVE=prod
```

Y en `docker-compose.yml`:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}
```

---

### **Método 3: Ejecución Local (Sin Docker)**

```bash
# Dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Prod
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 📁 Archivos de Configuración

### Ubicación de Perfiles
```
configServer/src/main/resources/config/
├── crypto-collector-micro-dev.yml    # Configuración DEV del crypto service
├── crypto-collector-micro-prod.yml   # Configuración PROD del crypto service
├── auth-microServices-dev.yml        # Configuración DEV del auth service
├── auth-microServices-prod.yml       # Configuración PROD del auth service
├── apiGateway-dev.yml                # Configuración DEV del gateway
├── apiGateway-prod.yml               # Configuración PROD del gateway
├── discoveryServer-dev.yml           # Configuración DEV de Eureka
└── discoveryServer-prod.yml          # Configuración PROD de Eureka
```

---

## ⚙️ Diferencias Clave entre Perfiles

| Configuración | DEV | PROD |
|--------------|-----|------|
| **Hibernate DDL** | `update` | `validate` |
| **SQL Logging** | `true` | `false` |
| **Log Level** | `DEBUG` / `INFO` | `WARN` |
| **Swagger UI** | ✅ Habilitado | ❌ Deshabilitado |
| **Actuator Details** | `always` | `when-authorized` |
| **Connection Pool** | Default | Optimizado (20 max) |
| **Secrets** | Valores por defecto | Obligatorios desde ENV |

---

## 🔐 Variables de Entorno Requeridas en PROD

En producción, estas variables **DEBEN** estar configuradas:

```properties
# Obligatorias
JWT_SECRET=tu_secret_super_seguro_minimo_32_caracteres
COINGECKO_API_KEY=tu_api_key_de_coingecko
POSTGRES_PASSWORD=password_seguro_de_base_de_datos

# Recomendadas
JWT_EXPIRATION=86400000  # 24 horas en milisegundos
POSTGRES_USER=crypto_admin
POSTGRES_DB=crypto_production
```

---

## ✅ Verificación de Perfil Activo

### Ver qué perfil está activo:

```bash
# Logs del servicio crypto-collector-micro
docker logs crypto-crypto-collector-micro-1 | grep "The following 1 profile is active"

# Logs del servicio auth
docker logs crypto-auth-service-1 | grep "The following 1 profile is active"
```

**Salida esperada:**
```
The following 1 profile is active: "dev"
```
o
```
The following 1 profile is active: "prod"
```

---

## 🐛 Troubleshooting

### Problema: Cambié el perfil pero sigue usando el anterior
**Solución:**
```bash
# Rebuild completo
docker compose down
docker compose build --no-cache crypto-collector-micro auth-service
docker compose up -d
```

### Problema: En PROD muestra "The following 1 profile is active: dev"
**Solución:** Verifica que `SPRING_PROFILES_ACTIVE=prod` esté en el `environment` del servicio en `docker-compose.yml`

### Problema: Error "JWT_SECRET is required"
**Solución:** En modo `prod`, el JWT_SECRET debe estar en el archivo `.env`:
```properties
JWT_SECRET=un_secret_muy_seguro_con_minimo_32_caracteres
```

---

## 📊 Verificar Configuración Actual

```bash
# Endpoint de actuator que muestra configuración (solo en DEV)
curl http://localhost:8092/actuator/env | grep "spring.profiles.active"
```

---

## 🔄 Migración Dev → Prod

### Checklist antes de pasar a producción:

- [ ] Cambiar `SPRING_PROFILES_ACTIVE=prod` en docker-compose.yml
- [ ] Configurar `JWT_SECRET` seguro (mínimo 32 caracteres)
- [ ] Configurar `POSTGRES_PASSWORD` fuerte
- [ ] Obtener API Key válida de CoinGecko
- [ ] Ejecutar migraciones de Liquibase manualmente
- [ ] Verificar que `ddl-auto: validate` funciona sin errores
- [ ] Deshabilitar endpoints innecesarios de actuator
- [ ] Configurar HTTPS en el API Gateway
- [ ] Configurar límites de rate limiting
- [ ] Configurar monitoreo y alertas (Prometheus/Grafana)

---

## 📝 Notas Importantes

1. **Liquibase**: En ambos perfiles, Liquibase maneja las migraciones automáticamente. La diferencia es:
   - **DEV**: Hibernate puede crear/modificar tablas adicionales (`ddl-auto: update`)
   - **PROD**: Hibernate solo valida el esquema (`ddl-auto: validate`), cualquier cambio debe hacerse por Liquibase

2. **Config Server**: Siempre usa perfil `native` para leer archivos del filesystem

3. **Cambios de perfil**: Requieren rebuild del contenedor para aplicarse completamente

---

¿Tienes dudas? Consulta la documentación de Spring Profiles: https://docs.spring.io/spring-boot/reference/features/profiles.html
