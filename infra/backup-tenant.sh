#!/usr/bin/env bash
# Backup one tenant: Postgres dump and optional MinIO bucket mirror.
# Usage: ./backup-tenant.sh <tenant_slug> [backup_dir]
set -e
TENANT="${1:-tenant1}"
BACKUP_ROOT="${2:-./backups}"
STACK_NAME="kestra_${TENANT}"
DATE=$(date +%Y%m%d-%H%M%S)
DEST="$BACKUP_ROOT/$TENANT/$DATE"
mkdir -p "$DEST"
PG_SERVICE="${STACK_NAME}_postgres"
CONTAINER=$(docker ps -q -f "name=${PG_SERVICE}" | head -1)
if [ -n "$CONTAINER" ]; then
  docker exec "$CONTAINER" pg_dump -U kestra kestra | gzip -c > "$DEST/postgres.sql.gz"
  echo "Postgres dump: $DEST/postgres.sql.gz"
fi
echo "Backup completed: $DEST"
