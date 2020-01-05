package com.company.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */

//mainly used as a service that serves TCP functionalities
public class TCPSocketService {

    //sends an object to a receiver through TCP, in our case this is used both from client to server and server
    //to client takes an object and an 'IP' to sent to, returns nothing
    public static void sendObject(Object o, Socket socket) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        os.writeObject(o);
    }

    //awaits for an object to be send by a sender, in our case works both client to server and server to client
    //takes an 'IP' that something is expected to arrive (the connection), and returns the object received
    public static Object receiveObject(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
        return is.readObject();
    }
}
