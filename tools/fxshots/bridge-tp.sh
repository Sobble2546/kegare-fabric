source /opt/kegare-lab/work/bridge-lib.sh
S "time set 13200"; S "kill @e[type=pig]"; S "tp Tester 520.5 -60 520.5 0 8"; sleep 1
S "summon pig 520.5 -60 527.5 {NoAI:1b,Silent:1b}"
sleep 1
[ "$1" = f5 ] && { K key F5; sleep 1; }
