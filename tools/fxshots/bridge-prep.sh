source /opt/kegare-lab/work/bridge-lib.sh
S "time set ${1:-midnight}"; S "gamerule advance_time false"; S "gamerule doMobSpawning false"; S "weather clear"
S "kill @e[type=!player]"
S "kegare trait remove Tester six_eyes"
S "kegare innate set Tester ${2:-rime}"
S "kegare refinement set Tester 10"
S "kegare loadout reset Tester"
S "tp Tester 520.5 -60 520.5 0 0"
sleep 1
S "execute as Tester at @s run summon pig ^ ^ ^6 {NoAI:1b,Silent:1b}"
S "kegare cooldown reset Tester"; S "kegare malice fill Tester"
sleep 2
S "kegare info Tester"
sleep 1
tail -30 /opt/kegare-lab/logs/bridge-server.log | cut -c1-400
