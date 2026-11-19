#!/bin/bash
# on VPS (bridge stage). usage: bridge-ctl.sh prep|xvfb|server <secs>|client <secs>|cmd <console command>|kill
source /opt/kegare-lab/env.sh
export JAVA_HOME=$JAVA25_HOME
B=/opt/kegare-lab/work/bridge
LOG=/opt/kegare-lab/logs
PORT=25611
case "$1" in
prep)
  for d in run-server run-client; do
    [ -d $B/$d ] || { mkdir -p $B/$d; (cd /opt/kegare-lab/work/effects/$d && tar cf - --exclude=logs --exclude=in --exclude=crash-reports .) | tar xf - -C $B/$d; }
  done
  sed -i "s/^server-port=.*/server-port=$PORT/; s/^query.port=.*/query.port=$PORT/" $B/run-server/server.properties
  grep -q '^onboardAccessibility:false' $B/run-client/options.txt || echo 'onboardAccessibility:false' >> $B/run-client/options.txt
  sed -i 's/^pauseOnLostFocus:.*/pauseOnLostFocus:false/; s/^soundCategory_master:.*/soundCategory_master:0.0/' $B/run-client/options.txt
  grep -n 'vfxQuality' $B/run-client/config/kegare-client.toml; grep -n 'server-port' $B/run-server/server.properties; cat $B/run-server/eula.txt | tail -1 ;;
xvfb)
  pgrep -f 'Xvfb :96' >/dev/null || { nohup Xvfb :96 -screen 0 1280x720x24 >/dev/null 2>&1 & }; sleep 1; pgrep -fa 'Xvfb :96' ;;
server)
  cd $B/run-server; rm -f in; mkfifo in
  # keeps the console fifo open; it must not inherit the session's Gradle lock (fd 9), or the lock outlives the session
  ( exec 3<>in 9>&-; exec -a bridge-fifo-holder sleep ${2:-900} ) &
  timeout ${2:-900} $JAVA_HOME/bin/java -Dkegare.bridge.run=1 -Dfabric.dli.config=$B/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=server -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotServer @$B/build/loom-cache/argFiles/runServer -Xmx1200m -Dfile.encoding=UTF-8 net.fabricmc.devlaunchinjector.Main nogui < in > $LOG/bridge-server.log 2>&1 &
  echo server started ;;
client)
  cd $B/run-client
  export DISPLAY=:96 LIBGL_ALWAYS_SOFTWARE=1
  timeout ${2:-600} $JAVA_HOME/bin/java -Dkegare.bridge.run=1 -Dfabric.dli.config=$B/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=client -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient @$B/build/loom-cache/argFiles/runClient -Xmx2g -Dfile.encoding=UTF-8 net.fabricmc.devlaunchinjector.Main --username Tester --width 1280 --height 720 --quickPlayMultiplayer localhost:$PORT > $LOG/bridge-client.log 2>&1 &
  echo client started ;;
cmd)  shift; echo "$*" > $B/run-server/in ;;
kill)
  pkill -f 'kegare.bridge.run=1'; sleep 3; pkill -9 -f 'kegare.bridge.run=1'; pkill -f 'Xvfb :96'; pkill -f bridge-fifo-holder; sleep 1; pgrep -fa 'kegare.bridge.run|Xvfb :96|bridge-fifo-holder' | cut -c1-80 ;;
esac
