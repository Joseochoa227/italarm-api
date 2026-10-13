#!/usr/bin/env bash
# Copia de la base de datos de producción a Cloudflare R2 (RNF-05, P-67).
#
# Saca un pg_dump comprimido y lo sube a s3://$COPIAS_BUCKET/base/AAAA-MM-DD.dump. Después borra
# las copias viejas: se guardan las de los últimos 30 días y la del día 1 de cada mes durante un año.
#
# Variables:
#   COPIA_DB_URL          URL de conexión de PostgreSQL (postgresql://usuario:clave@host/base?sslmode=require)
#   COPIAS_BUCKET         bucket de las copias (por ejemplo italarm-copias)
#   S3_ENDPOINT           endpoint S3 de R2 (https://<cuenta>.r2.cloudflarestorage.com)
#   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY  clave de API de R2
#   PG_DUMP               opcional: ruta de pg_dump (versión 16 o mayor)
#   HOY                   opcional: fecha de la copia (AAAA-MM-DD); por defecto, hoy en Colombia
set -euo pipefail

: "${COPIA_DB_URL:?Falta COPIA_DB_URL}"
: "${COPIAS_BUCKET:?Falta COPIAS_BUCKET}"
: "${S3_ENDPOINT:?Falta S3_ENDPOINT}"
PG_DUMP="${PG_DUMP:-pg_dump}"
HOY="${HOY:-$(TZ=America/Bogota date +%F)}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-auto}"
s3() { aws --endpoint-url "$S3_ENDPOINT" "$@"; }

archivo="$(mktemp -d)/italarm-$HOY.dump"
echo "Sacando la copia de la base ($HOY)..."
"$PG_DUMP" --format=custom --no-owner --no-privileges --file="$archivo" "$COPIA_DB_URL"
tamano=$(wc -c < "$archivo")
if [ "$tamano" -lt 1024 ]; then
  echo "La copia pesa solo $tamano bytes: algo salió mal." >&2
  exit 1
fi
s3 s3 cp "$archivo" "s3://$COPIAS_BUCKET/base/$HOY.dump" --only-show-errors
echo "Copia subida: s3://$COPIAS_BUCKET/base/$HOY.dump ($tamano bytes)"

# Retención (P-67): últimos 30 días + el día 1 de cada mes durante 365 días.
limite_diario=$(date -d "$HOY - 30 days" +%F)
limite_mensual=$(date -d "$HOY - 365 days" +%F)
for clave in $(s3 s3api list-objects-v2 --bucket "$COPIAS_BUCKET" --prefix base/ \
    --query 'Contents[].Key' --output text); do
  fecha="${clave#base/}"
  fecha="${fecha%.dump}"
  [[ "$fecha" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || continue
  if [[ "$fecha" > "$limite_diario" || "$fecha" == "$limite_diario" ]]; then
    continue
  fi
  if [[ "${fecha:8:2}" == "01" && ( "$fecha" > "$limite_mensual" || "$fecha" == "$limite_mensual" ) ]]; then
    continue
  fi
  s3 s3 rm "s3://$COPIAS_BUCKET/$clave" --only-show-errors
  echo "Copia vieja borrada: $clave"
done
