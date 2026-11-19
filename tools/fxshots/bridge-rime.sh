source /opt/kegare-lab/work/bridge-lib.sh
P=${1:-fp}
S "kegare cooldown reset Tester"; S "kegare malice fill Tester"; sleep 1
G=/opt/kegare-lab/work/bridge/gstate
[ "$(cat $G)" = on ] || { K key g; echo on > $G; sleep 1; }
# frost shard (slot 1)
K key 1; SH $P-shard 8 0.12
S "kegare cooldown reset Tester"; sleep 1.5
# lance: hold 2 (charge loop), release (flight + trail + impact)
K keydown 2; sleep 0.8; SH $P-charge 3 0.4; sleep 0.6
K keyup 2; SH $P-lance 10 0.1
sleep 1.5; S "kegare cooldown reset Tester"; sleep 1
# frost field (slot 3): cone
K key 3; SH $P-field 8 0.25
sleep 1; S "kegare cooldown reset Tester"; sleep 1
# rime shell (slot 4)
K key 4; SH $P-shell 6 0.3
sleep 1
tail -4 /opt/kegare-lab/logs/bridge-server.log | cut -c1-200
grep -c . /opt/kegare-lab/logs/bridge-client.log
