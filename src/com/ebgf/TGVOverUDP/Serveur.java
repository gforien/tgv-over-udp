package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;

public abstract class Serveur implements Runnable {

    public static final int MAXBUFFSIZE = 8192;
    public static final int NBYTESEQ    = 6;

    public static final String RESET  = "\033[0m";
    public static final String NOIR   = "\033[1;30m";
    public static final String ROUGE  = "\033[4;31m";
    public static final String VERT   = "\033[1;32m";
    public static final String JAUNE  = "\033[1;33m";
    public static final String BLEU   = "\033[1;34m";
    public static final String VIOLET = "\033[1;35m";
    public static final String CYAN   = "\033[1;36m";
    public static final String BLANC  = "\033[1;37m";

    public int debugLevel = 1;
    public String debugColor = BLANC;

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



    /******************************************************************************/
    //                   Méthodes à redéfinir selon le comportement voulu
    //
                        public abstract void run();
    //
    /******************************************************************************/

    public Serveur(int port, String ip) throws IOException, UnknownHostException {
        log("ip = "+ip+" port = "+port);
        this.port   = port;
        this.socket = (ip == null)? new DatagramSocket(this.port): new DatagramSocket(this.port, InetAddress.getByName(ip));
        log("");
    }
    public Serveur(int port) throws IOException, UnknownHostException {
        this(port, null);
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

        for (int i=0; i<this.bufferEnvoi.length; i++) {
            bufferEnvoi[i] = (byte)(message.charAt(i));
        }

        log("bufferEnvoi = "+bufferEnvoi);
        this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
        log("");
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
            log(2, "verifieRecu(): message recu '"+messageRecu+"' au lieu du message attendu '"+messageAttendu+"'");
            throw new Exception();
        } else {
            log("message vérifié");
        }
        log("");
    }

    // surcharge
    protected void verifieRecu(int ackAVerifier) throws Exception {
        log("ackAVerifier = "+ackAVerifier);

        String s = String.valueOf(ackAVerifier);
        while (s.length() < NBYTESEQ) {
            s = new String("0"+s);
            log(2, "s = "+s);
        }
        verifieRecu("ACK"+s);
        log("");
    }


    protected void initClientApresRecu() throws IOException {
        log("");
        this.addrClient = this.packetRecu.getAddress();
        this.portClient = this.packetRecu.getPort();
        log("client "+this.addrClient.toString()+":"+this.portClient);

        // on doit initialise bufferEnvoi avec une taille arbitraire uniquement pour initialiser packetEnvoi
        this.bufferEnvoi = new byte[2000];
        this.packetEnvoi = new DatagramPacket(bufferEnvoi, bufferEnvoi.length, this.addrClient, this.portClient);
        log("");
    }


    public void log(int level, String msg, String couleur) {
        if(level < this.debugLevel)
            return;

        StringBuffer sb = new StringBuffer(100);
        String t = Thread.currentThread().getName();
        String m = Thread.currentThread().getStackTrace()[3].getMethodName();

        sb.append("[" + t + "] " + m + "()");
        for (int i=sb.length(); i<35; i++) sb.append(" ");

        System.out.println( couleur + sb + msg + RESET);
    }
    public void log(int level, String msg) {
        if (level==2)
            log(level, msg, this.debugColor);
        else if (level==3)
            log(level, msg, ROUGE);
        else
            log(level, msg, BLANC);
    }
    public void log(String msg) {
        log(1, msg, BLANC);
    }
    public void log(String msg, String couleur) {
        log(1, msg, couleur);
    }
}