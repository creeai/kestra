#!/usr/bin/env bash
# Create a new Kestra tenant: deploy stack. Optionally create MinIO bucket first.
# Usage: ./create-tenant.sh <tenant_slug> [domain]
set -e
TENANT="${1:-tenant1}"
DOMAIN="${2:-${TENANT}.kestra.seudominio.com}"
STACK_NAME="kestra_${TENANT}"
BUCKET="kestra-${TENANT}"
export TENANT DOMAIN
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-$(openssl rand -base64 24)}"
export S3_ENDPOINT="${S3_ENDPOINT:-http://infra_minio:9000}"
export S3_BUCKET="$BUCKET"
export S3_REGION="${S3_REGION:-us-east-1}"
export S3_ACCESS_KEY="${S3_ACCESS_KEY:-minio_access}"
export S3_SECRET_KEY="${S3_SECRET_KEY:-minio_secret}"
echo "Deploying stack $STACK_NAME for $DOMAIN..."
docker stack deploy -c stack-kestra-tenant.yml --with-registry-auth "$STACK_NAME"
echo "Done. Add DNS: $DOMAIN to your node IP."
