#!/usr/bin/env bash
set -euo pipefail

# Levanta el proyecto local usando docker-compose
# 1) Copia .env.example -> .env y edítalo si aún no lo hiciste
# 2) Ejecuta este script desde la raíz del repo

if [ ! -f .env ]; then
  echo ".env no encontrado. Copia .env.example a .env y edita los valores antes de continuar."
  exit 1
fi

echo "Compilando jars con Maven (sin tests)..."
mvn -DskipTests package

echo "Construyendo imágenes Docker y levantando servicios..."
docker compose build --pull --no-cache
docker compose up -d

echo "Servicios levantados. Revisa logs con: docker compose logs -f"
