#!/bin/bash

# 1 - tout compiler
echo "make -f src/Makefile"
make -f src/Makefile

# 2 - Lancer le serveur
ip=$(hostname -I)
port=2000
if [[ $(pgrep java ) ]]; then
    echo "Serveur déjà lancé"
else
    echo "java -cp bin com.ebgf.TGVOverUDP.Test $ip $port"
    $(java -cp bin com.ebgf.TGVOverUDP.Test $ip $port) & > /dev/null 2>&1
fi

# 3 - Lancer les clients
echo "./client1 $ip $port 5Mo"
{ time ./bin/client1 $ip $port 5Mo 0; } 2>temp
t=$(cat temp | grep real | cut -f 2 | cut -d m -f 2 | cut -d s -f 1)
debit=$(echo "scale=2; 8*5 / $t" | bc -l)
echo -e "temps = $t s\t\tdebit = $debit Mb/s"

echo "./client2 $ip $port 5Mo"
{ time ./bin/client2 $ip $port 5Mo 0; } 2>temp
t=$(cat temp | grep real | cut -f 2 | cut -d m -f 2 | cut -d s -f 1)
debit=$(echo "scale=2; 8*5 / $t" | bc -l)
echo -e "temps = $t s\t\tdebit = $debit Mb/s"


rm -f temp