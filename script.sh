#!/bin/bash
# set -x
# dd bs=1000 count=1000 </dev/urandom >1Mo
# cat 50Mo | nc -l -p 2000
# time netcat 127.0.0.1 2000 > copy
# sans pertes sur une machine en local => 360 Mb/s
TIMEFORMAT='%3R'

# MODE='UNI-CLIENT'
MODE='MULTI-CLIENT'
echo "MODE $MODE"

if [[ $MODE == 'UNI-CLIENT' ]]; then
        #-------------------------------------------------------------------------
        #
        #       MODE 1 : le serveur se lance une fois et accepte un seul client
        #                on peut tweaker les paramètres comme on veut
        #
        #-------------------------------------------------------------------------

        ip=$(hostname -I)
        port=2000
        enBoucle=false
        debugLevel=4

        bufferSize=65000
        timeout=3
        client=client1
        taille=5

        make -f src/Makefile

        if [[ $(pgrep java ) ]]; then
            echo "Serveur déjà lancé (PID $(pgrep java )) -> on le tue"
            kill $(pgrep java)
        fi

        echo -e "$client :: bufferSize = $bufferSize\ttimeout = $timeout\tfichier = ${taille}Mo"
        echo "-------------------------------------------------------------"

        for bufferSize in $(seq 50000 2000 65000); do

            # on lance le serveur en tâche de fond
            # echo "java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle &"
            java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle &
            sleep 1

            # on lance le client et on attend qu'il soit bien fini
            # echo -n "./bin/$client $ip $port ${taille}Mo 0"
            (time ./bin/$client $ip $port ${taille}Mo 0) &> temp
            sleep 1

            echo -en "bufferSize = $bufferSize\t\t"

            # on calcule le débit
            t=$(cat temp 2>/dev/null | sed 's/,/./')
            Mo=$(echo "scale=2; $taille / $t" | bc -l 2>/dev/null)
            Mb=$(echo "scale=2; 8*$taille / $t" | bc -l 2>/dev/null)
            echo -e "\t\tt = $t s\t\tdebit = $Mb Mb/s"
        done


elif [[ $MODE == 'MULTI-CLIENT' ]]; then
        #-------------------------------------------------------------------------
        #
        #       MODE 2 : le serveur tourne en tache de fond et accepte les clients
        #                on fixe les paramètres au début et on n'y touche plus
        #
        #-------------------------------------------------------------------------

    ip=$(hostname -I)
    port=2000
    enBoucle=true
    debugLevel=3

    bufferSize=60000
    timeout=3
    client='client1'
    taille=5

    nbEssais=20

    # si le serveur tourne, on lance juste les clients
    if [[ $(pgrep java ) ]]; then
        echo "Serveur déjà lancé (PID $(pgrep java ))"

    else
        make -f src/Makefile
        echo "java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout"
        java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle
        exit
    fi

    echo -e "$client :: bufferSize = $bufferSize\ttimeout = $timeout\tfichier = ${taille}Mo"
    echo "-------------------------------------------------------------"

    for i in $(seq 1 $nbEssais); do
        # on crée des nouveaux fichiers
        if [[ ! -f "./bin/fichier$i" ]]; then
            cp -v "./bin/${taille}Mo" "./bin/fichier$i"
        fi

        # echo "./bin/$client $ip $port fichier$i 0"
        echo "Envoi du fichier$i"
        #{ time ./bin/$client $ip $port fichier$i 0; } 2>temp$i &
        (time ./bin/$client $ip $port fichier$i 0) &> temp &
        sleep 0.25
    done

    echo -n "On attend le retour du serveur... "
    read

    tempsTotal=0
    for i in $(seq 1 $nbEssais); do
        # echo -n "./bin/$client $ip $port fichier$i 0"
        echo -en "Fichier$i\t\t"
        t=$(cat temp$i 2>/dev/null | sed 's/,/./')
        #t=$(cat temp$i 2>/dev/null | grep real | cut -f 2 | cut -d m -f 2 | cut -d s -f 1 | sed 's/,/./')
        Mo=$(echo "scale=2; $taille / $t" | bc -l 2>/dev/null)
        Mb=$(echo "scale=2; 8*$taille / $t" | bc -l 2>/dev/null)
        tempsTotal=$(echo "$tempsTotal + $t" | bc -l 2>/dev/null)
        echo -e "\t\tt = $t s\t\tdebit = $Mb Mb/s"
    done

    Mb=$(echo "scale=2; 8*$taille*$nbEssais / $tempsTotal" | bc -l 2>/dev/null)
    echo "TEMPS TOTAL = $tempsTotal s"
    echo "DEBIT MOYEN = $Mb Mb/s"
fi

echo "-------------------------------------------------------------"
# Cleaner le repertoire
\rm -f temp* copy*
# rm -fv bin/fichier*