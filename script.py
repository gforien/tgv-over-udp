from subprocess import *
from time import sleep
"""
Script d'automatisation
https://stackoverflow.com/questions/4256107/running-bash-commands-in-python

En python + bash, interdit de :
    - chainer les commandes avec ; && ||
    - rediriger les flux avec > >> 2> &>
    - exécuter en parallèle avec &
 """

def main():
    ## Trucs chiants
    ip = run(["hostname","-I"], stdout=PIPE, universal_newlines=True).stdout.split("\n")[0].split(" ")[0]
    port           = 2000
    taille         = 5


    debugLevel     = 4
    client         = "client1"
    enBoucle       = "false"
    bufferSize     = 62000
    timeout        = 2
    cwnd           = 1
    maxAckDuplique = 3


    ## Encore les trucs chiants
    check_call("make -sf src/Makefile".split(" "))
    check_call("chmod a+x bin/client1 bin/client2".split(" "))
    if run(["pgrep", "java"], stdout=PIPE).returncode == 0:
        print("Process déjà lancé: PID "+cmd("pgrep java"))
    elif "java" in " ".join(cmd("ps -ax")):
        print("Process lancé là aussi")
    else:
        try:
            Popen(["java", "-cp", "bin" , "com.ebgf.TGVOverUDP.Test",
                  ip, str(port), str(debugLevel), str(bufferSize), str(timeout), str(cwnd), str(maxAckDuplique), enBoucle])
        except UnicodeDecodeError as e:
            pass
        sleep(1)

    client_cmd = ["time", "-f", "%e", "./bin/client1", ip, str(port), str(taille)+"Mo", "0"]

    # for i in range(0,3):
    process = Popen(client_cmd, universal_newlines=True, stdout=PIPE, stderr=PIPE)
    out, err = process.communicate()
    sleep(1)
    t = float(err)
    print("%d / %.2f = %.2f Mb/s" % (taille*8, t, taille*8/t))


def cmd(cmd):
    res = run(cmd.split(" "), check=True, universal_newlines=True, stdout=PIPE, stderr=PIPE)
    if res.stdout:
        arr = res.stdout.split("\n")
        arr = arr[0:len(arr)-1]
        return arr if len(arr)>1 else arr[0]

def cmd_to(cmd, nom_fichier):
    fichier = open(nom_fichier, "w")
    a = run(cmd.split(" "), check=True, universal_newlines=True, stdout=fichier, stderr=fichier)
    fichier.close()



if __name__ == '__main__':
    main()