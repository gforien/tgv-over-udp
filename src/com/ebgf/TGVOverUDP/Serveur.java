package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;

public abstract class Serveur implements Runnable {

    public static final int MAXBUFFSIZE = 8192;
    public static final int NBYTESEQ = 6;

    // attributs du serveur : définis par le constructeur
    protected int port;
    protected DatagramSocket socket;

    // attributs du clients : définis par initClientApresRecu()
    protected int portClient;
    protected InetAddress addrClient;

    // re-définis chaque fois que c'est nécessaire par initRecu() et initEnvoi()
    protected byte[] bufferRecu;
    protected byte[] bufferEnvoi;
    protected DatagramPacket packetRecu;
    protected DatagramPacket packetEnvoi;

    // fonctionnalités supplémentaires : ACK, n° seq, envoi de fichier
    protected int dernierACKRecu;
    protected int seq;
    protected boolean fichierEnCours;
    protected String nomFichier;
    protected BufferedInputStream fluxFichier;



    /******************************************************************************/
    //                   Méthodes à redéfinir selon le comportement voulu
    //
                        public abstract void initConnection();
                        public abstract void run();
    //
    /******************************************************************************/



    public Serveur(int port) throws IOException {
        this.port = port;
        this.socket = new DatagramSocket(this.port);
    }


    protected void initClientApresRecu() throws IOException {
        this.addrClient = this.packetRecu.getAddress();
        this.portClient = this.packetRecu.getPort();
        this.packetEnvoi.setAddress(this.addrClient);
        this.packetEnvoi.setPort(this.portClient);
    }





    // on se prépare à recevoir n bytes dans bufferRecu
    protected void initRecu(int tailleByte) {
        this.bufferRecu = new byte[tailleByte];
        this.packetRecu = new DatagramPacket(this.bufferRecu, this.bufferRecu.length);
    }

    protected void initEnvoi(boolean avecNumSeq, boolean envoiFichier, String messageOuNomFichier) throws FileNotFoundException, IOException{

        // (1) on définit la taille du message
        int tailleByte;
        if (envoiFichier) {
            tailleByte = (avecNumSeq)? MAXBUFFSIZE-NBYTESEQ: MAXBUFFSIZE;
        } else {
            tailleByte = (avecNumSeq)? messageOuNomFichier.length()+NBYTESEQ: messageOuNomFichier.length();
        }
        this.bufferEnvoi = new byte[tailleByte];
        this.packetEnvoi.setLength(this.bufferEnvoi.length);

        // (2) on remplit les NBYTESEQ premiers bytes avec seq, si besoin
        int offset = 0;
        if (avecNumSeq) {
            /*int puissance = 0;
            int seq2 = this.seq;
            while ((seq2/=10) != 0) puissance++;*/
            int seq2 = 1234;
            for (int i=NBYTESEQ-1; i>=0; i--) {
                bufferEnvoi[i] = (byte)(seq2 %10);
                seq2 /= 10;
                System.out.println(bufferEnvoi[i]);
            }
            offset = NBYTESEQ-1;
        }

        // (3) on charge le contenu
        if (envoiFichier && this.fichierEnCours) {
            fluxFichier.read(bufferEnvoi, offset, bufferEnvoi.length-offset);
        } else {
            for (int i=0; i<messageOuNomFichier.length(); i++) {
                bufferEnvoi[offset+i] = (byte)(messageOuNomFichier.charAt(i));
            }
        }
    }



    protected void recoitBloquant() throws IOException {
        this.socket.receive(this.packetRecu);
    }

    protected void envoiBloquant() throws IOException {
        this.socket.send(this.packetEnvoi);
    }

    // on compare bufferRecu au message qu'on était censés recevoir: s'ils ne sont pas identiques on lève une exception
    protected void verifieRecu(String messageAttendu) throws Exception {
        String messageRecu = new String(this.bufferRecu, "UTF-8");
        if (!messageAttendu.equals(messageRecu)) {
            throw new Exception("verifieRecu(): message recu '"+messageRecu+"' au lieu du message attendu '"+messageAttendu+"'");
        }
    }

    // surcharge
    protected void verifieRecu(int ackAVerifier) throws Exception {
        char[] seqChar = new char[NBYTESEQ];
        for (int i=NBYTESEQ-1; i>=0; i--) {
            seqChar[i] = (char)(ackAVerifier %10);
            ackAVerifier /= 10;
            System.out.println(seqChar[i]);
        }
        verifieRecu("ACK"+new String(seqChar));
    }
}