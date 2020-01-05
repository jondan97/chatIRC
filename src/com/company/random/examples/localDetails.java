package com.company.random.examples;/*
 * Filename: localDetails.java
 * Description: Finds the name and the IP Address of a local machine.
 * Operation: java localDetails
 *
 */

import java.net.InetAddress;
import java.net.UnknownHostException;

public class localDetails {
    public static void main(String[] args) {
        try {
            /* Create an InetAddress object for the local host. */
            InetAddress thisMachine = InetAddress.getLocalHost();
            /* Display the local host's name from the InetAddress object hostMachine */
            System.out.println("\nThis local machine's seems to be: " + thisMachine.getHostName());
            /* Display the local host's four byte IP address in dotted decimal string format */
            System.out.println("The IP Address is: " + thisMachine.getHostAddress() + "\n");
        }
        /* Catch exception if details can not be found */ catch (UnknownHostException e) {
            System.out.println("Failed to find local host's details.");
            return;
        }
    }
}

/*
 * Execution Example:
 *   java localDetails
 * Output:
 *	 This local machine's name is: mitsos-pc
 *	 The IP Address is: 127.0.0.1
 */