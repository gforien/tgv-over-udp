package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Test {

    public static void main(String[] args) throws IOException, UnknownHostException {

        try {

            String ip = args[0];
            int port = Integer.parseInt(args[1]);

            (new Thread(new Test.ThreadMere(port, ip), "PERE")).start();
            //(new Thread(new Worker(port, ip), "worker")).start();
        } catch (Exception e) {
            System.out.println("usage: java com.ebgf.TGVOverUDP.Test  <ip>  <port>");
        }
    }


    public static class ThreadMere extends Serveur {

        public int portDedie = 4983;
        public ExecutorService executeur = Executors.newFixedThreadPool(100);

        public ThreadMere(int port, String ip) throws IOException {
            super(port, ip);
            this.debugColor = VIOLET;
            this.debugLevel = 2;
        }

        @Override
        public void run() {
            Runnable fils;

            try {
                while(true) {

                    log(2, "<< three-way handshake >>");
                    initRecu(3);                // on reçoit SYN ou ACK -> 3 caractères
                    recoitBloquant();
                    verifieRecu("SYN");
                    log(2, "SYN reçu");

                    initClientApresRecu();      // après avoir reçu un message on a bien les infos du client

                    fils = new Worker(this.portDedie, this.ip);

                    initEnvoiChaine("SYN-ACK"+String.valueOf(this.portDedie));
                    envoiBloquant();
                    log(2, "SYN-ACK"+String.valueOf(this.portDedie)+" envoyé");

                    recoitBloquant();
                    verifieRecu("ACK");
                    log(2, "three-way handshake réussi");

                    executeur.execute(fils);
                    //fils.start();
                    //arrayFils.add(fils);
                    this.portDedie++;
                    log("Worker lancé ! nouveau port dédié ="+this.portDedie);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            /*log(2, "On termine l'exécuteur");
            executeur.shutdown();
            while (!executeur.isTerminated()) {}
            log(2, "Tous threads fermés");*/
        }
    }
}