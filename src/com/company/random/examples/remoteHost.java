package com.company.random.examples;/*
 * Filename: remoteHost.java
 * Description: Finds the IP Address of a specified host.
 * Operation: java remoteHost [hostname]
 *
 */

import java.net.InetAddress;
import java.net.UnknownHostException;

public class remoteHost {
    public static void main(String[] args) {
        try {
            // Create an InetAddress object based on the host name the user supplied.
            InetAddress hostMachine = InetAddress.getByName("john-pc");
            // Display the host's name from the InetAddress object hostMachine
            System.out.println("\nHost name is: " + hostMachine.getHostName());
            // Display the host's four byte IP address in dotted decimal string format
            System.out.println("IP Address is: " + hostMachine.getHostAddress() + "\n");
        }

        // Catch exception if supplied host name is invalid.
        catch (UnknownHostException e) {
            System.out.println("Failed to find specified host.");
            return;
        }
    }
}

/*
 * Example:
 *   java remoteHost www.city.academic.gr
 * Output:
 *	 Host name is: www.city.academic.gr
 *	 IP Address is: 212.251.20.226
 */