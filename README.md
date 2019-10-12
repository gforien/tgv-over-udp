# TGV-over-udp :train2::satellite::globe_with_meridians:

![PRS TP3](https://img.shields.io/static/v1.svg?label=PRS&message=TP3&color=2aaee6&style=flat)
![License](https://img.shields.io/static/v1.svg?label=License&message=None&color=ff69b4&style=flat)
## :scroll: La légende d'un protocole pas comme les autres
À l'origine Elisa et Gabriel voulaient seulement réussir leur TP de PRS pour valider leur échange et continuer à chiller en TC.<br>
Après avoir écrit un petit programme **tcp-over-udp**, un soir d'octobre 2019 ils lancent des tests de débit avant de partir en vendredi chill boire du rhum. Encore jeunes et naïfs, ils sont loin d'imaginer l'aventure qui s'ouvre à eux...

Les résultats des tests arrivent quelques jours plus tard : Elisa et Gabriel comprennent qu'ils viennent d'inventer le protocole le plus rapide de l'histoire. Confrontés aux débits incroyable de leur programme, ils décident de quitter l'INSA pour monter leur start-up à San Francisco, après avoir renommé le projet **TGV-over-udp**<br>

En octobre 2019, leur start-up entre à la bourse de New York pour une valeur de **123,456,789 $**<br>
Le TGV est maintenant lancé, et il est en passe de conquérir le monde.<br>
**Montez en voiture, mais attention ! Un train peut en cacher un autre :fire:**

## :man_pilot: En voiture !
```bash
$ make
$ ./server 5001
$ ./client 127.0.0.1 5001
```

#
#### Elisa BOUVET - Gabriel FORIEN - INSA Lyon 4TC
![Logo INSA Lyon](https://upload.wikimedia.org/wikipedia/commons/b/b9/Logo_INSA_Lyon_%282014%29.svg)
