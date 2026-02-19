#!/usr/bin/env bash
# Restore one tenant from backup: Postgres + MinIO bucket, then redeploy stack.
# Usage: ./restore-tenant.sh <tenant_slug> <backup_dir>
# Example: ./restore-tenant.sh tenant1 /backups/kestra/tenant1/20250101-120000

set -e
TENANT=${1:?Usage: $0 <tenant_slug> <backup_dir>}
BACKUP_DIR=${2:?Usage: $0 <tenant_slug> <backup_dir>}
STACK_NAME=kestra_${TENANT}
BUCKET=kestra-${TENANT}

echo "Restoring tenant $TENANT from $BACKUP_DIR"

# 1. Stop stack to avoid writes during restore
docker stack rm "$STACK_NAME" || true
echo "Waiting for stack to shut down..."
sleep 10

# 2. Restart stack so Postgres is up
export TENANT
export DOMAIN=${DOMAIN:-${TENANT}.kestra.seudominio.com}
export POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
export S3_ENDPOINT=${S3_ENDPOINT:-http://infra_minio:9000}
export S3_BUCKET=$BUCKET
export S3_REGION=${S3_REGION:-us-east-1}
export S3_ACCESS_KEY=${S3_ACCESS_KEY}
export S3_SECRET_KEY=${S3_SECRET_KEY}
docker stack deploy -c stack-kestra-tenant.yml "$STACK_NAME"
echo "Waiting for Postgres to be ready..."
sleep 15

# 3. Restore Postgres
CONTAINER=$(docker ps -q -f "name=${STACK_NAME}_postgres" | head -1)
if [ -n "$CONTAINER" ] && [ -f "$BACKUP_DIR/postgres.sql.gz" ]; then
  gunzip -c "$BACKUP_DIR/postgres.sql.gz" | docker exec -i "$CONTAINER" psql -U kestra kestra
  echo "Postgres restored."
fi

# 4. Restore MinIO bucket (if mc and backup exist)
if command -v mc &>/dev/null && [ -d "$BACKUP_DIR/bucket" ]; then
  mc mirror "$BACKUP_DIR/bucket" "minio/$BUCKET"
  echo "Bucket restored."
fi

echo "Restore completed. Stack $STACK_NAME is running."
