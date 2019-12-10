#!/usr/bin/env python3
# coding: utf-8

"""
Script d'automatisation pour TGV-over-UDP

Liens utiles :
    https://stackoverflow.com/questions/4256107/running-bash-commands-in-python
    http://queirozf.com/entries/python-3-subprocess-examples

Gabriel Forien
INSA Lyon 4TC
 """
from subprocess import *
from time import sleep
from sys import argv
from socket import *

def main():
    global ip, port, debugLevel, nEchantillons, enBoucle, typeClient, nClients, taille, bufferSize, timeout, cwnd, maxAckDuplique
    ip             = "134.214.202.228"
    port           = 3000
    debugLevel     = 4
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


def serveur():
    global ip, port, debugLevel, nEchantillons, enBoucle, typeClient, nClients, taille, bufferSize, timeout, cwnd, maxAckDuplique

    # socket serveur
    ss = socket(AF_INET, SOCK_STREAM)
    ss.bind((ip, port))
    ss.listen()
    s = ss.accept()[0]

    # on compile + chmod
    check_call("make -sf src/Makefile".split(" "))
    check_call("chmod a+x bin/client1 bin/client2".split(" "))
    
    # test lambda
    debit = serveur_launch(s, 'client1', 1, 5, bufferSize, timeout, cwnd, maxAckDuplique)
    print("débit recu %.2f" % (debit))

    # algorithme de recherche
    precision = 2
    parametres = ['bufferSize', 'cwnd', 'timeout', 'maxAckDuplique']
    callback = lambda buf, cwind, time, maxAck: serveur_launch(s, 'client1', 1, taille, buf, time, cwind, maxAck)
    minMax = {  "bufferSize_min"     : 100,     "bufferSize_max" : 1490,
                "cwnd_min"           : 1,             "cwnd_max" : 10,
                "maxAckDuplique_min" : 0,   "maxAckDuplique_max" : 10,
                "timeout_min"        : 2,          "timeout_max" : 10}
    algo_recherche(precision, parametres, minMax, callback)

    s.close()
    ss.close()

def serveur_launch(s, typeClient, nClients, taille, bufferSize, timeout, cwnd, maxAckDuplique):
    global ip, port, debugLevel, nEchantillons, enBoucle
    # on tue le process et on le relance avec les bons paramètres
    # if run(["pgrep", "java"], stdout=PIPE).returncode == 0:
    #    pid = run(["pgrep", "java"], stdout=PIPE, universal_newlines=True).stdout.replace("\n", "")
    #    print("Process déjà lancé: PID "+pid+" -> kill")
    #    check_call(["kill", pid])
    #    sleep(1)
    # elif "java" in run(["ps","-ax"], stdout=PIPE, universal_newlines=True).stdout:
    #    print("ERREUR: Process déjà lancé et intuable")
    #    exit(1)
    try:
        Popen(["java", "-cp", "bin" , "com.ebgf.TGVOverUDP.Test",
        ip, str(port), str(debugLevel), str(bufferSize), str(timeout), str(cwnd), str(maxAckDuplique), enBoucle])
        ## ATTENTION ## sleep indispensable
        sleep(0.3)
        ##################################
    except UnicodeDecodeError as e:
        pass
    instructions = {"typeClient": typeClient, "nClients": nClients, "fichier" : str(taille)+"Mo"}
    s.send(str(instructions).encode())
    return float(s.recv(4096).decode())

def client():
    global ip, port
    while True:
        try:
            s = socket(AF_INET, SOCK_STREAM)
            s.connect((ip, port))

            while True:
                d = eval(s.recv(4096).decode())
                print("Lance: %dx %s pour %s" % (d["nClients"], d["typeClient"], d["fichier"]), end='')
                client_cmd = ["time", "-f", "%e", "./bin/"+d["typeClient"], ip, str(port), d["fichier"], "0"]
                process = Popen(client_cmd, universal_newlines=True, stdout=PIPE, stderr=PIPE)
                out, err = process.communicate()
                debit = taille*8/float(err)
                print("    ->  %.2f Mb/s" % (debit))
                s.send(str(debit).encode())

        except (SyntaxError, ConnectionRefusedError):
            print("Connection lost, try again..")
            s.close()
            sleep(3)


def algo_recherche(n, dim, var, cb):
    f = open("trace.log", "w")

    for d in dim :
        var[d+"_plage"] = var[d+"_max"] - var[d+"_min"]

    ## pour chaque dimension on initialise le delta
    for d in dim:
        delta = int(var[d+"_plage"]/2**(n-1))
        var[d+"_delta"] = 1 if delta < 1 else delta

    x = {}

    ## on boucle sur les blocs => un index par dimension, chacun variant de 0 à 2**(n-1)
    idx = [0 for x in dim]
    for i in range((2 **(n-1)) **len(dim)):
        print(str(idx))

        for k in range(len(dim)):
            var[dim[k]] = int(var[dim[k]+"_min"] + idx[k]*var[dim[k]+"_delta"] + (var[dim[k]+"_delta"]/2))
            print(str(var[dim[k]])+", ", end='')
        print()

        cle  = "\t".join([str(eval(str(var[x]))) for x in dim])
        x[cle] = cb(*[eval(str(var[x])) for x in dim])
        print("%s\t%s" % (cle, str(x[cle])), file=f)

        ## on met à jour les index
        idx[0] += 1
        for j in range(len(idx)):
            if idx[j] == 2**(n-1) and j+1 != len(idx):
                idx[j+1] += 1
                idx[j] = 0
    f.close()

if __name__ == '__main__':
    try:
        main()
    except AssertionError:
        print("usage: ./script.py [client|serveur] scen[1|2|3]")
    except KeyboardInterrupt as e:
        print()
        exit(1)
