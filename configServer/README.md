# Config Server - instrucciones rápidas

Este README explica cómo preparar variables de entorno y levantar los servicios en local.

Variables requeridas (copia `.env.example` a `.env` y edítalo):

- `CONFIG_SERVER_URI` (ej: `http://localhost:8888`)
- `JWT_SECRET` (secreto para JWT; en `prod` la aplicación esperará que esté configurado)
- `JWT_EXPIRATION` (milisegundos, opcional)
- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` (para docker-compose)

Pasos para ejecutar en local (bash):

1. Copia `.env.example` a `.env` y edita los valores sensibles (no comitees `.env`).
2. Levanta la DB: `docker-compose up -d` (en la raíz del repo).
3. Ejecuta en orden:
   - `mvn -pl configServer spring-boot:run`
   - `mvn -pl discoveryServer spring-boot:run`
   - `mvn -pl apiGateWay spring-boot:run`
   - `mvn -pl microServices/auth-microServices spring-boot:run`

Notas:
- Para producción, usa un secret manager (Vault, AWS Secrets Manager, etc.) y no guardes secretos en el repo.
- `.env` está incluido en `.gitignore`.
