package com.ebgf.TGVOverUDP;

import java.io.*;
import java.util.*;

public class Test {

    public static void main(String[] args) throws IOException {

        try {

            int port = Integer.parseInt(args[0]);
//            (new Thread(new Test.ThreadMere(port), "MERE")).start();
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
                this.fluxFichier = new BufferedInputStream(Test.class.getResourceAsStream(nomFichier));

                log(2, "Envoi du fichier");
                while(initEnvoiFichier() != -1) {
                    envoiBloquant();
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



/*

a implémenter

b & 0xFF

    
    // Variables utiles
    protected String nomFichier;
    protected BufferedInputStream fichier;
    protected InetAddress addrClient;
    protected int portClient;
    
    protected byte[] ack;
    protected byte[] buffer;

    public ServeurDedie(int i) throws IOException, FileNotFoundException {
        super(i);

        // on capte le fichier demandé
        byte[] fichierDemande = new byte[1000];
        packetRecu            = new DatagramPacket(fichierDemande, fichierDemande.length);
        // IOException
        socket.receive(packetRecu);

        addrClient = packetRecu.getAddress();
        portClient = packetRecu.getPort();
        nomFichier = new String(fichierDemande, "UTF-8");

        System.out.println("Fichier requis: "+nomFichier);
        // FileNotFoundException
        fichier = new BufferedInputStream(new FileInputStream(nomFichier));
            // si c'est le première fois qu'on appelle initEnvoi() pour ce fichier, il faut ouvrir le fichier
            if (!this.fichierEnCours) {
                this.fichierEnCours = true;
                this.nomFichier = messageOuNomFichier;
                this.fluxFichier = new BufferedInputStream(new FileInputStream(this.nomFichier));
            }


        // on prépare la transmission en connaissant le client
        buffer      = new byte[MAXBUFFSIZE];
        packetEnvoi = new DatagramPacket(buffer, MAXBUFFSIZE, addrClient, portClient);
        ack         = new byte[3];
        packetRecu  = new DatagramPacket(ack, ack.length);

                    while( bis.read(buffer) != -1 ) {
                bos.write(buffer);
    }
*/