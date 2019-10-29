package com.ebgf.TGVOverUDP;

import java.io.*;

public class Test {

    public static void main(String[] args) throws IOException {

        try {

            int port = Integer.parseInt(args[0]);

            Thread t = new Thread(new Test.ThreadMere(port), "MERE :"+String.valueOf(port));
            t.start();

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

        public int portDedie = 1234;

        public ThreadMere(int port) throws IOException {
            super(port);
            this.debugLevel = 1;
        }

        @Override
        public void initConnection() throws Exception {
            // three-way handshake
            initRecu(3);                // on reçoit SYN ou ACK -> 3 caractères
            recoitBloquant();
            verifieRecu("SYN");

            initClientApresRecu();      // après avoir reçu un message on a bien les infos du client

            /*bufferEnvoi = new byte[]{'S', 'Y', 'N', '-', 'A', 'C', 'K', '3', '0', '0', '0'};
            packetEnvoi = new DatagramPacket(bufferEnvoi, bufferEnvoi.length);*/

            initEnvoiChaine("SYN-ACK"+String.valueOf(portDedie));
            this.portDedie++;
            envoiBloquant();
            System.out.println("SYN-ACK envoyé");

            recoitBloquant();
            verifieRecu("ACK");
        }

        @Override
        public void run() {
            try {
                while(true) {
                    initConnection();
                    System.out.println(this.retard);


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
        }

        @Override
        public void initConnection() throws IOException, FileNotFoundException {

            // on reçoit un nom de fichier d'une taille quelconque
            initRecu(1000);
            recoitBloquant();
            initClientApresRecu();

            System.out.println("Fichier requis: "+new String(bufferRecu, "UTF-8"));
            this.fluxFichier = new BufferedInputStream(new FileInputStream(new String(bufferRecu, "UTF-8")));
        }

        @Override
        public void run() {
            try {
                initConnection();

                while(initEnvoiFichier() != -1) {
                    envoiBloquant();
                }

                initEnvoiChaine("FIN");
                envoiBloquant();
            } catch (Exception e) {
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