#!/usr/bin/env sh
# Crea las bases de datos que necesitan los tests de contexto de cada servicio.
# Uso:
#   sh scripts/create-test-databases.sh                    # psql o el contenedor de compose
#   PGPORT=5433 sh scripts/create-test-databases.sh        # Postgres de docker compose
#   PG_CONTAINER=mi-postgres sh scripts/create-test-databases.sh
#   PGHOST=db PGPORT=5432 PGUSER=ledgerline PGPASSWORD=... sh scripts/create-test-databases.sh
set -eu

HOST="${PGHOST:-localhost}"
PORT="${PGPORT:-5432}"
USER="${PGUSER:-ledgerline}"
PASSWORD="${PGPASSWORD:-ledgerline_secret}"
DATABASES="ledgerline_auth ledgerline_customer ledgerline_account \
ledgerline_transaction ledgerline_transaction_test \
ledgerline_notification ledgerline_notification_test \
ledgerline_audit ledgerline_audit_test ledgerline_reporting"

find_container() {
  # 1) el nombre explícito, 2) el de compose, 3) el primer contenedor de Postgres
  if [ -n "${PG_CONTAINER:-}" ]; then
    echo "$PG_CONTAINER"
    return 0
  fi
  names=$(docker ps --format '{{.Names}} {{.Image}}' 2>/dev/null || true)
  for candidate in ledgerline-postgres; do
    if echo "$names" | awk '{print $1}' | grep -qx "$candidate"; then
      echo "$candidate"
      return 0
    fi
  done
  echo "$names" | grep -i 'postgres' | awk '{print $1}' | head -n 1
}

run_psql() {
  if command -v psql >/dev/null 2>&1; then
    PGPASSWORD="$PASSWORD" psql -h "$HOST" -p "$PORT" -U "$USER" -d postgres -v ON_ERROR_STOP=1 "$@"
    return
  fi
  if command -v docker >/dev/null 2>&1; then
    container=$(find_container)
    if [ -n "$container" ]; then
      docker exec -e PGPASSWORD="$PASSWORD" "$container" \
        psql -U "$USER" -d postgres -v ON_ERROR_STOP=1 "$@"
      return
    fi
  fi
  echo "No encuentro psql ni un contenedor de Postgres en marcha." >&2
  echo "Arranca Postgres (docker compose up postgres) o instala postgresql-client." >&2
  echo "Si usas otro contenedor, indícalo con PG_CONTAINER=<nombre>." >&2
  exit 1
}

for db in $DATABASES; do
  exists=$(run_psql -tAc "SELECT 1 FROM pg_database WHERE datname = '$db'" | tr -d '[:space:]')
  if [ "$exists" = "1" ]; then
    echo "ya existe: $db"
  else
    run_psql -c "CREATE DATABASE $db" >/dev/null
    echo "creada:    $db"
  fi
done

echo
echo "Listo. Si tu Postgres no escucha en 5432, exporta POSTGRES_TEST_PORT antes de lanzar los tests."
