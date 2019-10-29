package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;

public abstract class Serveur implements Runnable {

    public static final int MAXBUFFSIZE = 8192;
    public static final int NBYTESEQ    = 6;

    public static final String RESET  = "\033[0m";
    public static final String NOIR   = "\033[1;30m";
    public static final String ROUGE  = "\033[1;31m";
    public static final String VERT   = "\033[1;32m";
    public static final String JAUNE  = "\033[1;33m";
    public static final String BLEU   = "\033[1;34m";
    public static final String VIOLET = "\033[1;35m";
    public static final String CYAN   = "\033[1;36m";
    public static final String BLANC  = "\033[1;37m";

    public int debugLevel = 1;
    public String debugColog = BLANC;
    public long retard;

    // attributs du serveur : définis par le constructeur
    protected int port;
    protected DatagramSocket socket;
    protected int seq;

    // attributs du clients : définis par initClientApresRecu()
    protected int portClient;
    protected InetAddress addrClient;

    // re-définis chaque fois que c'est nécessaire par initRecu() et initEnvoi()
    protected byte[] bufferRecu;
    protected byte[] bufferEnvoi;
    protected DatagramPacket packetRecu;
    protected DatagramPacket packetEnvoi;

    // fonctionnalités supplémentaires : à définir dans les classes filles qui veulent l'implémenter
    protected int dernierACKRecu;
    //protected boolean fichierEnCours;
    //protected String nomFichier;
    protected BufferedInputStream fluxFichier;



    /******************************************************************************/
    //                   Méthodes à redéfinir selon le comportement voulu
    //
                        public abstract void initConnection() throws Exception;
                        public abstract void run();
    //
    /******************************************************************************/



    public Serveur(int port) throws IOException {
        log("port = "+port);
        this.port       = port;
        this.socket     = new DatagramSocket(this.port);
        this.seq        = 1;
        log("");
    }

    public void log(int level, String msg, String couleur) {
        if(level < this.debugLevel)
            return;
        long start = System.currentTimeMillis();

        StringBuffer sb = new StringBuffer(100);
        String t = Thread.currentThread().getName();
        String m = Thread.currentThread().getStackTrace()[3].getMethodName();

        sb.append("[" + t + "] " + m + "()");
        for (int i=sb.length(); i<35; i++) sb.append(" ");

        System.out.println( couleur + sb + msg + RESET);
        this.retard += System.currentTimeMillis()-start;
    }
    public void log(int level, String msg) {
        if (level>1)
            log(level, msg, ROUGE);
        else
            log(level, msg, this.debugColog);
    }
    public void log(String msg) {
        log(1, msg, this.debugColog);
    }
    public void log(String msg, String couleur) {
        log(1, msg, couleur);
    }


    protected void initClientApresRecu() throws IOException {
        log("");
        this.addrClient = this.packetRecu.getAddress();
        this.portClient = this.packetRecu.getPort();
        log("client "+this.addrClient.toString()+":"+this.portClient);

        this.bufferEnvoi = new byte[2000];
        this.packetEnvoi = new DatagramPacket(bufferEnvoi, bufferEnvoi.length, this.addrClient, this.portClient);
        log("");
    }


    // on se prépare à recevoir n bytes dans bufferRecu
    protected void initRecu(int tailleByte) {
        log("tailleByte = "+tailleByte);
        this.bufferRecu = new byte[tailleByte];
        this.packetRecu = new DatagramPacket(this.bufferRecu, this.bufferRecu.length);
        log("");
    }

    protected void initEnvoiChaine(String message) {
        log("msg = "+message);
        this.bufferEnvoi = new byte[message.length()];
        this.packetEnvoi.setLength(this.bufferEnvoi.length);

        for (int i=0; i<this.bufferEnvoi.length; i++) {
            bufferEnvoi[i] = (byte)(message.charAt(i));
        }
        log("");
    }

    // attention, on doit avoir défini fluxfichier !
    protected int initEnvoiFichier() throws IOException{
        log("");
        this.bufferEnvoi = new byte[MAXBUFFSIZE-NBYTESEQ];
        this.packetEnvoi.setLength(this.bufferEnvoi.length);

        int offset = NBYTESEQ-1;
        int seq2 = this.seq;
        for (int i=offset; i>=0; i--) {
            this.bufferEnvoi[i] = (byte)(seq2 %10);
            seq2 /= 10;
            log(1, "initEnvoiFichier(): "+String.valueOf((char)this.bufferEnvoi[i]), VERT);
        }

        log("");
        return fluxFichier.read(this.bufferEnvoi, offset, this.bufferEnvoi.length-offset);
    }


    protected void recoitBloquant() throws IOException {
        log("");
        this.socket.receive(this.packetRecu);
        log("");
    }

    protected void envoiBloquant() throws IOException {
        log("");
        this.socket.send(this.packetEnvoi);
        log("");
    }



    // on compare bufferRecu au message qu'on était censés recevoir: s'ils ne sont pas identiques on lève une exception
    protected void verifieRecu(String messageAttendu) throws Exception {
        log("messageAttendu = "+messageAttendu);
        String messageRecu = new String(this.bufferRecu, "UTF-8");
        if (!messageAttendu.equals(messageRecu)) {
            throw new Exception("verifieRecu(): message recu '"+messageRecu+"' au lieu du message attendu '"+messageAttendu+"'");
        }
        log("");
    }

    // surcharge
    protected void verifieRecu(int ackAVerifier) throws Exception {
        log("ackAVerifier = "+ackAVerifier);
        char[] seqChar = new char[NBYTESEQ];
        for (int i=NBYTESEQ-1; i>=0; i--) {
            seqChar[i] = (char)(ackAVerifier %10);
            ackAVerifier /= 10;
        }
        verifieRecu("ACK"+new String(seqChar));
        log("");
    }
}