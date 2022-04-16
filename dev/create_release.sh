#!/bin/zsh

set -euo pipefail

if [ -z "$1" ]; then
  echo "abort - parameter is empty"
  exit
fi

SCRIPT_PATH=$(readlink -f "$0")
SCRIPT_DIR=$(dirname "$SCRIPT_PATH")

(
  set -euo pipefail
  cd "$SCRIPT_DIR" || exit
  echo "tag: $1"

  git tag "$1"
  git push origin
  git push origin "$1"
  git push github
  git push github "$1"
  git push gitlab
  git push gitlab "$1"
  git push gitea
  git push gitea "$1"

  echo ""
  release_title="Release version $1"
  echo "$release_title"

  echo ""
  release_message="$(git log --format=%B -n 1 "$1")"
  echo "$release_message"

  release_file="../ffupdater/release/ffupdater-release.apk"

  echo "create release on GitHub:"
  gh release create "$1" "$release_file" --notes "$release_message" --title "$release_title"

  echo "create release on GitLab:"
  glab release create "$1" "$release_file" --notes "$release_message" --title "$release_title"

  startrepomaker
)
