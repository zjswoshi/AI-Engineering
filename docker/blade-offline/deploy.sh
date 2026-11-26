#!/usr/bin/env bash
set -euo pipefail

release_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
state_dir=${BLADE_STATE_DIR:-/srv/blade}
config_file=$state_dir/.env.blade

if [[ $(uname -m) != x86_64 ]]; then
    printf 'The bundle requires a linux/amd64 Docker host.\n' >&2
    exit 1
fi
if (( EUID != 0 )); then
    printf 'Run with sudo bash deploy.sh.\n' >&2
    exit 1
fi

compose=(docker compose --project-directory "$release_dir" --env-file "$config_file"
    -f "$release_dir/compose.blade.yaml" -f "$release_dir/compose.blade.gpu.yaml"
    -f "$release_dir/compose.images.yaml")

docker info --format '{{.OSType}}' | grep -qx linux
install -d -m 0750 "$state_dir"
exec 9>"$state_dir/.deploy.lock"
flock 9
install -d -o 10001 -g 10001 -m 0750 "$state_dir/data/uploads" "$state_dir/data/reports"
export BLADE_DATA_DIR="$state_dir/data"
export BLADE_MODEL_DIR="$release_dir/blade-models"

if [[ ! -e $config_file ]]; then
    db_password=$(od -An -N 24 -tx1 /dev/urandom | tr -d ' \n')
    admin_password=$(od -An -N 24 -tx1 /dev/urandom | tr -d ' \n')
    config_temp=$(mktemp "$state_dir/.env.blade.XXXXXX")
    trap 'rm -f -- "$config_temp"' EXIT
    printf 'BLADE_DB_PASSWORD=%s\nBLADE_ADMIN_PASSWORD=%s\nBLADE_BIND_ADDRESS=127.0.0.1\nBLADE_WEB_PORT=8090\n' \
        "$db_password" "$admin_password" > "$config_temp"
    mv -- "$config_temp" "$config_file"
    trap - EXIT
fi

"${compose[@]}" config --quiet
docker image load --input "$release_dir/images.tar"
"${compose[@]}" up -d --no-build --pull never --wait --wait-timeout 180
"${compose[@]}" exec -T algorithm python -c 'import json; from urllib.request import urlopen; result = json.load(urlopen("http://127.0.0.1:8000/health", timeout=5)); assert result == {"status": "UP", "provider": "CUDAExecutionProvider"}, result; print(result)'
"${compose[@]}" ps
printf 'Initial login: admin. Credentials and port settings: %s\n' "$config_file"
