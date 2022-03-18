#!/bin/zsh

echo "$1"

git tag "$1"
git push origin
git push origin "$1"
git push github
git push github "$1"
git push gitlab
git push gitlab "$1"
git push gitea
git push gitea "$1"