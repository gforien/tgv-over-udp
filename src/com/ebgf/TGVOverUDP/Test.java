package com.ebgf.TGVOverUDP;

import java.io.*;

public class Test {

    public static void main(String[] args) throws IOException {
        try {
            Serveur s = new Serveur(Integer.parseInt(args[0]), 1);
            s.threeWayHandshake();
        }
        catch (NumberFormatException err) {
            System.out.println("usage: ./server server_port");
        }
        catch (ArrayIndexOutOfBoundsException err) {
            System.out.println("usage: ./server server_port");
        }
    }
}