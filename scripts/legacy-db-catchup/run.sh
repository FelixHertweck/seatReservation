#!/usr/bin/env bash
# Backs up the docker-compose database and applies catchup.sql to it.
# Usage: scripts/legacy-db-catchup/run.sh [backup-file]
# Requires the `db` service of docker-compose.yml to be running (docker compose up -d db).
# Stop the app service first so nothing writes to the database meanwhile.
set -euo pipefail

cd "$(dirname "$0")/../.."
SQL_FILE="scripts/legacy-db-catchup/catchup.sql"
BACKUP="${1:-db-backup-$(date +%Y%m%d-%H%M%S).sql}"

if [ -z "$(docker compose ps --status running --services db)" ]; then
    echo "The db service is not running. Start it first: docker compose up -d db" >&2
    exit 1
fi

echo "==> Backup -> $BACKUP"
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$BACKUP"
[ -s "$BACKUP" ] || { echo "Backup is empty, aborting." >&2; exit 1; }

echo "==> Applying $SQL_FILE"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < "$SQL_FILE"

echo "==> Done. To restore the backup if needed:"
echo "    docker compose exec -T db sh -c 'psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\"' < $BACKUP"
