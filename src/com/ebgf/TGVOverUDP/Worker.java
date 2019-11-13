package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Worker extends Serveur {

    public static final int MAXBUFFSIZE = 8192;
    public static final int TIMEOUT = 1000;

    private BufferedInputStream fluxFichier;

    private HashMap<Integer, byte[]> window;
    private int cwnd;
    private int seq;
    private int ssthresh;

    public Worker(int port, String ip) throws IOException {
        super(port, ip);

        this.window     = new HashMap<Integer, byte[]>(1000);
        this.cwnd       = 1;
        this.seq        = 1;
        this.ssthresh   = 1000;
        
        this.debugColor = CYAN;
        this.debugLevel = 2;
    }

    private int initEnvoiFichier() throws IOException {
        log("");
        this.bufferEnvoi = new byte[MAXBUFFSIZE];

        String seqString = String.valueOf(this.seq);
        while (seqString.length() < NBYTESEQ) {
            seqString = new String("0"+seqString);
        }
        for (int i=0; i < NBYTESEQ; i++) {
            this.bufferEnvoi[i] = (byte)seqString.charAt(i);
        }
        log("buffer rempli avec seq = "+seqString);

        // (!)  on doit avoir défini fluxfichier  (!)
        int nBytesLus = fluxFichier.read(this.bufferEnvoi, NBYTESEQ, this.bufferEnvoi.length-NBYTESEQ);
        log("buffer rempli avec "+nBytesLus+" bytes du fichier");

        this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
        log("return nBytesLus = "+nBytesLus);
        return nBytesLus;
    }

    private void recoitNonBloquant() throws IOException {
        log("");
        // pour avoir un receive non bloquant on doit appeler socket.setSoTimeout(TIMEOUT);
        // TIMEOUT = 200 ms;
        // (!) receive bloque donc seulement 200 ms puis lève une SocketTimeoutException
        this.socket.receive(this.packetRecu);
        log("");
    }



    @Override
    public void run() {
        try {
            /*************************************************************************************
                    (1) Initialisation
                            - on reçoit le nom du fichier (on ne vérifie pas qu'il existe)
                            - on initialise le client (ip et port)
                            - on ouvre le fichier
                            - on met un timeout sur la socket en réception
            /*************************************************************************************/
            String nomFichier;
            
            initRecu(1000);                 // on reçoit un nom d'une taille quelconque -> 1000 caractères
            recoitBloquant();
            initClientApresRecu();

            nomFichier = (new String(bufferRecu, "UTF-8")).trim();
            log(2, "Fichier demandé: "+nomFichier);
            // (!)   le fichier doit exister dans le classpath ./bin   (!)

            this.fluxFichier = new BufferedInputStream(new FileInputStream("./bin/"+nomFichier));
            this.socket.setSoTimeout(TIMEOUT);



            /*************************************************************************************
                    (2) Gestion des paquets TCP
                            (2.1) on envoie tous les paquets de la congestion window
                                  on les ajoute à la HashMap en mode "false" = non acquittés

                            (2.2) on attend l'acquittement des paquets "false"
                                    (2.2.1) paquet acquitté -> on le passe en "true" et cwnd++ (Slow Start)
                                    (2 .2.2) on reçoit un ACK supérieur -> on acquitte tout ??
                                    (2.2.3) problème : on reçoit un ACK inférieur
                                    (2.2.4) problème : pas de réponse du client

                            (2.3) on enlève de la window les paquets "true" = acquittés
            /*************************************************************************************/
            cwnd       = 1;
            seq        = 1;
            ssthresh   = 1000;
            int nBytesLus = 0;
            int dernierAckRecu = 0;
            byte[] copieBuffer = new byte[MAXBUFFSIZE];
            byte[] seq2 = new byte[NBYTESEQ];

            Scanner scanner = new Scanner(System.in);

            while(nBytesLus != -1 || dernierAckRecu != (seq-1)) {

                //  (2.1) Envoi
                while (nBytesLus != -1 && dernierAckRecu+cwnd >= seq) {
                    nBytesLus = initEnvoiFichier();
                    envoiBloquant();

                    copieBuffer = new byte[MAXBUFFSIZE];
                    System.arraycopy(this.bufferEnvoi, 0, copieBuffer, 0, MAXBUFFSIZE);
                    window.put(seq, copieBuffer);

                    log(2, "paquet "+seq+" envoyé");
                    seq++;
                }
                log(2, "");

                //  (2.2) Réception
                //  (2.2.1) Tout va bien
                try {
                    initRecu(9);
                    recoitNonBloquant();
                    verifieRecu(dernierAckRecu+1);
                    dernierAckRecu++;

                    //log(2, "paquet "+dernierAckRecu+" vérifié");
                    cwnd++;
                    //log(2, "cwnd = "+cwnd);
                    log(2, "paquet = "+dernierAckRecu+" vérifié\t\tcwnd = "+cwnd);
                }

                catch (ErreurMessageInattendu e) {

                    //  (2.2.2)
                    if (e.ackRecu > dernierAckRecu+1) {
                        //log(2, "ackRecu supérieur = "+e.ackRecu);
                        dernierAckRecu = e.ackRecu;
                        cwnd += e.ackRecu - dernierAckRecu+1 + 1;
                        log(2, "ackRecu supérieur = "+e.ackRecu+"\t\tcwnd = "+cwnd);
                    }
                    // (2.2.3)
                    else if (e.ackRecu == dernierAckRecu) {
                        //log(2, "ackRecu dupliqué = "+e.ackRecu);
                        log(2, "ackRecu dupliqué = "+e.ackRecu+"\t\tcwnd = 1");
                        // ssthresh = cwnd/2;
                        cwnd = 1;
                        this.bufferEnvoi = window.get(dernierAckRecu+1);
                        this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
                        this.envoiBloquant();

                        System.arraycopy(bufferEnvoi,0, seq2, 0, NBYTESEQ);
                        try{
                            log(2, "paquet "+new String(seq2, "UTF-8")+" renvoyé");
                        } catch (Exception e3) {}
                        //log(2, "paquet perdu ! cwnd = "+cwnd+" ssthresh = "+ssthresh);
                    }

                    // On s'en bat bien bien les couilles
                    else {
                        //log(2, "ackRecu inférieur = "+e.ackRecu);
                        log(2, "ackRecu inférieur = "+e.ackRecu+"\t\tcwnd = "+cwnd);
                    }

                }

                //  (2.2.4)
                catch (SocketTimeoutException e) {
                    ssthresh = cwnd/2;
                    cwnd = 1;
                    //log(2, "paquet "+(dernierAckRecu+1)+" perdu ! pas de réponse");
                    log(2, "pas de réponse du client !");
                    //log(2, "seq = "+seq);
                    //log(2, "dernierAckRecu = "+dernierAckRecu);
                    //log(2, "cwnd = "+cwnd);

                    this.bufferEnvoi = window.get(dernierAckRecu+1);
                    this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
                    this.envoiBloquant();

                    System.arraycopy(bufferEnvoi,0, seq2, 0, NBYTESEQ);
                    try{
                        log(2, "paquet "+new String(seq2, "UTF-8")+" renvoyé");
                    } catch (Exception e3) {}
                }
                log(2, "------------------------------------------------");
                // scanner.nextLine();
            }

            initEnvoiChaine("FIN");
            envoiBloquant();
            log(2, "FIN envoyé -> end()");

        } catch (Exception e) {
            // (!) on peut recevoir une SocketException
            // (!) on peut recevoir une UnsupportedEncodingException
            e.printStackTrace();
            return;
        }
    }
}