package com.company.random.examples;/*
 * Filename: TCPEchoServer.java
 * Description: An echo server using connection-oriented delivery system (TCP).
 *              Receives character messages from clients which are capitalized
 *              and sent back. No error handling and exceptions are implemented.
 * Operation: java TCPEchoServer [port]
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

class TCPEchoServer {
    public static void main(String argv[]) throws Exception {
        String clientSentence, capitalizedSentence;
        int port;
        Socket connectionSocket = null;
        BufferedReader inFromClient;
        PrintStream outToClient;

        /* port is the argument passed to program */
        port = Integer.parseInt("4567");

        /* Create socket for port */
        ServerSocket welcomeSocket = new ServerSocket(port);

        System.out.println("Server waiting at port: " + welcomeSocket.getLocalPort());

        /* Wait endlessly for connections */
        while (true) {
            /* Accept the connection */
            connectionSocket = welcomeSocket.accept();
            System.out.println("Accepted connection from: " + connectionSocket.getInetAddress());

            /* Create a reading stream to the socket */
            inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            /* Create a writing stream to the socket */
            outToClient = new PrintStream(connectionSocket.getOutputStream());

            /* Wait endlessly for specific client to type messages */
            while (true) {
                clientSentence = null;
                try {
                    /* Read client's message through the socket's input buffer */
                    clientSentence = inFromClient.readLine();
                } catch (IOException e) {
                    System.out.println(connectionSocket.getInetAddress() + " broke the connection.");
                    break;
                }

                /* Output to screen the message received by the client */
                System.out.println("Message Received: " + clientSentence);

                /* If message is exit then terminate specific connection - exit the loop */
                if (clientSentence.equals("exit")) {
                    System.out.println("Closing connection with " + connectionSocket.getInetAddress() + ".");
                    break;
                }

                /* Capitalize the received message */
                capitalizedSentence = clientSentence.toUpperCase();

                /* Send it back through socket's output buffer */
                outToClient.println(capitalizedSentence);
            }
            /* Close input stream */
            inFromClient.close();

            /* Close output stream */
            outToClient.close();

            /* Close TCP connection with client on specific port */
            connectionSocket.close();

            /* Wait for more connections */
            System.out.println("Server waiting at port: " + welcomeSocket.getLocalPort());
        }
    }
}

/*
 * Example:
 *   java TCPEchoServer 4567
 * Output:
 *	 Server waiting at port 4567
 *	 Accepted connection from: machineName/IPaddress
 *   Message received: Hallo server
 */