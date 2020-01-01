package com.company.random.examples;/*
 * Searches for occupied UDP ports in the local machine
 *
 */

import java.io.IOException;
import java.net.DatagramSocket;

public class UDPPortScanner {
    public static void main(String[] args) {
        int startport = 1;
        int stopport = 65535;

        for (int port = startport; port <= stopport; port += 1) {
            try {
                DatagramSocket ds = new DatagramSocket(port);
                System.out.println("Started server on port: " + port);
                ds.close();
            } catch (IOException e) {
                System.out.println("Searching for free port....");
            }
        }
    }
}

