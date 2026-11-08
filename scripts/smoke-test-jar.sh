#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jar_path="${1:-}"
java_bin="${JAVA_BIN:-java}"

if [[ -z "$jar_path" ]]; then
    jar_path="$(find "$repo_root/target" -maxdepth 1 -name '*-All.jar' -print -quit)"
fi

if [[ -z "$jar_path" || ! -f "$jar_path" ]]; then
    echo "Smoke test failed: shaded JAR not found." >&2
    exit 1
fi

jar_path="$(cd "$(dirname "$jar_path")" && pwd)/$(basename "$jar_path")"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

(
    cd "$work_dir"
    "$java_bin" -jar "$jar_path" generate-config
)

if [[ ! -s "$work_dir/config.txt" ]]; then
    echo "Smoke test failed: config.txt was not generated." >&2
    exit 1
fi

if ! grep -q "# START OF JASMINE CONFIG" "$work_dir/config.txt"; then
    echo "Smoke test failed: generated config.txt has an unexpected header." >&2
    exit 1
fi

echo "Smoke test passed: $(basename "$jar_path") generated a valid config.txt."
