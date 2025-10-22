# ✅ VERIFICACIÓN HIBERNATE + LIQUIBASE

## 📊 Estado Actual del Sistema

### ✅ **TODO CONFIGURADO CORRECTAMENTE**

---

## 🗄️ Base de Datos: `crypto_collector_db`

### Tablas:
- ✅ `cryptocurrency` - Creada por Liquibase changeset #1
- ✅ `databasechangelog` - Tabla de control de Liquibase
- ✅ `databasechangeloglock` - Lock table de Liquibase

### Migraciones Ejecutadas:
```
ID: 1
Author: crypto_collector_db
File: db/changelog/db.changelog-master.yaml
Ejecutado: 2025-10-22 04:10:29
```

---

## 🗄️ Base de Datos: `cryptousers`

### Tablas:
- ✅ `users` - Creada por Liquibase changeset #001
- ✅ `databasechangelog` - Tabla de control de Liquibase
- ✅ `databasechangeloglock` - Lock table de Liquibase

### Migraciones Ejecutadas:
```
ID: 001-create-users-table
Author: assistant
File: db/changelog/db.changelog-master.yaml
Ejecutado: 2025-10-22 01:19:33
```

---

## ⚙️ Configuración de Hibernate

### **Perfil DEV (Actual)**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update        # ✅ Permite crear/modificar tablas
    show-sql: true            # ✅ Muestra queries SQL en logs
    properties:
      hibernate:
        format_sql: true      # ✅ Formatea SQL legiblemente
```

### **Perfil PROD (Cuando se active)**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate      # ✅ Solo valida esquema (más seguro)
    show-sql: false           # ❌ No muestra queries (mejor performance)
    properties:
      hibernate:
        format_sql: false
```

---

## 🔄 Flujo de Migraciones

### 1. **Liquibase se ejecuta PRIMERO**
   - Lee `db/changelog/db.changelog-master.yaml`
   - Ejecuta changesets que no están en `databasechangelog`
   - Crea estructura base de tablas

### 2. **Hibernate se ejecuta DESPUÉS**
   - **DEV**: `ddl-auto: update` → Puede agregar columnas/índices adicionales
   - **PROD**: `ddl-auto: validate` → Solo verifica que el esquema coincida con las entidades

### 3. **Sincronización**
   - Liquibase maneja migraciones estructurales
   - Hibernate mantiene sincronía entre entidades Java y BD

---

## 📝 Archivos de Changelog

### Crypto Collector Micro
```
microServices/crypto-collector-micro/src/main/resources/
└── db/
    └── changelog/
        └── db.changelog-master.yaml  ✅ Configurado
```

**Contenido:**
- Changeset #1: Crea tabla `cryptocurrency` con todas sus columnas
- Define constraints: PRIMARY KEY, UNIQUE, NOT NULL
- Especifica tipos de datos apropiados

### Auth Micro Services  
```
microServices/auth-microServices/src/main/resources/
└── db/
    └── changelog/
        └── db.changelog-master.yaml  ✅ Configurado
```

**Contenido:**
- Changeset #001: Crea tabla `users`
- Define columnas: id, first_name, last_name, email, password
- Establece constraints de unicidad en email

---

## 🔍 Verificación en Tiempo Real

### Ver logs de Liquibase:
```bash
# Crypto Collector
docker logs crypto-crypto-collector-micro-1 | grep -i liquibase

# Auth Service
docker logs crypto-auth-service-1 | grep -i liquibase
```

### Ver estructura de tablas:
```bash
# Crypto DB
docker exec -it crypto-postgres-1 psql -U postgres -d crypto_collector_db -c "\d cryptocurrency"

# Auth DB
docker exec -it crypto-postgres-1 psql -U postgres -d cryptousers -c "\d users"
```

### Ver changesets ejecutados:
```bash
docker exec -it crypto-postgres-1 psql -U postgres -d crypto_collector_db \
  -c "SELECT * FROM databasechangelog ORDER BY orderexecuted;"
```

---

## ✅ Checklist de Validación

- [x] Liquibase configurado en ambos servicios
- [x] Tablas `databasechangelog` y `databasechangeloglock` creadas
- [x] Changesets ejecutados correctamente
- [x] Hibernate configurado con `ddl-auto: update` en DEV
- [x] Hibernate configurado con `ddl-auto: validate` en PROD
- [x] Tablas de negocio creadas (`cryptocurrency`, `users`)
- [x] Constraints aplicados (PK, UNIQUE, NOT NULL)
- [x] Sin errores de sincronización entre entidades y BD

---

## 🚀 Agregar Nueva Migración

### Paso 1: Crear nuevo changeset en YAML

```yaml
databaseChangeLog:
  - changeSet:
      id: "2"
      author: "tu_nombre"
      changes:
        - addColumn:
            tableName: cryptocurrency
            columns:
              - column:
                  name: nueva_columna
                  type: varchar(255)
                  constraints:
                    nullable: true
```

### Paso 2: Rebuild y deploy
```bash
mvn clean package -DskipTests
docker compose up -d --build crypto-collector-micro
```

### Paso 3: Verificar
```bash
docker logs crypto-crypto-collector-micro-1 | grep -i "changeset 2"
```

---

## ⚠️ Mejores Prácticas

### ✅ **DO (Hacer)**
- Usar Liquibase para **TODAS** las modificaciones de esquema
- Probar migraciones en DEV antes de PROD
- Mantener changesets pequeños y atómicos
- Incluir rollback en changesets complejos
- Versionar archivos de changelog con el código

### ❌ **DON'T (No Hacer)**
- Modificar changesets ya ejecutados
- Confiar solo en `ddl-auto: update` para producción
- Ejecutar SQL manual sin registrar en Liquibase
- Cambiar `ddl-auto` a `create` o `create-drop` en PROD
- Mezclar cambios de esquema con datos de negocio

---

## 🔄 Rollback de Migraciones

Liquibase soporta rollback automático:

```bash
# Rollback del último changeset
docker exec -it crypto-crypto-collector-micro-1 \
  java -jar app.jar --spring.liquibase.rollback-count=1

# Rollback a una fecha específica
docker exec -it crypto-crypto-collector-micro-1 \
  java -jar app.jar --spring.liquibase.rollback-date=2025-10-20
```

---

## 📚 Referencias

- **Liquibase**: https://docs.liquibase.com/home.html
- **Hibernate DDL**: https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.jpa-and-spring-data.ddl-mode
- **Spring Boot Migrations**: https://docs.spring.io/spring-boot/reference/howto/data-initialization.html

---

✅ **RESUMEN**: Hibernate + Liquibase están perfectamente configurados y funcionando en conjunto.
