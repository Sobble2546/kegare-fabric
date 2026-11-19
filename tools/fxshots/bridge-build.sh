#!/bin/bash
# on the VPS: gradle in the bridge dir inside the shared lock. usage: bridge-build.sh <logname> <gradle args...>
source /opt/kegare-lab/env.sh; export JAVA_HOME=$JAVA25_HOME; cd /opt/kegare-lab/work/bridge
L=/opt/kegare-lab/logs/$1.log; shift
flock /opt/kegare-lab/gradle.lock timeout 2400 ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx2500m "$@" > $L 2>&1
echo "EXIT $?" >> $L
