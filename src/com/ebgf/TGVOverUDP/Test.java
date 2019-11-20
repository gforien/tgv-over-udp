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
            int debugLevel = (args.length > 2)? Integer.parseInt(args[2]): 2;


            (new Thread(new Test.Pere(port, ip, debugLevel), "PERE")).start();
            //(new Thread(new Worker(port, ip), "worker")).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("usage: java com.ebgf.TGVOverUDP.Test  <ip>  <port>  <debugLevel>");
        }
    }


    public static class Pere extends Serveur {

        public int portDedie = 4983;
        public ExecutorService executeur = Executors.newFixedThreadPool(100);

        public Pere(int port, String ip, int debugLevel) throws IOException {
            super(port, ip);
            this.debugColor = VIOLET;
            this.debugLevel = debugLevel;
            log("ip = "+ip+" port = "+port);
            log("");
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

                    fils = new Worker(this.portDedie, this.ip, this.debugLevel);

                    initEnvoiChaine("SYN-ACK"+String.valueOf(this.portDedie));
                    envoiBloquant();
                    log(2, "SYN-ACK"+String.valueOf(this.portDedie)+" envoyé");

                    recoitBloquant();
                    verifieRecu("ACK");
                    log(2, "three-way handshake réussi");

                    executeur.execute(fils);
                    log(3, "Worker lancé sur le port "+this.portDedie);
                    this.portDedie++;
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