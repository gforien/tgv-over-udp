package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Worker extends Serveur {

    private BufferedInputStream fluxFichier;

    private HashMap<Integer, byte[]> window;
    private int cwnd;
    private int seq;
    private int ssthresh;

    private int MAXBUFFSIZE = 1300;
    private int TIMEOUT = 5;

    public Worker(int port, String ip, int debugLevel, int bufferSize, int timeout) throws IOException {
        super(port, ip);
        this.debugColor = "\033[1;3"+((new Random()).nextInt(7) + 1)+"m";
        // this.debugColor = CYAN;
        this.debugLevel = debugLevel;

        log(3, "ip = "+ip+" port = "+port);
        log(3, "bufferSize = "+bufferSize+" timeout = "+timeout);
        this.window     = new HashMap<Integer, byte[]>(1000);
        this.cwnd       = 1;
        this.seq        = 1;
        this.ssthresh   = 1000;
        this.MAXBUFFSIZE = bufferSize;
        this.TIMEOUT = timeout;
        log("");
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
            log(3, "Fichier demandé: "+nomFichier);
            // (!)   le fichier doit exister dans le classpath ./bin   (!)

            this.fluxFichier = new BufferedInputStream(new FileInputStream("./bin/"+nomFichier));
            this.socket.setSoTimeout(TIMEOUT);



            /*************************************************************************************
                    (2) Gestion des paquets TCP
                            - on envoie tous les paquets de la congestion window
                            - on attend un acquittement du dernier paquet envoyé
                            - si on reçoit des ACK en double, ou pas d'ACK, on renvoie le paquet
            /*************************************************************************************/
            cwnd       = 39;
            int cwnd2  = 1;
            seq        = 1;
            int rwnd   = 1;
            ssthresh   = 5;
            int nBytesLus = 0;
            int dernierAckRecu = 0;
            int ackDupliques = 0;
            byte[] copieBuffer = new byte[MAXBUFFSIZE];
            byte[] seq2 = new byte[NBYTESEQ];

            int i = 0;
            double sommeCwnd = 0;
            int maxCwnd = cwnd;
            int pertesTimeout = 0;
            int pertesAck = 0;
            int nbTimeOut=0;

            Scanner scanner = new Scanner(System.in);

            while(nBytesLus != -1 || dernierAckRecu != (seq-1)) {

                //  (1) Envoi
                i++;
                sommeCwnd += cwnd;
                if (cwnd > maxCwnd) {
                    maxCwnd = cwnd;
                }
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

                //  (2) On attend un ACK du dernier paquet envoyé
                try {
                    initRecu(9);
                    recoitNonBloquant();
                    verifieRecu(dernierAckRecu+1);

                    //  (2.1) Tout se déroule comme prévu
                    dernierAckRecu++;
                    cwnd++;
                    if (cwnd < ssthresh) {
                        cwnd++;
                    } else {
                        cwnd2++;
                        if (cwnd2 >= cwnd) {
                            cwnd2 = 0;
                            cwnd++;
                        }
                    }
                    ackDupliques = 0;
                    log(2, "paquet = "+dernierAckRecu+" vérifié\t\t\tcwnd = "+cwnd+"\tssthresh = "+ssthresh);
                }

                catch (ErreurMessageInattendu e) {
                    //  (2.2) Plusieurs ACK aquittés d'un coup
                    if (e.ackRecu > dernierAckRecu+1) {
                        dernierAckRecu = e.ackRecu;
                        cwnd += e.ackRecu - dernierAckRecu+1 + 1;
                        log(2, "ackRecu supérieur = "+e.ackRecu+"\t\tcwnd = "+cwnd);
                    }

                    // (2.3) ACK dupliqué -> on renvoie le paquet
                    else if (e.ackRecu == dernierAckRecu) {

                        if (ackDupliques == 3) {
                            ackDupliques = 0;
                            //ssthresh = (rwnd<cwnd)? rwnd/2: cwnd/2;
                            cwnd = 39;
                            log(2, "ackRecu dupliqué = "+e.ackRecu+"\t\tcwnd = "+cwnd+"\t\tssthresh = "+ssthresh);

                            this.bufferEnvoi = window.get(dernierAckRecu+1);
                            this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
                            this.envoiBloquant();

                            System.arraycopy(bufferEnvoi,0, seq2, 0, NBYTESEQ);
                            log(2, "paquet "+new String(seq2, "UTF-8")+" renvoyé");
                        } else {
                            // on continue comme si de rien n'était
                            pertesAck++;
                            ackDupliques++;
                            log(2, "ackRecu dupliqué = "+e.ackRecu+"\t\t"+ackDupliques+" fois");
                        }
                    }

                    else {
                        log(2, "ackRecu inférieur = "+e.ackRecu+"\t\tcwnd = "+cwnd);
                    }
                }

                //  (2.4) Plus de réponse -> on renvoie un paquet
                catch (SocketTimeoutException e) {
                    pertesTimeout++;
                    nbTimeOut++;
                    //ssthresh = (rwnd<cwnd)? rwnd/2: cwnd/2;
                    cwnd = 39;
                    log(2, "pas de réponse du client !");
                    if (nbTimeOut==3){
                        this.bufferEnvoi = window.get(dernierAckRecu+1);
                        this.packetEnvoi = new DatagramPacket(this.bufferEnvoi, this.bufferEnvoi.length, this.addrClient, this.portClient);
                        this.envoiBloquant();
                        System.arraycopy(bufferEnvoi,0, seq2, 0, NBYTESEQ);
                        log(2, "paquet "+new String(seq2, "UTF-8")+" renvoyé");
                        nbTimeOut=0;
                    }

                    
                }
                log(2, "------------------------------------------------");
                //scanner.nextLine();
            }

            double moyenneCwnd = sommeCwnd / i;

            /*************************************************************************************
                    (3) Terminaison
            /*************************************************************************************/
            initEnvoiChaine("FIN");
            envoiBloquant();
            log(3, "FIN envoyé -> end()");
            System.out.println((pertesTimeout+pertesAck)+" "+pertesAck+" "+pertesTimeout+" "+maxCwnd+" "+moyenneCwnd);
        } catch (Exception e) {
            // (!) on peut recevoir une SocketException
            // (!) on peut recevoir une UnsupportedEncodingException
            e.printStackTrace();
            return;
        }
    }
}
