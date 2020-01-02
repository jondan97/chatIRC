package com.company.random.oldFilesIfNeeded;

import com.company.entity.Chatroom;
import com.company.entity.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Server {

    public static void main(String[] args) throws IOException {
        int port;
        int startPort = 1;
        int stopPort = 65535;
        int maxLength = 3500;
        InetAddress thisMachine = null;
        DatagramSocket datagramSocket = null;
        try {
            thisMachine = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("Server's IP Address is: " + thisMachine.getHostAddress() + "\n");

        for (port = startPort; port <= stopPort; port += 1) {
            try {
                System.out.println("Searching for a free port to start server on....");
                datagramSocket = new DatagramSocket(port);
                System.out.println("Started server on port: " + port);
                break;
            } catch (IOException e) {
            }
        }
        byte[] buffer = new byte[maxLength];
        ArrayList<User> users = new ArrayList<>();
        users.add(new User("ska"));
        ArrayList<Chatroom> chatrooms = new ArrayList<>();


        while (true) {
//            UDPThread t = new UDPThread(datagramSocket);
//            TCPThread h = new TCPThread(new Socket());
//            t.start();
//            h.start();
            //if we don't declare the datagram packet inside the loop, then in the next iteration of the while loop,
            //our datagram packet will be lost and the thread will be left with a null packet,
            //as the socket waits to receive a new datagram
            DatagramPacket receivedDatagram = new DatagramPacket(buffer, maxLength);
            datagramSocket.receive(receivedDatagram);
            new ServerThread(datagramSocket, receivedDatagram, users, chatrooms).start();
            //System.out.println("Handled a new packet.");
        }
    }
}
