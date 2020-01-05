package com.company.thread;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.User;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */
//this class reads notifications that were added to a multimap (pendingChatroomMessages) and multicasts it to multiple
//users, servers as a middle-man for chatroom members to communicate
//also used as a notification publisher, sending unicasted messages to users that need to be informed about something
//(kicked/accepted etc.)
public class MulticastPublisher extends Thread {
    //list of all the chatrooms in the application
    private List<Chatroom> chatrooms;
    //a multimap that contains the chatroom, and the message that needs to be multicasted to all the members
    private Multimap<Chatroom, Message> pendingChatroomMessages;
    //a list of all the users in the application
    private List<User> users;
    //a sort of 'custom latch' made for communication between chatroomActivityChecker thread and MulticastPublisher thread
    //the Checkers send a multimap key/value pair to the Publisher and then 'locks', waiting for the Publisher to finish with the pair.
    //When the Publisher finishes with the pair, it sets the latch to 0 and the Checker 'unlocks' and sets the next pair
    private AtomicInteger latch;

    //constructor
    public MulticastPublisher(List<Chatroom> chatrooms, Multimap<Chatroom, Message> pendingChatroomMessages, List<User> users, AtomicInteger latch) {
        this.chatrooms = chatrooms;
        this.pendingChatroomMessages = pendingChatroomMessages;
        this.users = users;
        this.latch = latch;
    }

    //takes a certain IP and calculates the next one
    //takes an IP
    //returns next IP
    public static String getNextIPV4Address(InetAddress ip) {
        String ipString = ip.getHostAddress();
        String[] nums = ipString.split("\\.");
        int i = (Integer.parseInt(nums[0]) << 24 | Integer.parseInt(nums[2]) << 8
                | Integer.parseInt(nums[1]) << 16 | Integer.parseInt(nums[3])) + 1;

        // If you wish to skip over .255 addresses.
        if ((byte) i == -1) i++;

        return String.format("%d.%d.%d.%d", i >>> 24 & 0xFF, i >> 16 & 0xFF,
                i >> 8 & 0xFF, i >> 0 & 0xFF);
    }

