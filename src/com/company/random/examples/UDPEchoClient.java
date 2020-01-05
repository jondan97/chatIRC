package com.company.random.examples;/*
 * Filename: UDPEchoClient.java
 * Description: An echo client using connectionless delivery system (UDP).
 *              Sends character messages to a server which are echoed capitalized.
 *              No error handling and exceptions are implemented.
 * Operation: java UDPEchoCLient [hostname] [port]
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPEchoClient {
    public static void main(String[] argv) throws Exception {
        int port, maxLength = 255;
        String hostname;
        String lineToServer, lineFromServer;

        /* First argument is the running server's name */
        hostname = "john-pc";

        /* Second argument is the port in which the server accepts connections */
        port = Integer.parseInt("4567");

        /* Determine the IP address of the server from the hostname */
        InetAddress serverAddr = InetAddress.getByName(hostname);

        while (true) {
            System.out.println("Type text to send to server: ");

            /* Create a buffer to hold the user's input */
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            /* Get the user's input */
            lineToServer = userInput.readLine();

            /* Stop infinite loop if user wants to stop getting echos by typing exit */
            if (lineToServer.equals("exit"))
                break;

            /* Create array of 255 bytes to hold outgoing message */
            byte[] data = new byte[maxLength];

            /* Convert the string message into bytes */
            data = lineToServer.getBytes();

            /* Create datagram to send to server specifying message, message length, server address, port */
            DatagramPacket outToServer = new DatagramPacket(data, data.length, serverAddr, port);

            /* Create a datagram socket through which the data will be send */
            DatagramSocket socket = new DatagramSocket();

            /* Send the datagram through the socket */
            socket.send(outToServer);

            /* Create array of 255 raw bytes to hold incomingmessage */
            byte[] response = new byte[maxLength];

            /* Create a datagram to receive from server specifying the message received */
            DatagramPacket inFromServer = new DatagramPacket(response, maxLength);

            /* Receive the echo datagram from server (capitalized) */
            socket.receive(inFromServer);

            /* Convert received byte array to string for displaying */
            lineFromServer = new String(inFromServer.getData(), 0, inFromServer.getLength());

            /* Output echoed message to the screen */
            System.out.println("Received: " + lineFromServer);
        }
    }
}

/*
 * Example:
 *   java UDPEchoClient 192.168.12.12 4567
 * Output:
 *	 Type text to send to server:
 *   Hallo server.
 *   Received: HALLO SERVER.
 */

/*** EXTRA INFORMATION ***/		
/*
DatagramPacket: it implements UDP Socket communication.
DatagramSocket: it is used for UDP communication.
BufferedReader: it supports input buffering. It provides the readLine() method for reading an entire line at a
                time from a stream.
InputStreamReader: reads a stream. It is used to convert between byte streams and character streams. It provides
                   a bridge between byte-oriented and character-oriented input streams.
*/