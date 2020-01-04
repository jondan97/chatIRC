package com.company.thread;

import com.company.entity.Message;
import com.company.entity.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MulticastReceiver extends Thread {
    protected MulticastSocket socket;
    protected byte[] buf = new byte[256];
    private int multicastPort;
    private ArrayList<InetAddress> multicastAddresses;
    private BufferedReader userInput;
    //for all the users that requested to join a chatroom
    private ArrayList<Message> permissions;

    public void run() {
        multicastAddresses = new ArrayList<>();
        try {
            socket = new MulticastSocket(multicastPort);
            //address for permissions notified
            socket.joinGroup(InetAddress.getByName("239.0.0.0"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (true) {
            try {
                socket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if (received.contains("[{[FOR_DELETION]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    received = "Chatroom [" + arrOfStr[1] + "] has been deleted.";
                    InetAddress chatroomMulticastPort = InetAddress.getByName(arrOfStr[2]);
                    this.leaveChatroomMulticastAddress(chatroomMulticastPort);
                } else if (received.contains("[{[GOT_KICKED]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    received = "You were kicked from chatroom [" + arrOfStr[1] + "].";
                    InetAddress chatroomMulticastPort = InetAddress.getByName(arrOfStr[2]);
                    this.leaveChatroomMulticastAddress(chatroomMulticastPort);
                } else if (received.contains("[{[PERMISSION_ASKED]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    Message permissionQuestion = new Message(arrOfStr[1], new User(arrOfStr[2]));
                    permissions.add(permissionQuestion);
                    received = "New permission by " + arrOfStr[2] + " requested to join [" + arrOfStr[1] + "]";
                } else if (received.contains("[{[PERMISSION_ACCEPTED]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    this.addChatroomMulticastAddress(InetAddress.getByName(arrOfStr[2]));
                    received = "You were accepted in [" + arrOfStr[1] + "].";
                }
                //waiting for user input to finish and then sending
                userInput.ready();
                System.out.println(received);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //perhaps safer methods but still unused
    public void addChatroomMulticastAddress(InetAddress chatroomMulticastAddress) throws IOException {
        multicastAddresses.add(chatroomMulticastAddress);
        this.socket.joinGroup(chatroomMulticastAddress);
    }

    public void leaveChatroomMulticastAddress(InetAddress chatroomMulticastAddress) throws IOException {
        multicastAddresses.remove(chatroomMulticastAddress);
        this.socket.leaveGroup(chatroomMulticastAddress);
    }

    //for debugging only
    public void showMulticast() {
        System.out.println("Multicast addresses:");
        for (InetAddress a : multicastAddresses) {
            System.out.println(a.getHostAddress() + ", " + a.getHostName());
        }
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public MulticastSocket getSocket() {
        return socket;
    }

    public void setSocket(MulticastSocket socket) {
        this.socket = socket;
    }

    public BufferedReader getUserInput() {
        return userInput;
    }

    public void setUserInput(BufferedReader userInput) {
        this.userInput = userInput;
    }

    public ArrayList<Message> getPermissions() {
        return permissions;
    }

    public void setPermissions(ArrayList<Message> permissions) {
        this.permissions = permissions;
    }
}
