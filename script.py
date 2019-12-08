#!/usr/bin/env python3
# coding: utf-8

"""
Script d'automatisation
https://stackoverflow.com/questions/4256107/running-bash-commands-in-python
http://queirozf.com/entries/python-3-subprocess-examples

En python + bash, interdit de :
    - chainer les commandes avec ; && ||
    - rediriger les flux avec > >> 2> &>
    - exécuter en parallèle avec &
 """
from subprocess import *
from time import sleep
from sys import argv
from socket import *



def serveur():
    global ip, port, debugLevel, nEchantillons, enBoucle, typeClient, nClients, taille, bufferSize, timeout, cwnd, maxAckDuplique
    # la socket
    ss = socket(AF_INET, SOCK_STREAM)
    ss.bind((ip, port))
    # ss.listen()
    # s = ss.accept()[0]

    # on compile + chmod
    check_call("make -sf src/Makefile".split(" "))
    check_call("chmod a+x bin/client1 bin/client2".split(" "))
    # test pour le fun
    # x = serveur_launch(s, 'client1', 1, 5, bufferSize, timeout, cwnd, maxAckDuplique)
    # print("débit recu %.2f" % (x))


    ## ALGORITHME DE RECHERCHE
    # nb blocs (dim, n) = (2**(n-1))**dim
    # parametres
    dim = ['bufferSize', 'cwnd', 'maxAckDuplique']
    var = {"bufferSize_min" : 100,
        "bufferSize_max" : 1400,
        "cwnd_min" : 1,
        "cwnd_max" : 10,
        "maxAckDuplique_min" : 0,
        "maxAckDuplique_max" : 10}
    cb = lambda a,c,d: serveur_launch(s, 'client1', 1, 5, a, 2, c, d)


    for d in dim :
        var[d+"_plage"] = var[d+"_max"] - var[d+"_min"]

    ## a chaque étape n = 1, 2, 3 on divise la plage en n blocs
    for n in range(1,3):

        print("n = "+str(n))
        print("")
        ## pour chaque dimension, on initialise le delta qui dépend de n
        ## et le compteur qui va varier de 0 à blocs-1
        for d in dim:
            delta = int(var[d+"_plage"]/2**(n-1))
            if delta < 1:
                print("ERREUR: dim["+d+"] etape("+n+") -> delta = "+delta)
                delta = 1
            var[d+"_delta"] = delta


        idx = [0 for k in dim]
        for i in range((2 **(n-1)) **len(dim)):
            print(str(idx))

            for k in range(len(dim)):
                # print("dimension "+dim[k]+": min="+str(var[dim[k]+"_min"])+" max="+str(var[dim[k]+"_max"])+" delta="+str(var[dim[k]+"_delta"]))
                # print("bloc "+str(idx[k])+" -> dTotal="+str(idx[k]*var[dim[k]+"_delta"]))
                var[dim[k]] = int(var[dim[k]+"_min"] + idx[k]*var[dim[k]+"_delta"] + (var[dim[k]+"_delta"]/2))
                print(str(var[dim[k]])+", ", end='')
            print()

            ## on met à jour les index
            idx[0] += 1
            for j in range(len(idx)):
                if idx[j] == 2**(n-1) and j+1 != len(idx):
                    idx[j+1] += 1
                    idx[j] = 0
            

        # for bloc in range(0, 2**(n-1))
        #     # dimension 1 en position de départ

        #     for d2 in [k for k in dim if k != d]

        #         for bloc in range(0, 2**(n-1))
        #             var[d2] = int(valeurMin + bloc*delta2 + delta2/2)

        #             cle  = [str(eval(var[k])) for k in dim].join(" ")
        #             x[cle] = cb(*[eval(var[k]) for k in dim])

    # s.close()






def main():
    global ip, port, debugLevel, nEchantillons, enBoucle, typeClient, nClients, taille, bufferSize, timeout, cwnd, maxAckDuplique
    # ip             = run(["hostname","-I"], stdout=PIPE, universal_newlines=True).stdout.split("\n")[0].split(" ")[0]
    ip             = "192.168.1.74"
    port           = 2000
    debugLevel     = 3
    nEchantillons  = 10

    enBoucle       = "false"
    typeClient     = "client1"
    nClients       = 1
    taille         = 5

    bufferSize     = 62000
    timeout        = 2
    cwnd           = 1
    maxAckDuplique = 3

    # traitement des paramètres
    assert 2<= len(argv) <=3
    assert argv[1] in ["client", "serveur"]
    if len(argv) == 3:
        assert argv[2] in ["scen1", "scen2", "scen3"]
    params = {"scen1": ("client1", "false"),
              "scen2": ("client2", "false"),
              "scen3": ("client1", "true")}
    typeClient, enBoucle = params[argv[2] if len(argv)==3 else "scen1"]
    eval(argv[1]+"()")

def serveur_launch(s, typeClient, nClients, taille, bufferSize, timeout, cwnd, maxAckDuplique):
    global ip, port, debugLevel, nEchantillons, enBoucle
    # on tue le process et on le relance avec les bons paramètres
    if run(["pgrep", "java"], stdout=PIPE).returncode == 0:
        pid = run(["pgrep", "java"], stdout=PIPE, universal_newlines=True).stdout.replace("\n", "")
        print("Process déjà lancé: PID "+pid+" -> kill")
        check_call(["kill", pid])
        sleep(1)
    elif "java" in run(["ps","-ax"], stdout=PIPE, universal_newlines=True).stdout:
        print("ERREUR: Process déjà lancé et intuable")
        exit(1)
    try:
        Popen(["java", "-cp", "bin" , "com.ebgf.TGVOverUDP.Test",
        ip, str(port), str(debugLevel), str(bufferSize), str(timeout), str(cwnd), str(maxAckDuplique), enBoucle])
    except UnicodeDecodeError as e:
        pass
    sleep(1)
    instructions = {"typeClient": typeClient, "nClients": nClients, "fichier" : str(taille)+"Mo"}
    print("Demande: %dx %s pour %dMo" % (nClients, typeClient, taille))
    s.send(str(instructions).encode())
    return float(s.recv(4096).decode())

def client():
    global ip, port
    # la socket
    s = socket(AF_INET, SOCK_STREAM)
    s.connect((ip, port))

    try:
        while True:
            d = eval(s.recv(4096).decode())
            # print("Lance: %dx %s pour %s" % (d["nClients"], d["typeClient"], d["fichier"]))
            client_cmd = ["time", "-f", "%e", "./bin/"+d["typeClient"], ip, str(port), d["fichier"], "0"]
            # for i in range(0,3):
            process = Popen(client_cmd, universal_newlines=True, stdout=PIPE, stderr=PIPE)
            out, err = process.communicate()
            sleep(1)
            t = float(err)
            print("Résultat: %.2f Mb/s" % (taille*8/t))
            s.send(str(taille*8/t).encode())

    except SyntaxError:
        s.close()

if __name__ == '__main__':
    try:
        main()
    except AssertionError:
        print("usage: ./script.py [client|serveur] scen[1|2|3]")
    except KeyboardInterrupt as e:
        print()
        exit(1)