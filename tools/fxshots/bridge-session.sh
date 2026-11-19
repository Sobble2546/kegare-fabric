#!/bin/bash
# on VPS: one Kegare server + one client with Photon FX under Xvfb :96, inside the shared Gradle lock. Ends when
# /opt/kegare-lab/work/bridge/stop appears or after $1 seconds (default 1500). Everything it started is killed at the end.
C=/opt/kegare-lab/work/bridge-ctl.sh
MAX=${1:-1500}
rm -f /opt/kegare-lab/work/bridge/stop
exec 9> /opt/kegare-lab/gradle.lock
flock 9
if pgrep -f 'fabric\.dli\.env=' >/dev/null; then echo "another game JVM is running" > /opt/kegare-lab/logs/bridge-session.txt; exit 1; fi
echo "locked $(date +%T)" > /opt/kegare-lab/logs/bridge-session.txt
bash $C xvfb
echo off > /opt/kegare-lab/work/bridge/gstate
bash $C server $MAX
for i in $(seq 1 60); do grep -q 'Done (' /opt/kegare-lab/logs/bridge-server.log 2>/dev/null && break; sleep 3; done
echo "server up $(date +%T)" >> /opt/kegare-lab/logs/bridge-session.txt
bash $C client $MAX
t=0; while [ ! -e /opt/kegare-lab/work/bridge/stop ] && [ $t -lt $MAX ]; do sleep 3; t=$((t+3)); done
bash $C kill
echo "ended $(date +%T)" >> /opt/kegare-lab/logs/bridge-session.txt
