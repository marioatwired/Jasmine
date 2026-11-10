#!/usr/bin/env bash
set -euo pipefail

image="${1:-jasmine:smoke}"
container="jasmine-docker-smoke-$$"
work_dir="$(mktemp -d)"

cleanup() {
    docker rm -f "$container" "${container}-first" >/dev/null 2>&1 || true
    rm -rf "$work_dir"
}
trap cleanup EXIT

fail() {
    echo "Docker smoke test failed: $*" >&2
    exit 1
}

# The container runs as UID 10001; allow it to write to the temporary volume.
chmod 777 "$work_dir"

echo "==> First run without config.txt"
set +e
first_output="$(timeout 120s docker run --rm --name "${container}-first" \
    -v "$work_dir:/musicbot" "$image" 2>&1)"
first_status=$?
set -e
printf '%s\n' "$first_output"

[[ $first_status -eq 0 ]] || fail "first-run container exited with status $first_status"
[[ -s "$work_dir/config.txt" ]] || fail "config.txt was not created"
grep -q "# START OF JASMINE CONFIG" "$work_dir/config.txt" \
    || fail "config.txt has an unexpected header"
grep -q "Default config created" <<< "$first_output" \
    || fail "the default-config log message was not emitted"

rm -f "$work_dir/config.txt"

echo "==> Environment credentials"
docker run -d --name "$container" \
    -e JASMINE_TOKEN=fake-token \
    -e JASMINE_OWNER=123456789012345678 \
    -v "$work_dir:/musicbot" "$image" >/dev/null

logs=""
for _ in $(seq 1 30); do
    logs="$(docker logs "$container" 2>&1 || true)"
    if grep -q "Using Discord owner ID from environment" <<< "$logs"; then
        break
    fi
    if ! docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null | grep -q true; then
        break
    fi
    sleep 1
done
printf '%s\n' "$logs"

grep -q "Using Discord token from environment" <<< "$logs" \
    || fail "token was not read from the environment"
grep -q "Using Discord owner ID from environment" <<< "$logs" \
    || fail "owner was not read from the environment"
if grep -q "Please provide a bot token" <<< "$logs"; then
    fail "the container prompted for a token"
fi
[[ -s "$work_dir/config.txt" ]] \
    || fail "config.txt was not created when environment credentials were used"

echo "Docker smoke test passed: $(basename "$image")"
