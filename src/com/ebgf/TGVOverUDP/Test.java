package com.ebgf.TGVOverUDP;

import java.io.*;
import java.util.*;

public class Test {

    public static void main(String[] args) throws IOException {

        try {

            int port = Integer.parseInt(args[0]);
            // (new Thread(new Test.ThreadMere(port), "MERE")).start();
            (new Thread(new Test.ThreadFils(port), "fils-"+port)).start();

        } catch (NumberFormatException err) {
            System.out.println("usage: ./server server_port");
        } catch (ArrayIndexOutOfBoundsException err) {
            System.out.println("usage: ./server server_port");
        }
    }



/******************************************************************************************/
//                    Définition du comportement des différents threads
/******************************************************************************************/
    public static class ThreadMere extends Serveur {

        public int portDedie = 4983;
        ArrayList<Thread> arrayFils = new ArrayList<Thread>();

        public ThreadMere(int port) throws IOException {
            super(port);
            this.debugColor = VIOLET;
            this.debugLevel = 2;
        }

        @Override
        public void run() {
            Thread fils;

            try {
                while(true) {

                    log(2, "<< three-way handshake >>");
                    initRecu(3);                // on reçoit SYN ou ACK -> 3 caractères
                    recoitBloquant();
                    verifieRecu("SYN");
                    log(2, "SYN reçu");

                    initClientApresRecu();      // après avoir reçu un message on a bien les infos du client

                    initEnvoiChaine("SYN-ACK"+String.valueOf(this.portDedie));
                    envoiBloquant();
                    log(2, "SYN-ACK"+String.valueOf(this.portDedie)+" envoyé");

                    recoitBloquant();
                    verifieRecu("ACK");
                    log(2, "three-way handshake réussi");

                    fils = new Thread(new Test.ThreadFils(this.portDedie), "fils-"+String.valueOf(this.portDedie));
                    fils.start();
                    arrayFils.add(fils);
                    log(2, "-> fils lancé");
                    this.portDedie++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }


    public static class ThreadFils extends Serveur {
        public ThreadFils(int port) throws IOException {
            super(port);
            this.debugColor = CYAN;
            this.debugLevel = 1;
        }

        @Override
        public void run() {
            String nomFichier;

            try {
                // on reçoit un nom de fichier d'une taille quelconque
                log(2, "Début de la communication");
                initRecu(1000);
                recoitBloquant();
                initClientApresRecu();

                nomFichier = (new String(bufferRecu, "UTF-8")).trim();
                log(2, "Fichier demandé: "+nomFichier);
                // (!)   le fichier doit exister dans le classpath ./bin   (!)
                this.fluxFichier = new BufferedInputStream(new FileInputStream("./bin/"+nomFichier));

                log(2, "Envoi du fichier");
                // this.debugLevel = 2;
                while(initEnvoiFichier() != -1) {
                    envoiBloquant();
                    initRecu(9);
                    recoitBloquant();
                    verifieRecu(this.seq);
                }
                this.debugLevel = 1;

                initEnvoiChaine("FIN");
                envoiBloquant();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

}

