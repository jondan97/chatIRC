package com.company.thread;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.UDPSocketService;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.List;

public class UDPThread extends Thread {
    //all the users on the server
    List<User> users;
    //all the chatrooms on the server
    List<Chatroom> chatrooms;
    //current client serving
    User currentUser;
    //mainly to send answers
    private DatagramChannel udpserver;
    //contains the data that were received from the sender
    private byte[] dataArray;
    //sender's details
    private InetSocketAddress clientNetworkDetails;


    public UDPThread(DatagramChannel udpserver, byte[] dataArray, InetSocketAddress receiverNetworkDetails, List<User> users, List<Chatroom> chatrooms) {
        this.udpserver = udpserver;
        this.dataArray = dataArray;
        this.clientNetworkDetails = receiverNetworkDetails;
        this.users = users;
        this.chatrooms = chatrooms;
    }

    @Override
    public void run() {
        try {
            for (User user : users) {
                //this detects if the user already exists, if he exists then set him as current user
                //this would be safer if I could know the mac-address but going into too much security
                //also for some reason some host names are saved in lowercase (no idea why) while they are
                //capitalized when first received
                if (clientNetworkDetails.getAddress().getHostName().toUpperCase().equals(user.getDetails().getHostName().toUpperCase())) {
                    currentUser = user;
                }
            }
            //this is the first part of the "packet" that is received, this allows
            //the server to distinguish between various functionalities, for example between
            //deleting or creating a chatroom (see the socket service class for more info)
            //NOTICE: most functionalities were moved under the TCP protocol, but the design of having an index was left
            int index = 0;
            //this is the second part of the packet, it could be any object from String to Chatroom
            Serializable messageReceived = null;
            ObjectInputStream iStream = null;
            try {
                //we convert the array back to the object, so we know what to do with it
                iStream = new ObjectInputStream(new ByteArrayInputStream(dataArray));
                index = iStream.readInt();
                messageReceived = (Serializable) iStream.readObject();
                //this exception always hits: the end of file is reached for the ObjectInputStream
                //and an exception is always thrown, so we need to catch it and handle it 'appropriately'
            } catch (EOFException e) {
                iStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (currentUser != null) {
                System.out.println("UDP request read from '" + currentUser.getUsername() + "'.");
            } else {
                System.out.println("UDP request read from an unknown user.");
            }
            //in case the object is a string:
            if (messageReceived instanceof String) {
                String messageReceivedString = (String) messageReceived;
                //request to see if server is up
                if (messageReceivedString.equals("/availability")) {
                    //answering to the client that indeed, the server is up
                    udpserver.send(UDPSocketService.newQuickConfirmationPacket("true"), clientNetworkDetails);
                }
                //server received an unknown message, so lets user know
                else {
                    //if we received a message from an unregistered user
                    if (currentUser != null) {
                        System.out.println("Received an unknown message '" + messageReceivedString + "' from user '" + currentUser.getUsername() + "'");
                        udpserver.send(UDPSocketService.newQuickConfirmationPacket("unknownMessage"), clientNetworkDetails);
                    }
                    //if we received an unknown message from an registered user
                    else {
                        System.out.println("Received an unknown message '" + messageReceivedString + "' from an unknown user.");
                        udpserver.send(UDPSocketService.newQuickConfirmationPacket("unknownMessage"), clientNetworkDetails);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
