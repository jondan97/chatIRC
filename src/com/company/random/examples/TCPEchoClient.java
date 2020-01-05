package com.company.random.examples;/*
 * Filename: TCPEchoClient.java
 * Description: An echo client using connection-oriented delivery system (TCP).
 *              Sends character messages to a server which are echoed capitalized.
 *              No error handling and exceptions are implemented
 * Operation: java TCPEchoClient [hostname] [port]
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

class TCPEchoClient {
    public static void main(String[] argv) throws Exception {
        String sentence = null, modifiedSentence = null, serverMachine;
        int port;

        /* The first argument is the server's name */
        serverMachine = "john-pc";

        /* The second argument the port that the server accepts connections */
        port = Integer.parseInt("4567");

        /* Create a buffer to hold the user's input */
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        /* Create the client socket according to the server's address and port */
        Socket clientSocket = new Socket(serverMachine, port);

        /* Display a connection established message  */
        System.out.println("Connected to: " + clientSocket.getInetAddress() + " on port " + port);

        /* Create a writing buffer to the socket */
        PrintStream outToServer = new PrintStream(clientSocket.getOutputStream());

        /* Create a reading buffer to the socket */
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        while (true) {
            System.out.println("Type message to send to server: ");

            /* Get user's input */
            sentence = inFromUser.readLine();

            /* Send the message to server */
            outToServer.println(sentence);

            /* Stop infinite loop if user wants to stop getting echos by typing exit */
            if (sentence.equals("exit"))
                break;

            /* Read the server's response */
            modifiedSentence = inFromServer.readLine();

            /* Display echoed message from server */
            System.out.println("Server returned: " + modifiedSentence);
        }

        System.out.println("Closing socket.");
        clientSocket.close();
    }
}

/*
 * Example:
 *   java TCPEchoClient machinename 4567
 * Output:
 *	 Connected to: machinename/IPaddress on port 4567
 *	 Type a message to send to server:
 *   Hallo server
 *   Server returned: HALLO SERVER
 */

/*** EXTRA INFORMATION ***/
/*
BufferedReader: it supports input buffering. It provides the readLine() method for reading an entire line at a
                time from a stream.
InputStreamReader: reads a stream. It is used to convert between byte streams and character streams. It provides
				   a bridge between byte-oriented and character-oriented input streams.
PrintStream: Prints to an output stream.
*/