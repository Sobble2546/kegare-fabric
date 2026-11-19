#!/bin/bash
# sync kegare-fabric (working tree) to the VPS dir of the bridge stage
set -e
S='ssh -i /tmp/kegare-vps/id -o IdentitiesOnly=yes -o PasswordAuthentication=no -o BatchMode=yes'
rsync -a --delete -e "$S" --exclude .git --exclude build --exclude .gradle --exclude '/run' --exclude '/run-*' --exclude '/argf' --exclude __pycache__ /mnt/hdd/codes/kegare-fabric/ root@87.192.41.14:/opt/kegare-lab/work/bridge/
echo synced
