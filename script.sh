#!/bin/bash
# set -x
# dd bs=1000 count=1000 </dev/urandom >1Mo
# cat 50Mo | nc -l -p 2000
# time netcat 127.0.0.1 2000 > copy
# sans pertes sur une machine en local => 360 Mb/s
TIMEFORMAT='%3R'
echo "MODE UNI-CLIENT"

ip="134.214.202.228"
port=2000
client=client2
taille=3

enBoucle=true
debugLevel=5

bufferSize=1200
timeout=1
# cwnd=39
# maxAckDuplique=3

make -f src/Makefile

if [[ $(pgrep java ) ]]; then
    echo "Serveur déjà lancé (PID $(pgrep java )) -> on le tue"
    kill $(pgrep java)
    sleep 3
fi
# on lance le serveur en tâche de fond
java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle &
sleep 1

echo -e "TCP Reno => cwnd variable\t maxAckDuplique = 3"
echo -e "$client :: fichier = ${taille}Mo"
echo -e "bufferSize = $bufferSize\ttimeout = $timeout\tfichier = ${taille}Mo"
echo "-------------------------------------------------------------"

for k in $(seq 1 10); do

    # on fait des moyennes sur 5 essais
    somme=0
    for i in $(seq 1 5); do
        (time ./bin/$client $ip $port ${taille}Mo 0) &> temp
        sleep 1

        t=$(cat temp 2>/dev/null | sed 's/,/./')
        Mb=$(echo "scale=2; 8*$taille / $t" | bc -l 2>/dev/null)
        somme=$(echo "scale=2; $somme + $Mb" | bc -l 2>/dev/null)
    done
    moyenne=$(echo "scale=2; $somme /5" | bc -l 2>/dev/null)

    # on calcule le débit
    echo -e "cwnd = $cwnd\t\tdebit = $moyenne Mb/s"
done

echo "-------------------------------------------------------------"
# Cleaner le repertoire
\rm -f temp* copy*
# rm -fv bin/fichier*
