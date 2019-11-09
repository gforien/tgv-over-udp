package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Worker extends Serveur {

    public static final int MAXBUFFSIZE = 8192;
    public static final int TIMEOUT = 100;

    private BufferedInputStream fluxFichier;

    private HashMap<Integer, Boolean> window;
    private int cwnd;
    private int seq;
    private int ssthresh;

    public Worker(int port, String ip) throws IOException {
        super(port, ip);

        this.window     = new HashMap<Integer, Boolean>(100);
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
            while(nBytesLus != -1) {

                //  (2.1)
                while (window.size() < cwnd) {
                    nBytesLus = initEnvoiFichier();
                    envoiBloquant();
                    window.put(seq, false);
                    log(2, "paquet "+seq+" envoyé");
                    seq++;
                }
                
                //  (2.2)
                for(int seqAVerifier : window.keySet()) {
                    if (window.get(seqAVerifier) == true) continue;

                    //  (2.2.1)
                    try {
                        initRecu(9);
                        recoitNonBloquant();
                        verifieRecu(seqAVerifier);
                        window.replace(seqAVerifier, true);
                        log(2, "paquet "+seqAVerifier+" vérifié");
                        cwnd++;
                        log(2, "cwnd = "+cwnd);
                        // log(2, "paquet "+seqAVerifier+" retiré de la window");
                    }

                    //  (2.2.4)
                    catch (SocketTimeoutException e) {
                        ssthresh = cwnd/2;
                        cwnd = 1;
                        log(2, "paquet perdu ! cwnd = "+cwnd+" ssthresh = "+ssthresh);
                    }

                    catch (Exception e) {
                        log(2, "ACK reçu "+e.getMessage()+" n'est pas celui attendu");
                        try {
                            int ackRecu = Integer.parseInt(e.getMessage());
                            //  (2.2.2)
                            if (ackRecu >= seqAVerifier) {
                                log(2, "ackRecu = "+ackRecu);
                                window.replace(seqAVerifier, true);
                                log(2, "paquet "+seqAVerifier+" vérifié");
                            }
                            // (2.2.3)
                            else {
                                ssthresh = cwnd/2;
                                cwnd = 1;
                                log(2, "paquet perdu ! cwnd = "+cwnd+" ssthresh = "+ssthresh);
                            }
                        } catch (Exception e2) { e.printStackTrace();}
                    }
                }

                //  (2.3)
                // window.entrySet().removeIf(entry -> (new Boolean(true)).equals(entry.getValue()));
                Iterator<Map.Entry<Integer,Boolean>> iter = window.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Integer,Boolean> entry = iter.next();
                    if(entry.getValue() == true){
                        iter.remove();
                        log(2, "paquet "+entry.getKey()+" supprimé");
                    }
                }
            }

            initEnvoiChaine("FIN");
            envoiBloquant();

        } catch (Exception e) {
            // (!) on peut recevoir une SocketException
            e.printStackTrace();
            return;
        }
    }
}