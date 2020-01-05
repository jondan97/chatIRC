package com.company.listener;

import com.company.entity.Message;
import com.company.entity.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */

public class MulticastListener extends Thread {
    //the datagram length shared across client (and server if adjusted)
    private int datagramPacketMaxLength;
    protected MulticastSocket socket;
    //needed in order to create the socket for listening
    private int multicastPort;
    //all the multicast addresses that the user is part of (all the chatrooms he is a member of)
    private ArrayList<InetAddress> multicastAddresses;
    //calling is .ready() command so that the application knows when the user is ready to see messages
    private BufferedReader userInput;
    //for all the users that requested to join a chatroom
    private List<Message> permissions;

    //constructor
    public MulticastListener(int multicastPort, List<Message> permissions, BufferedReader userInput, int datagramPacketMaxLength) {
        this.multicastPort = multicastPort;
        this.permissions = permissions;
        this.userInput = userInput;

        this.datagramPacketMaxLength = datagramPacketMaxLength;
    }

    public void run() {
        byte[] buf = new byte[datagramPacketMaxLength];
        multicastAddresses = new ArrayList<>();
        try {
            socket = new MulticastSocket(multicastPort);
            //address for permission notifications
            //this address is reserved for all users, users listen to this IP when a client asked for permission
            //to join any of the groups the user is owner of
            socket.joinGroup(InetAddress.getByName("239.0.0.0"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (true) {
            try {
                //awaiting for a multicasted datagram (in some cases it can also be unicasted)
                socket.receive(packet);
                //received it
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                //if packet data contain the following 'unique' substring, then the chatroom has been deleted
                //and the user should also forget about it
                if (received.contains("[{[FOR_DELETION]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    received = "Chatroom [" + arrOfStr[1] + "] has been deleted.";
                    InetAddress chatroomMulticastPort = InetAddress.getByName(arrOfStr[2]);
                    this.leaveChatroomMulticastAddress(chatroomMulticastPort);
                    //if packet data contain the following 'unique' substring, then the user was kicked
                    //by the owner and has to forget about it
                } else if (received.contains("[{[GOT_KICKED]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    received = "You were kicked from chatroom [" + arrOfStr[1] + "].";
                    InetAddress chatroomMulticastPort = InetAddress.getByName(arrOfStr[2]);
                    this.leaveChatroomMulticastAddress(chatroomMulticastPort);
                    //if packet data contain the following 'unique' substring, then the user was kicked because the
                    //chatroom was idle for too long and therefore got deleted
                } else if (received.contains("[{[FOR_IDLE]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    received = "Chatroom [" + arrOfStr[1] + "] was deleted because nobody was typing anything for too long.";
                    InetAddress chatroomMulticastPort = InetAddress.getByName(arrOfStr[2]);
                    this.leaveChatroomMulticastAddress(chatroomMulticastPort);
                    //if packet data contain the following 'unique' substring, then someone asked this client for
                    //permission to join their group, acceptance is handled as TCP
                } else if (received.contains("[{[PERMISSION_ASKED]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    Message permissionQuestion = new Message(arrOfStr[1], new User(arrOfStr[2]));
                    permissions.add(permissionQuestion);
                    received = "New permission by " + arrOfStr[2] + " requested to join [" + arrOfStr[1] + "]";
                    //if packet data contain the following 'unique' substring, then this is a notification that this
                    //client has been accepted to the group and can now read/send messages to that particular group
                } else if (received.contains("[{[PERMISSION_ACCEPTED]}]|><|")) {
                    String[] arrOfStr = received.split("\\|><\\|");
                    this.addChatroomMulticastAddress(InetAddress.getByName(arrOfStr[2]));
                    received = "You were accepted in [" + arrOfStr[1] + "].";
                }
                //waiting for user input to finish and then sending the message received
                userInput.ready();
                //already made message so we can print the value
                System.out.println(received);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

        }
    }

    //adds to the existing multicast list, one more IP
    //this IP represents a mmulticast IP that this thread listens to
    //takes an IP and returns nothing
    public void addChatroomMulticastAddress(InetAddress chatroomMulticastAddress) throws IOException {
        multicastAddresses.add(chatroomMulticastAddress);
        this.socket.joinGroup(chatroomMulticastAddress);
    }

    //removes from the existing multicast list, a particular IP
    //this happens when a user leaves a chatroom
    //takes an IP and returns nothing
    public void leaveChatroomMulticastAddress(InetAddress chatroomMulticastAddress) throws IOException {
        multicastAddresses.remove(chatroomMulticastAddress);
        this.socket.leaveGroup(chatroomMulticastAddress);
    }

    //for debugging mainly but user is allowed to see the multicast IP he has in his 'collection'
    //allows the user to see what is the multicast IP of all the chatrooms he is a member of
    //takes nothing, returns nothing
    public void showMulticast() {
        System.out.println("Multicast addresses:");
        for (InetAddress a : multicastAddresses) {
            System.out.println(a.getHostAddress());
        }
    }

    public MulticastSocket getSocket() {
        return socket;
    }

    public void setSocket(MulticastSocket socket) {
        this.socket = socket;
    }
}