    @Override
    public void run() {
        byte[] buf;
        Chatroom chatroom = null;
        Message message = null;
        //only used for the case of someone getting kicked
        boolean kicked = false;
        //used in the case of someone asking permission
        boolean permissionAsked = false;
        //used in the case of someone accepting a permission
        boolean accepted = false;
        //used in the case of someone deleting a chatroom
        boolean deleted = false;
        //used in the case of the server deleting an idle chatroom
        boolean idle = false;
        while (true) {
            //for some reason, if you remove this, the thread sleeps(?) or something like that and it never checks if the multimap is empty, this is what I call  M A G I C
            this.isAlive();
            //this does nothing (presumably), but the authors decided to leave it as it is in order to show
            //that effort was taken for thread-safety (more like this existed but it they did not work etc.)
            synchronized (pendingChatroomMessages) {
                //if a chatroom message or a notification was added to the map
                if (!pendingChatroomMessages.isEmpty()) {
                    //turned this into an iterator because we were deleting at the same time
                    //but this was throwing a concurrent exception so the .remove() function was dropped but the
                    //normal iterator remained
                    Iterator mapIterator = pendingChatroomMessages.entries().iterator();
                    while (mapIterator.hasNext()) {
                        Map.Entry<Chatroom, Message> e = (Map.Entry<Chatroom, Message>) mapIterator.next();
                        InetAddress chatroomMulticastAddress = e.getKey().getMulticastAddress();
                        //same as the outer synchronization
                        //"so we don't have a ConcurrentModificationException", a failed attempt on not having the
                        //exception
                        synchronized (chatrooms) {
                            for (Chatroom wantedChatroom : new ArrayList<>(chatrooms)) {
                                if (wantedChatroom.equals(e.getKey())) {
                                    chatroom = e.getKey();
                                    message = e.getValue();
                                    //in this case, the message is not a single character (like in private chat)
                                    //but a full message (String) associated with chatroom name and sender name
                                    String fullMessage = "";
                                    //unique sequence + chatroom name + multicast ip of chatroom for user to delete
                                    if (e.getValue().getMessage().contains("[{[FOR_DELETION]}]|><|")) {
                                        fullMessage = "[{[FOR_DELETION]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                        deleted = true;
                                        //unique sequence + chatroom name + multicast ip of chatroom for user to delete
                                    } else if (e.getValue().getMessage().contains("[{[FOR_IDLE]}]|><|")) {
                                        fullMessage = "[{[FOR_IDLE]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                        deleted = true;
                                        idle = true;
                                        //unique sequence + chatroom name + multicast ip of chatroom for user to delete
                                    } else if (e.getValue().getMessage().contains("[{[GOT_KICKED]}]|><|")) {
                                        fullMessage = "[{[GOT_KICKED]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                        kicked = true;
                                        //unique sequence + chatroom name + username of the person who asked for permission
                                    } else if (e.getValue().getMessage().contains("[{[PERMISSION_ASKED]}]|><|")) {
                                        fullMessage = "[{[PERMISSION_ASKED]}]|><|" + e.getKey().getName() + "|><|" + e.getValue().getSender().getUsername();
                                        permissionAsked = true;
                                        //unique sequence + chatroom name + multicast ip of chatroom for user to add to their chatrooms
                                    } else if (e.getValue().getMessage().contains("[{[PERMISSION_ACCEPTED]}]|><|")) {
                                        fullMessage = "[{[PERMISSION_ACCEPTED]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                        accepted = true;
                                    } else {
                                        fullMessage = "[" + e.getKey().getName() + "](" + e.getValue().getSender().getUsername() + "): " + e.getValue().getMessage();
                                    }
                                    //get the message and put it into a package
                                    buf = fullMessage.getBytes();
                                    //what pathway condition will be chosen, depends on the boolean that accompanied
                                    //the unique sequences, for example for deletion, we also delete the
                                    //chatroom from the chatrooms list
                                    DatagramSocket socket = null;
                                    if (permissionAsked) {
                                        for (User multicastReceiver : new ArrayList<>(users)) {
                                            if (multicastReceiver.getUsername().toLowerCase().equals(e.getKey().getOwner().getUsername().toLowerCase())) {
                                                try {
                                                    DatagramPacket packet
                                                            = new DatagramPacket(buf, buf.length, chatroomMulticastAddress, multicastReceiver.getMulticastPort());
                                                    socket = new DatagramSocket();
                                                    socket.send(packet);
                                                    socket.close();
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                    socket.close();
                                                }
                                                permissionAsked = false;
                                                break;
                                            }
                                        }
                                    } else if (accepted) {
                                        for (User multicastReceiver : new ArrayList<>(users)) {
                                            if (multicastReceiver.equals(e.getValue().getSender())) {
                                                try {
                                                    //once again: 239.0.0.0 is the 'official' multicast address for users to receive notifications
                                                    DatagramPacket packet
                                                            = new DatagramPacket(buf, buf.length, InetAddress.getByName("239.0.0.0"), multicastReceiver.getMulticastPort());
                                                    accepted = false;
                                                    socket = new DatagramSocket();
                                                    socket.send(packet);
                                                    socket.close();
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                    socket.close();
                                                }
                                                //adding the user is done in the '/permissions' (server-side) section
                                                //wantedChatroom.getUsers().add(message.getSender());
                                                break;
                                            }
                                        }
                                    }
                                    //turning the multicast feature into a unicast one by only sending the packet to the kicked user in order to notify him
                                    else if (kicked) {
                                        for (User multicastReceiver : new ArrayList<>(wantedChatroom.getUsers())) {
                                            if (multicastReceiver.equals(e.getValue().getSender())) {
                                                DatagramPacket packet
                                                        = new DatagramPacket(buf, buf.length, chatroomMulticastAddress, multicastReceiver.getMulticastPort());
                                                try {
                                                    socket = new DatagramSocket();
                                                    socket.send(packet);
                                                    socket.close();
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                    socket.close();
                                                }
                                                wantedChatroom.getUsers().remove(message.getSender());
                                                kicked = false;
                                                break;
                                            }
                                        }
                                        //if chatroom needs to be deleted
                                    } else {
                                        for (User multicastReceiver : new ArrayList<>(wantedChatroom.getUsers())) {
                                            DatagramPacket packet
                                                    = new DatagramPacket(buf, buf.length, chatroomMulticastAddress, multicastReceiver.getMulticastPort());
                                            try {
                                                socket = new DatagramSocket();
                                                socket.send(packet);
                                                socket.close();
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                                socket.close();
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (chatroom != null && message != null) {
                        pendingChatroomMessages.remove(chatroom, message);
                        if (deleted) {
                            if (idle) {
                                //allow the other thread (chatroomActivityChecker) to send a new deletion request
                                //this makes concurrent modification of the multimap data structure less frequent
                                latch.set(0);
                            }
                            deleted = false;
                            chatrooms.remove(chatroom);
                            //reset the chatroom
                            chatroom = null;
                        }
                        //or unicasted, depends on the condition chosen by the application
                        System.out.println("A message was multicasted.");
                    }
                }
            }
        }
    }
}
