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
            int bufferSize = (args.length > 3)? Integer.parseInt(args[3]): 1000;
            int timeout = (args.length > 4)? Integer.parseInt(args[4]): 10;
            boolean enBoucle = (args.length > 5)? Boolean.parseBoolean(args[5]): true;

            (new Thread(new Test.Pere(port, ip, debugLevel, bufferSize, timeout, enBoucle), "PERE")).start();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("usage: java com.ebgf.TGVOverUDP.Test  <ip> <port> <debugLevel> <bufferSize> <timeout> <enBoucle>");
        }
    }


    public static class Pere extends Serveur {

        public int portDedie = 4983;
        public ExecutorService executeur = Executors.newFixedThreadPool(100);
        public int bufferSize = 1000;
        public int timeout = 10;
        public boolean enBoucle = true;

        public Pere(int port, String ip, int debugLevel, int bufferSize, int timeout, boolean enBoucle) throws IOException {
            super(port, ip);
            this.debugColor = BLANC;
            this.debugLevel = debugLevel;
            this.bufferSize = bufferSize;
            this.timeout = timeout;
            this.enBoucle = enBoucle;
            log(2, "ip = "+ip+" port = "+port+" bufferSize = "+bufferSize+" timeout = "+timeout+" enBoucle = "+enBoucle);
            log("");
        }

        @Override
        public void run() {
            Runnable fils;

            try {
                do {

                    log(2, "<< three-way handshake >>");
                    initRecu(3);                // on reçoit SYN ou ACK -> 3 caractères
                    recoitBloquant();
                    verifieRecu("SYN");
                    log(2, "SYN reçu");

                    initClientApresRecu();      // après avoir reçu un message on a bien les infos du client

                    fils = new Worker(this.portDedie, this.ip, this.debugLevel, this.bufferSize, this.timeout);

                    initEnvoiChaine("SYN-ACK"+String.valueOf(this.portDedie));
                    envoiBloquant();
                    log(2, "SYN-ACK"+String.valueOf(this.portDedie)+" envoyé");

                    recoitBloquant();
                    verifieRecu("ACK");
                    log(2, "three-way handshake réussi");

                    executeur.execute(fils);
                    log(2, "Worker lancé sur le port "+this.portDedie);
                    this.portDedie++;

                } while (this.enBoucle);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            log(2, "On termine l'exécuteur");
            executeur.shutdown();
            while (!executeur.isTerminated()) {}
            log(2, "Tous threads fermés");
        }
    }
}