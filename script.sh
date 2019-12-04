#!/bin/bash
# set -x
# dd bs=1000 count=1000 </dev/urandom >1Mo
# cat 50Mo | nc -l -p 2000
# time netcat 127.0.0.1 2000 > copy
# sans pertes sur une machine en local => 360 Mb/s
TIMEFORMAT='%3R'

MODE='UNI-CLIENT'
# MODE='MULTI-CLIENT'
echo "MODE $MODE"

#-------------------------------------------------------------------------
#
#       MODE 1 : le serveur se lance une fois et accepte un seul client
#                on peut tweaker les paramètres comme on veut
#
#-------------------------------------------------------------------------

chmod a+x bin/client*

ip="134.214.202.227"
port=2000
client=client1
taille=5

enBoucle=false
debugLevel=4

bufferSize=62000
timeout=3
cwnd=1
maxAckDuplique=3

make -f src/Makefile

if [[ $(pgrep java ) ]]; then
    echo "Serveur déjà lancé (PID $(pgrep java )) -> on le tue"
    kill $(pgrep java)
fi

echo -e "$client :: bufferSize = $bufferSize\ttimeout = $timeout\tfichier = ${taille}Mo"
echo -e "\t\t\tcwnd = $cwnd\tmaxAckDuplique = $maxAckDuplique"
echo "-------------------------------------------------------------"

for cwnd in $(seq 1 10 100); do

    # on lance le serveur en tâche de fond
    java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $cwnd $maxAckDuplique $enBoucle &
    sleep 1

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