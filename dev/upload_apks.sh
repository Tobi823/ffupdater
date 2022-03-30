#!/bin/zsh

firefox https://gitlab.com/Tobiwan/ffupdater_gitlab/-/tags

firefox https://github.com/Tobi823/ffupdater/releases/new

(
  cd /home/hacker/Documents/Programme/repomaker/ || exit
  sudo systemctl start docker
  (
    sleep 5
    firefox http://localhost
  )&
  ./execute.sh
)