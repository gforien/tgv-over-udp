package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;
import java.util.*;

public class Test {

    public static void main(String[] args) throws IOException, UnknownHostException {

        try {

            int port = Integer.parseInt(args[0]);
            (new Thread(new Test.ThreadMere(port, "10.43.5.140"), "MERE")).start();
            //(new Thread(new Test.ThreadFils(port, "10.43.5.140"), "fils-"+port)).start();

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

        public ThreadMere(int port, String ip) throws IOException {
            super(port, ip);
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

                    fils = new Thread(new Test.ThreadFils(this.portDedie, "10.43.5.140"), "fils-"+String.valueOf(this.portDedie));

                    initEnvoiChaine("SYN-ACK"+String.valueOf(this.portDedie));
                    envoiBloquant();
                    log(2, "SYN-ACK"+String.valueOf(this.portDedie)+" envoyé");

                    recoitBloquant();
                    verifieRecu("ACK");
                    log(2, "three-way handshake réussi");

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

        HashMap<Integer, Boolean> window;
        int cwnd;
        BufferedInputStream fluxFichier;
        int dernierACKRecu;
        int seq;

        public ThreadFils(int port, String ip) throws IOException {
            super(port, ip);

            this.window     = new HashMap<Integer, Boolean>(100);
            this.cwnd       = 1;
            this.seq        = 1;
            
            this.debugColor = CYAN;
            this.debugLevel = 1;
        }

        @Override
        public void run() {

            try {
                initConnection();
                log(2, "Envoi du fichier");

                // this.debugLevel = 2;
                while(initEnvoiFichier() != -1) {
                    window.put(this.seq, false);
                    envoiBloquant();
                    initRecu(9);
                    recoitBloquant();
                    verifieRecu(this.seq);
                    this.seq++;
                }
                this.debugLevel = 1;

                initEnvoiChaine("FIN");
                envoiBloquant();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        public void initConnection() throws IOException {
            String nomFichier;
            // on reçoit un nom de fichier d'une taille quelconque
            log(2, "Début de la communication");
            initRecu(1000);
            recoitBloquant();
            initClientApresRecu();

            nomFichier = (new String(bufferRecu, "UTF-8")).trim();
            log(2, "Fichier demandé: "+nomFichier);
            // (!)   le fichier doit exister dans le classpath ./bin   (!)
            this.fluxFichier = new BufferedInputStream(new FileInputStream("./bin/"+nomFichier));
        }

        public int initEnvoiFichier() throws IOException {
            log("");
            this.bufferEnvoi = new byte[MAXBUFFSIZE];

            String s = String.valueOf(this.seq);
            while (s.length() < NBYTESEQ) {
                s = new String("0"+s);
                log(2, "s = "+s);
            }
            for (int i=0; i < NBYTESEQ; i++) {
                this.bufferEnvoi[i] = (byte)s.charAt(i);
                log("seq[] = "+String.valueOf((int)this.bufferEnvoi[i]));
            }
            int offset = NBYTESEQ-1;

            // // on remplit les 6 premiers octets du buffer
            // int seq2 = this.seq;
            // log("seq = "+this.seq);
            // for (int i=offset; i>=0; i--) {
            //     this.bufferEnvoi[i] = (byte)(seq2 %10);
            //     seq2 /= 10;
            //     log("seq[] = "+String.valueOf((int)this.bufferEnvoi[i]));
            // }

            // (!)  on doit avoir défini fluxfichier  (!)
            int n = fluxFichier.read(this.bufferEnvoi, NBYTESEQ, this.bufferEnvoi.length-NBYTESEQ);
            log(n+" bytes lus");

            this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
            log("");
            return n;
        }
    }

}

