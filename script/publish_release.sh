#!/usr/bin/env bash
set -euo pipefail

: "${RELEASE_API_URL:?Set RELEASE_API_URL to the hosting API base URL}"
: "${RELEASE_UPLOAD_URL:?Set RELEASE_UPLOAD_URL to the asset upload API base URL}"
: "${RELEASE_REPOSITORY:?Set RELEASE_REPOSITORY to owner/repository}"
: "${RELEASE_TOKEN:?Set RELEASE_TOKEN to a release-capable access token}"
: "${RELEASE_TAG:?Set RELEASE_TAG to the release tag}"
: "${RELEASE_NAME:?Set RELEASE_NAME to the release title}"

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
api_url="${RELEASE_API_URL%/}"
upload_url="${RELEASE_UPLOAD_URL%/}"
release_endpoint="$api_url/repos/$RELEASE_REPOSITORY/releases"

payload="$(RELEASE_TAG="$RELEASE_TAG" RELEASE_NAME="$RELEASE_NAME" python3 - <<'PY'
import json
import os

print(json.dumps({
    "tag_name": os.environ["RELEASE_TAG"],
    "name": os.environ["RELEASE_NAME"],
    "target_commitish": "dev",
    "body": "ARMCP Android APK release.",
    "draft": False,
    "prerelease": False,
}))
PY
)"

response="$(curl --fail --silent --show-error \
  --request POST \
  --header "Accept: application/json" \
  --header "Authorization: token $RELEASE_TOKEN" \
  --header "Content-Type: application/json" \
  --data "$payload" \
  "$release_endpoint")"

release_id="$(printf '%s' "$response" | python3 -c 'import json, sys; print(json.load(sys.stdin)["id"])')"
release_url="$(printf '%s' "$response" | python3 -c 'import json, sys; print(json.load(sys.stdin).get("html_url", ""))')"

for apk in "$project_root"/release/*.apk; do
  asset_name="$(basename "$apk")"
  encoded_name="$(ASSET_NAME="$asset_name" python3 -c 'import os, urllib.parse; print(urllib.parse.quote(os.environ["ASSET_NAME"]))')"
  curl --fail --silent --show-error \
    --request POST \
    --header "Accept: application/json" \
    --header "Authorization: token $RELEASE_TOKEN" \
    --header "Content-Type: application/vnd.android.package-archive" \
    --data-binary "@$apk" \
    "$upload_url/repos/$RELEASE_REPOSITORY/releases/$release_id/assets?name=$encoded_name" \
    >/dev/null
  printf 'Uploaded %s\n' "$asset_name"
done

printf 'Release published: %s\n' "${release_url:-$RELEASE_TAG}"
