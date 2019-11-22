#!/bin/bash
# set -x
# dd bs=1000 count=1000 </dev/urandom >1Mo
TIMEFORMAT='%3R'

MODE='1 CLIENT'
#MODE='MULTI-CLIENT'
echo "MODE $MODE"

if [[ $MODE == '1 CLIENT' ]]; then
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

        bufferSize=1000
        timeout=5
        client='client1'
        taille=10

        make -f src/Makefile

        if [[ $(pgrep java ) ]]; then
            echo "Serveur déjà lancé (PID $(pgrep java )) -> on le tue"
            kill $(pgrep java)
        fi

        for bufferSize in $(seq 10000 10000 60000); do

            # on lance le serveur en tâche de fond
            # echo "java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle &"
            java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle &
            sleep 1

            # on lance le client et on attend qu'il soit bien fini
            echo -n "./bin/$client $ip $port ${taille}Mo 0"
            (time ./bin/$client $ip $port ${taille}Mo 0) &> temp
            sleep 1

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
    debugLevel=4

    bufferSize=1000
    timeout=5
    client='client1'
    taille=10

    nbEssais=50

    # si le serveur tourne, on lance juste les clients
    if [[ $(pgrep java ) ]]; then
        echo "Serveur déjà lancé (PID $(pgrep java ))"

    else
        make -f src/Makefile
        echo "java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout"
        java -cp bin com.ebgf.TGVOverUDP.Test $ip $port $debugLevel $bufferSize $timeout $enBoucle
        exit
    fi


    for i in $(seq 1 $nbEssais); do
        # on crée des nouveaux fichiers
        if [[ ! -f "./bin/fichier$i" ]]; then
            cp -v "./bin/${taille}Mo" "./bin/fichier$i"
        fi

        echo "./bin/$client $ip $port fichier$i 0"
        #{ time ./bin/$client $ip $port fichier$i 0; } 2>temp$i &
        (time ./bin/$client $ip $port fichier$i 0) &> temp &
        sleep 0.25
    done

    echo -n "On attend le retour du serveur... "
    read

    tempsTotal=0
    for i in $(seq 1 $nbEssais); do
        echo -n "./bin/$client $ip $port fichier$i 0"
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

# Cleaner le repertoire
\rm -f temp* copy*
# rm -fv bin/fichier*