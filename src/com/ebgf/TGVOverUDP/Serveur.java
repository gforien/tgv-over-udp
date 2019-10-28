package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;

public class Serveur {

    public static final int BUFFSIZE = 8192;

    // Variables de l'instance mère
    protected int portPublic;
    protected int connectionsMax;
    protected ServerSocket socketMere;

    // Variables des instances dédiées
    // elles doivent être ouvertes et fermées pour chaque client
    protected Socket client;
    protected int portDedie;
    protected String fichierDemande;
    protected BufferedInputStream bis;
    protected BufferedOutputStream bos;
    protected byte[] buffer;


    public Serveur(int i, int i2) throws IOException {
        this.portPublic = i;
        this.connectionsMax = i2;
        this.socketMere = new ServerSocket(portPublic, connectionsMax);
    }


    public void threeWayHandshake() {
        // on reçoit SYN sur le port public
        // on envoie SYN-ACK9999
        // on reçoit ACK sur le port public
        // on reçoit "13MoFile" sur son port dédié
        byte[] syn = new byte[3];
        byte[] synack = {'S', 'Y', 'N', '-', 'A', 'C', 'K', '3', '0', '0', '0'};
        byte[] ack = new byte[3];

        int n = 0;
        try {
            System.out.println("Waiting for client...");
            client = this.socketMere.accept();
            System.out.println("Client accepted !");
            bis = new BufferedInputStream(client.getInputStream());
            bos = new BufferedOutputStream(client.getOutputStream());

            if ((n = bis.read(syn)) != 3) {
                System.out.println("threeWayHandshake(): erreur au SYN !");
            }
            bos.write(synack);
            if ((n = bis.read(syn)) != 3) {
                System.out.println("threeWayHandshake(): erreur au ACK !");
            }

            bis.close();
            bos.close();
        }
        catch (IOException err) {
            err.printStackTrace();
        }
        finally {
        }

        // chaque message envoyé commence par 123456
        // le client acquitte avec ACK123456 sur son port dédié

        // à la fin du transfert, le client attend un FIN
    }

    public void sendFile(Socket clientSocket, String filename) {
        // protected Socket         cs = ss.accept();
        byte[] buffer = new byte[BUFFSIZE];

        try(
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
            BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream());
        ){
            while( bis.read(buffer) != -1 ) {
                bos.write(buffer);
            }
        } catch (FileNotFoundException err) {
            System.out.println("Fichier non trouve !");
            err.printStackTrace();
        } catch (IOException err) {
            err.printStackTrace();
        }
    }
}