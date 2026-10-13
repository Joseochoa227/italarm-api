#!/usr/bin/env bash
# Copia semanal de los archivos (fotos, logo y facturas) a otro bucket de R2 (RNF-05, P-67).
#
# Sincroniza s3://$ARCHIVOS_BUCKET con s3://$COPIAS_BUCKET/archivos/. No borra en la copia lo que
# se haya borrado en el original: una foto quitada por error se puede recuperar de aquí.
#
# Variables: ARCHIVOS_BUCKET, COPIAS_BUCKET, S3_ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
set -euo pipefail

: "${ARCHIVOS_BUCKET:?Falta ARCHIVOS_BUCKET}"
: "${COPIAS_BUCKET:?Falta COPIAS_BUCKET}"
: "${S3_ENDPOINT:?Falta S3_ENDPOINT}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-auto}"

aws --endpoint-url "$S3_ENDPOINT" s3 sync "s3://$ARCHIVOS_BUCKET" "s3://$COPIAS_BUCKET/archivos" \
  --only-show-errors
total=$(aws --endpoint-url "$S3_ENDPOINT" s3 ls "s3://$COPIAS_BUCKET/archivos/" --recursive \
  | wc -l)
echo "Archivos en la copia: $total"
