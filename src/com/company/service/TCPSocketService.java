package com.company.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TCPSocketService {

    public static void sendObject(Object o, Socket server) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(server.getOutputStream());
        os.writeObject(o);
    }

    public static Object receiveObject(Socket server) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(server.getInputStream());
        return is.readObject();
    }
}
