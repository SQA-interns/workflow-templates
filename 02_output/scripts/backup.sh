#!/usr/bin/env bash
# Daily database backup of a staging or production environment (environments.md: pg_dump, kept 30 days,
# stored outside the database host). Usage: scripts/backup.sh deploy/<environment>.env
# Cron (02:00 Europe/Ljubljana):  CRON_TZ=Europe/Ljubljana
#                                 0 2 * * * /path/to/02_output/scripts/backup.sh /path/to/02_output/deploy/production.env
set -euo pipefail
env_file="$(realpath "${1:?usage: backup.sh deploy/<environment>.env}")"
cd "$(dirname "$0")/../deploy"
backup_dir="$(sed -n 's/^BACKUP_DIR=//p' "$env_file")"
[ -n "$backup_dir" ] && [ -d "$backup_dir" ] || { echo "BACKUP_DIR is empty or not a directory" >&2; exit 1; }
target="$backup_dir/contacts-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose --env-file "$env_file" exec -T postgres pg_dump -U contacts -d contacts -Fc > "$target.part"
mv "$target.part" "$target"
find "$backup_dir" -maxdepth 1 -name 'contacts-*.dump' -mtime +29 -delete
echo "Backup written: $target"
