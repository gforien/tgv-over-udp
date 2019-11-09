package com.ebgf.TGVOverUDP;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class ErreurMessageInattendu extends Exception {

    public String messageAttendu;
    public String messageRecu;

    public int ackAttendu;
    public int ackRecu;

    // Exception levée quand Serveur attend un certain message d'après le protocole
    // TCP (ex: ACK00003) mais que le message reçu ne respecte pas le protocole
    public ErreurMessageInattendu(String msgAttendu, String msgRecu) {
        super("message recu '"+msgRecu+"' au lieu du message attendu '"+msgAttendu+"'");

        // System.out.println("message recu '"+msgRecu+"' au lieu du message attendu '"+msgAttendu+"'");
        this.messageAttendu = msgAttendu;
        this.messageRecu    = msgRecu;
        this.ackAttendu     = -1;
        this.ackRecu        = -1;

        // si les messages en question sont des ACK, on les parse pour récupérer les numéros de seq
        try {
            if (messageAttendu.length() == Serveur.NBYTESEQ+3 && messageAttendu.substring(0,3).equals("ACK")) {
                this.ackAttendu = Integer.parseInt(messageAttendu.substring(3));
                // System.out.println("parsé ackAttendu = "+ackAttendu);
            }
            if (messageRecu.length() == Serveur.NBYTESEQ+3 && messageRecu.substring(0,3).equals("ACK")) {
                this.ackRecu = Integer.parseInt(messageRecu.substring(3));
                // System.out.println("parsé ackRecu = "+ackRecu);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}