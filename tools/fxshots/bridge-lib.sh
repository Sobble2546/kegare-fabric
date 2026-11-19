# on VPS; source me (bridge stage)
export DISPLAY=:96
D=/opt/kegare-lab/shots/bridge; mkdir -p $D
FIFO=/opt/kegare-lab/work/bridge/run-server/in
S() { echo "$*" > $FIFO; }
K() { xdotool "$@"; }
# SH <prefix> <count> <gap seconds>: screen grabs prefix-1..n
SH() { p=$1; n=$2; g=${3:-0.1}; for i in $(seq 1 $n); do import -window root $D/$p-$i.png & sleep $g; done; wait; }
