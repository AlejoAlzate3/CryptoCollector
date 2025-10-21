# auth-microServices

Este módulo contiene el servicio de autenticación (register/login) y usa Liquibase para migraciones.

Cómo funcionan las migraciones

- El changelog principal está en `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Al iniciar la aplicación, Spring Boot ejecuta Liquibase automáticamente (si está habilitado).

Comandos útiles

Levantar todo con Docker Compose (desde la raíz del repo):

```bash
docker compose up --build -d
```

Verificar que la tabla `users` existe (ejecutar dentro del contenedor postgres):

```bash
docker compose exec postgres psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "\\dt"
```

Ejecutar migraciones manualmente (Maven):

```bash
mvn -pl microServices/auth-microServices liquibase:update
```

Pruebas rápidas (curl)

Registrar usuario:

```bash
curl -X POST -H "Content-Type: application/json" \
  http://localhost:8081/api/auth/register \
  -d '{"firstName":"Juan","lastName":"Perez","email":"juan@example.com","password":"secret"}'
```

Login:

```bash
curl -X POST -H "Content-Type: application/json" \
  http://localhost:8081/api/auth/login \
  -d '{"email":"juan@example.com","password":"secret"}'
```

Notas

- Asegúrate de tener las variables de entorno en `.env` configuradas antes de levantar el stack.
- Si quieres que los servicios se registren en Eureka, el `discovery-server` debe estar operativo y accesible en `http://discovery-server:8761` dentro de la red de Docker Compose.
