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
import java.util.Map;

public class MulticastPublisher extends Thread {
    private DatagramSocket socket;
    private ArrayList<Chatroom> chatrooms;
    private Multimap<Chatroom, Message> pendingChatroomMessages;
    private ArrayList<User> users;

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

    public void run() {
        byte[] buf;
        Chatroom chatroom = null;
        Message message = null;
        //only used for the case of someone getting kicked
        boolean kicked = false;
        boolean permissionAsked = false;
        boolean accepted = false;
        while (true) {
            //for some reason, if you remove this, the thread sleeps(?) or something like that and it never checks if the multimap is empty, this is what I call  M A G I C
            this.isAlive();
            if (!pendingChatroomMessages.isEmpty()) {
                for (Map.Entry<Chatroom, Message> e : pendingChatroomMessages.entries()) {
                    InetAddress chatroomMulticastAddress = e.getKey().getMulticastAddress();
                    //so we don't have a ConcurrentModificationException
                    Iterator<Chatroom> iter = chatrooms.iterator();
                    while (iter.hasNext()) {
                        Chatroom wantedChatroom = iter.next();
                        //System.out.println(wantedChatroom.getName());
                        if (wantedChatroom.equals(e.getKey())) {
                            chatroom = e.getKey();
                            message = e.getValue();
                            //in this case, it's not a character but a full message (String) combined with chatroom name
                            //and sender name
                            String fullMessage = "";
                            if (e.getValue().getCharacter().contains("[{[FOR_DELETION]}]|><|")) {
                                fullMessage = "[{[FOR_DELETION]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                iter.remove();
                            } else if (e.getValue().getCharacter().contains("[{[GOT_KICKED]}]|><|")) {
                                fullMessage = "[{[GOT_KICKED]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                kicked = true;
                            } else if (e.getValue().getCharacter().contains("[{[PERMISSION_ASKED]}]|><|")) {
                                fullMessage = "[{[PERMISSION_ASKED]}]|><|" + e.getKey().getName() + "|><|" + e.getValue().getSender().getUsername();
                                permissionAsked = true;
                            } else if (e.getValue().getCharacter().contains("[{[PERMISSION_ACCEPTED]}]|><|")) {
                                fullMessage = "[{[PERMISSION_ACCEPTED]}]|><|" + e.getKey().getName() + "|><|" + e.getKey().getMulticastAddress().getHostAddress();
                                accepted = true;
                            } else {
                                fullMessage = "[" + e.getKey().getName() + "](" + e.getValue().getSender().getUsername() + "): " + e.getValue().getCharacter();
                            }
                            buf = fullMessage.getBytes();
                            if (permissionAsked) {
                                for (User multicastReceiver : users) {
                                    if (multicastReceiver.getUsername().toLowerCase().equals(e.getKey().getOwner().getUsername().toLowerCase())) {
                                        try {
                                            DatagramPacket packet
                                                    = new DatagramPacket(buf, buf.length, chatroomMulticastAddress, multicastReceiver.getMulticastPort());
                                            socket = new DatagramSocket();
                                            socket.send(packet);
                                            socket.close();
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                        wantedChatroom.getUsers().remove(message.getSender());
                                        permissionAsked = false;
                                        break;
                                    }
                                }
                            } else if (accepted) {
                                for (User multicastReceiver : users) {
                                    if (multicastReceiver.equals(e.getValue().getSender())) {
                                        try {
                                            DatagramPacket packet
                                                    = new DatagramPacket(buf, buf.length, InetAddress.getByName("239.0.0.0"), multicastReceiver.getMulticastPort());
                                            accepted = false;
                                            socket = new DatagramSocket();
                                            socket.send(packet);
                                            socket.close();
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                        wantedChatroom.getUsers().remove(message.getSender());
                                        permissionAsked = false;
                                        break;
                                    }
                                }
                            }
                            //turning the multicast feature into a unicast one by only sending the packet to the kicked user only in order to notify him
                            else if (kicked) {
                                for (User multicastReceiver : wantedChatroom.getUsers()) {
                                    //System.out.println("oops ;)" + multicastReceiver.getUsername());
                                    if (multicastReceiver.equals(e.getValue().getSender())) {
                                        //System.out.println("getting closer D:" + multicastReceiver.getUsername() + multicastReceiver.getMulticastPort());
                                        DatagramPacket packet
                                                = new DatagramPacket(buf, buf.length, chatroomMulticastAddress, multicastReceiver.getMulticastPort());
                                        try {
                                            socket = new DatagramSocket();
                                            socket.send(packet);
                                            socket.close();
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                        wantedChatroom.getUsers().remove(message.getSender());
                                        kicked = false;
                                        break;
                                    }
                                }
                            } else {
                                for (User multicastReceiver : wantedChatroom.getUsers()) {
                                    DatagramPacket packet
                                            = new DatagramPacket(buf, buf.length, chatroomMulticastAddress, multicastReceiver.getMulticastPort());
                                    try {
                                        socket = new DatagramSocket();
                                        socket.send(packet);
                                        socket.close();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                            //iter.remove();
                            break;
                        }
                    }
                }
                if (chatroom != null && message != null) {
                    pendingChatroomMessages.remove(chatroom, message);
                    System.out.println("A message was multicasted.");
                }
            }
        }
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public Multimap<Chatroom, Message> getPendingChatroomMessages() {
        return pendingChatroomMessages;
    }

    public void setPendingChatroomMessages(Multimap<Chatroom, Message> pendingChatroomMessages) {
        this.pendingChatroomMessages = pendingChatroomMessages;
    }

    public ArrayList<Chatroom> getChatrooms() {
        return chatrooms;
    }

    public void setChatrooms(ArrayList<Chatroom> chatrooms) {
        this.chatrooms = chatrooms;
    }
}
