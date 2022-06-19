#!/bin/zsh

if [ -z "$1" ]; then
  echo "abort - parameter is empty"
  exit
fi

set -euo pipefail
SCRIPT_PATH=$(readlink -f "$0")
SCRIPT_DIR=$(dirname "$SCRIPT_PATH")

(
  set -euo pipefail
  cd "$SCRIPT_DIR" || exit
  source /home/hacker/.zshrc
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
  glab release create "$1" "$release_file" --notes "$release_message" --name "$release_title"

  echo "start repomaker"
  set -euo pipefail
  cd /home/hacker/Documents/Programme/repomaker/

  if (! systemctl is-active --quiet docker ); then
    echo "start docker"
    sudo systemctl start docker
  else
    echo "docker is already running"
  fi

  (
    set -euo pipefail
    while [ -z "$(curl -Is http://localhost:80 | head -1)" ]; do
      sleep 0.1
    done
    xdg-open http://localhost
  ) &

  ./execute.sh
)
