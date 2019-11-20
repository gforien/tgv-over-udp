#!/bin/bash
#set -x

# 1 - Récupérer IP + port
ip=$(hostname -I)
port=2000


# 2 - Si le serveur tourne, on lance juste les clients
if [[ $(pgrep java ) ]]; then
    echo "Serveur déjà lancé (PID $(pgrep java ))"

# 2 - Sinon c'est qu'on a arrêté le serveur pour des modifications donc on le recompile + relance
else
    echo "make -f src/Makefile"
    make -f src/Makefile
    echo "java -cp bin com.ebgf.TGVOverUDP.Test $ip $port 3"
    java -cp bin com.ebgf.TGVOverUDP.Test $ip $port 3
    exit
fi



# 3 - Tests de débit uni-client

client='client1'
taille='25'

# for client in 'client1' 'client2'; do
for taille in '5' '10' '25' '50'; do
    echo -n "./bin/$client $ip $port ${taille}Mo 0"
    { time ./bin/$client $ip $port ${taille}Mo 0; } 2>temp$i
    t=$(cat temp | grep real | cut -f 2 | cut -d m -f 2 | cut -d s -f 1 | sed 's/,/./')
    Mo=$(echo "scale=2; $taille / $t" | bc -l)
    Mb=$(echo "scale=2; 8*$taille / $t" | bc -l)
    echo -e "\t\tt = $t s\t\tdebit = $Mb Mb/s"
done


# 4 - Tests de débit multi-client

client='client1'
taille='25'
nbEssais=50

# for i in $(seq 1 $nbEssais); do
#     if [[ ! -f "./bin/fichier$i" ]]; then
#         cp -v "./bin/${taille}Mo" "./bin/fichier$i"
#     fi

#     # echo "./bin/$client $ip $port fichier$i 0"
#     { time ./bin/$client $ip $port fichier$i 0; } 2>temp$i &
#     sleep 0.25
# done

# echo -n "On attend le retour du serveur... "
# read

# tempsTotal=0
# for i in $(seq 1 $nbEssais); do
#     echo -n "./bin/$client $ip $port fichier$i 0"
#     t=$(cat temp$i 2>/dev/null | grep real | cut -f 2 | cut -d m -f 2 | cut -d s -f 1 | sed 's/,/./')
#     Mo=$(echo "scale=2; $taille / $t" | bc -l 2>/dev/null)
#     Mb=$(echo "scale=2; 8*$taille / $t" | bc -l 2>/dev/null)
#     tempsTotal=$(echo "$tempsTotal + $t" | bc -l 2>/dev/null)
#     echo -e "\t\tt = $t s\t\tdebit = $Mb Mb/s"
# done

# Mb=$(echo "scale=2; 8*$taille*$nbEssais / $tempsTotal" | bc -l 2>/dev/null)
# echo "TEMPS TOTAL = $tempsTotal s"
# echo "DEBIT MOYEN = $Mb Mb/s"


# 5 - Cleaner le repertoire
\rm -f temp* copy*
# rm -fv bin/fichier*