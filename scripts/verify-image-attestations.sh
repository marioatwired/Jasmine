#!/usr/bin/env bash
set -euo pipefail

image="${1:-}"
if [[ -z "$image" ]]; then
    echo "Usage: $0 <image-reference>" >&2
    exit 2
fi

fail() {
    echo "Image attestation verification failed: $*" >&2
    exit 1
}

command -v jq >/dev/null 2>&1 || fail "jq is required"
command -v docker >/dev/null 2>&1 || fail "docker is required"

echo "==> Verifying manifest platforms for $image"
platforms="$(docker buildx imagetools inspect "$image" \
    --format '{{range .Manifests}}{{.Platform}}{{"\n"}}{{end}}')"
for platform in linux/amd64 linux/arm64; do
    grep -qx "$platform" <<< "$platforms" \
        || fail "missing $platform manifest in $image"
done

for platform in linux/amd64 linux/arm64; do
    echo "==> Verifying $platform attestations for $image"
    provenance="$(docker buildx imagetools inspect "$image" \
        --format "{{ json (index .Provenance \"$platform\").SLSA }}")"
    [[ -n "$provenance" && "$provenance" != "null" ]] \
        || fail "missing SLSA provenance for $platform"
    jq -e '.buildDefinition or .buildType' <<< "$provenance" >/dev/null \
        || fail "invalid SLSA provenance for $platform"

    sbom="$(docker buildx imagetools inspect "$image" \
        --format "{{ json (index .SBOM \"$platform\").SPDX }}")"
    [[ -n "$sbom" && "$sbom" != "null" ]] \
        || fail "missing SPDX SBOM for $platform"
    jq -e '(.packages // []) | length > 0' <<< "$sbom" >/dev/null \
        || fail "empty SPDX SBOM for $platform"
done

echo "Image attestations verified: $image"
