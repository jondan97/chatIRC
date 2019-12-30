package com.company.examples;/*
 * Filename: UDPEchoServer.java
 * Description: An echo server using connectionless delivery system (UDP).
 *              Receives character messages at a specified (hardcoded) port.
 *              The message is send back to the client capitalized.
 *              No error handling and exceptions are implemented.
 * Operation: java UDPEchoServer
 *
 * Author: Thanos Hatziapostolou
 * Date: 28-Feb-2005
 * Module Name: Data Communications and Networks
 * Module Number: CSD3420
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPEchoServer {
    public static void main(String argv[]) throws Exception {
        String capitalizedSentence = null;
        int port = 4567;
        int maxLength = 255;

        /* Create a receiving datagram data buffer */
        byte[] buffer = new byte[255];

        /* Create receiving datagram object of maximum size maxLength */
        DatagramPacket indatagram = new DatagramPacket(buffer, maxLength);

        /* Create a UDP socket on a specific port */
        DatagramSocket socket = new DatagramSocket(port);

        /* Display message that the UDP echo server is running */
        System.out.println("Starting a UDP Echo Server on port " + port);

        while (true) {
            /* Set the max length of datagram to 255 */
            indatagram.setLength(maxLength);

            /* Receive the datagram from the client  */
            socket.receive(indatagram);

            /* Convert the message from the byte array to a string array for displaying */
            String msgFromClient = new String(indatagram.getData(), 0, indatagram.getLength());

            /* Display the message on the screen */
            System.out.println("\nMessage received from " + indatagram.getAddress() + " from port "
                    + indatagram.getPort() + ".\nContent: " + msgFromClient);

            /* Capitalize the received message */
            capitalizedSentence = msgFromClient.toUpperCase() + '\n';

            /* Create a new datagram data buffer (byte array) for echoing capitalized message */
            byte[] msgToClient = capitalizedSentence.getBytes();

            /* Create an outgoing datagram by extracting the client's address and port from incoming datagram */
            DatagramPacket outdatagram = new DatagramPacket(msgToClient, msgToClient.length,
                    indatagram.getAddress(), indatagram.getPort());

            /* Send the reply back to the client */
            socket.send(outdatagram);
        }
    }
}

/*
 * Example:
 *   java UDPEchoServer
 * Output:
 *	 Starting a UDP Echo Server on port 4567:
 *   Message Received from: hatziapostolou/197.87.76.3 on port 1354
 *   Content: Hallo server
 */