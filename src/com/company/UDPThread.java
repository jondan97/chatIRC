package com.company;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.SocketService;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;

public class UDPThread extends Thread {
    //all the users on the server
    ArrayList<User> users;
    //all the chatrooms on the server
    ArrayList<Chatroom> chatrooms;
    //current client serving
    User currentUser;
    //container (packet) that includes our data
    ByteBuffer byteBuffer;
    //mainly to send answers
    private DatagramChannel udpserver;
    //contains the data that were received from the sender
    private byte[] dataArray;
    //sender's details
    private InetSocketAddress clientNetworkDetails;
    //max length of packet that will be sent
    private int maxLengthOfPacket;

    public UDPThread(DatagramChannel udpserver, byte[] dataArray, InetSocketAddress receiverNetworkDetails, ArrayList<User> users, ArrayList<Chatroom> chatrooms) {
        super("UDPThread");
        this.udpserver = udpserver;
        this.dataArray = dataArray;
        this.clientNetworkDetails = receiverNetworkDetails;
        this.maxLengthOfPacket = 500;
        this.users = users;
        this.chatrooms = chatrooms;
    }

    @Override
    public void run() {
        try {
            for (User user : users) {
                //this detects if the user already exists, if he exists then set him as current user
                //ska is a test user
                //this would be safer if I could know the mac-address but going into too much security
                //also for some reason some host names are saved in lowercase (no idea why) while they are
                //capitalized when first received
                if (!user.getUsername().equals("ska") && clientNetworkDetails.getAddress().getHostName().toUpperCase().equals(user.getDetails().getHostName().toUpperCase())) {
                    currentUser = user;
                }
            }
            //this is the first part of the "packet" that is received, this allows
            //the server to distinguish between various functionalities, for example between
            //deleting or creating a chatroom (see the socket service class for more info)
            int index = 0;
            //this is the second part of the packet, it could be any object from String to Chatroom
            Serializable messageReceived = null;
            ObjectInputStream iStream = null;
            try {
                System.out.println(dataArray.length);
                //we convert the array back to the object, so we know what to do with it
                iStream = new ObjectInputStream(new ByteArrayInputStream(dataArray));
                index = iStream.readInt();
                messageReceived = (Serializable) iStream.readObject();
                //this exception always hits: the end of file is reached for the ObjectInputStream
                //and an exception is always thrown, so we need to catch it
            } catch (EOFException e) {
                iStream.close();
                if (currentUser != null) {
                    System.out.println("New message from a user '" + currentUser.getUsername() + "' received.");
                } else {
                    System.out.println("New message from a user received.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //in case the object is a string:
            if (messageReceived instanceof String) {
                String messageReceivedString = (String) messageReceived;
                System.out.println(index);
                System.out.println(messageReceived);
                //request to see if server is up
                if (messageReceivedString.equals("/availability")) {
                    //answering to the client that indeed, the server is up
                    udpserver.send(SocketService.newQuickConfirmationPacket("true"), clientNetworkDetails);
                    //request to check if a user is already registered
                } else if (messageReceivedString.equals("/isUserRegistered")) {
                    System.out.println("New user requested to register.");
                    if (currentUser != null) {
                        udpserver.send(SocketService.newQuickConfirmationPacket("true"), clientNetworkDetails);
                        System.out.println("User is already registered as '" + currentUser.getUsername() + "'");
                        udpserver.send(SocketService.newQuickConfirmationPacket(currentUser.getUsername()), clientNetworkDetails);
                    } else {
                        udpserver.send(SocketService.newQuickConfirmationPacket("false"), clientNetworkDetails);

                    }
                    //request to see all chatrooms
                } else if (messageReceivedString.equals("/showallchatrooms")) {
                    byte[] serializedAllChatrooms = SocketService.convertObjectToByteArray(chatrooms);
                    byteBuffer = ByteBuffer.wrap(serializedAllChatrooms);
                    udpserver.send(byteBuffer, clientNetworkDetails);
                    System.out.println("'" + currentUser.getUsername() + "' requested to see all chatrooms.");
                    //request to see all users
                } else if (messageReceivedString.equals("/showallusers")) {
                    byte[] serializedAllUsers = SocketService.convertObjectToByteArray(users);
                    byteBuffer = ByteBuffer.wrap(serializedAllUsers);
                    udpserver.send(byteBuffer, clientNetworkDetails);
                    System.out.println("'" + currentUser.getUsername() + "' requested to see all users.");
                }
                //request to see all users of a particular chatroom
                else if (index == 1) {
                    Chatroom requestedChatroom = new Chatroom();
                    for (Chatroom chatroom : chatrooms) {
                        if (chatroom.getName().equals(messageReceivedString)) {
                            requestedChatroom = chatroom;
                        }
                    }
                    byte[] serializedChatroomUsers = SocketService.convertObjectToByteArray(requestedChatroom.getUsers());
                    byteBuffer = ByteBuffer.wrap(serializedChatroomUsers);
                    udpserver.send(byteBuffer, clientNetworkDetails);
                    System.out.println("'" + currentUser.getUsername() + "' requested to see all users of chatroom '" + requestedChatroom.getName() + "'");
                }
                //request to register new user
                //this is where we first distinguish functionalities between objects
                //TO FUTURE AUTHORS, THE ABOVE (INDEX=1) WILL PROBABLY CHANGE TO TCP SO COMMENT STARTS HERE
                //this one requests to register new user
                //so the packet which is received contains the index (1) which is the first part
                //and the second part is the String object, which is the name of the user
                //so the server sees its a string, then it sees the index, and gets us here,
                //the String received is unknown up until this point,
                //a new user is created with the String received, and the network details are received from
                //the packet
                else if (index == 2) {
                    String username = messageReceivedString;
                    boolean alreadyExists = false;
                    for (User existingUser : users) {
                        if (username.toLowerCase().equals(existingUser.getUsername().toLowerCase())) {
                            udpserver.send(SocketService.newQuickConfirmationPacket("alreadyExists"), clientNetworkDetails);

                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        User newUser = new User(username, clientNetworkDetails.getAddress());
                        users.add(newUser);
                        System.out.println("New user registered as '" + newUser.getUsername() + "' (" + newUser.getDetails().getHostName() + ")");
                        udpserver.send(SocketService.newQuickConfirmationPacket("userAdded"), clientNetworkDetails);
                    }
                }
                //request to delete a chatroom
                //same here as above
                else if (index == 3) {
                    System.out.println("User '" + currentUser.getUsername() + "' requested to delete a chatroom.");
                    Chatroom toBeDeletedChatroom = new Chatroom();
                    toBeDeletedChatroom.setName(messageReceivedString);
                    toBeDeletedChatroom.setOwner(currentUser);
                    boolean chatroomDeleted = false;
                    for (Chatroom room : chatrooms) {
                        if (room.equals(toBeDeletedChatroom)) {
                            chatrooms.remove(toBeDeletedChatroom);
                            udpserver.send(SocketService.newQuickConfirmationPacket("chatroomDeleted"), clientNetworkDetails);

                            chatroomDeleted = true;
                            System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' has been deleted.");
                            break;
                        }
                    }
                    if (!chatroomDeleted) {
                        udpserver.send(SocketService.newQuickConfirmationPacket("chatroomNotDeleted"), clientNetworkDetails);
                        System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' does not exist or user is not the owner for it to be deleted.");
                    }
                }
                //server received an unknown message, so lets user know
                else {
                    System.out.println("Received an unknown message '" + messageReceivedString + "' from user '" + currentUser.getUsername() + "'");
                    udpserver.send(SocketService.newQuickConfirmationPacket("unknownMessage"), clientNetworkDetails);


                }
                //this also needs to be changed to TCP
            } else if (messageReceived instanceof Chatroom) {
                Chatroom newChatroom = (Chatroom) messageReceived;
                //if the user requested to create a chatroom
                if (index == 0) {
                    System.out.println("User '" + currentUser.getUsername() + "' requested to create a chatroom.");
                    boolean alreadyExists = false;
                    for (Chatroom room : chatrooms) {
                        if (room.getName().toLowerCase().equals(newChatroom.getName().toLowerCase())) {
                            udpserver.send(SocketService.newQuickConfirmationPacket("alreadyExists"), clientNetworkDetails);
                            alreadyExists = true;
                            System.out.println("Chatroom '" + newChatroom.getName() + "' already exists.");
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        newChatroom.setOwner(currentUser);
                        chatrooms.add(newChatroom);
                        System.out.println("Chatroom '" + newChatroom.getName() + "' has been created.");
                        udpserver.send(SocketService.newQuickConfirmationPacket("chatroomAdded"), clientNetworkDetails);
                    }
                }
                //if the user requested to delegate ownership (get ready for a wild trip!)
                else if (index == 2) {
                    System.out.println("User '" + currentUser.getUsername() + "' requested to delegate ownership of a chatroom.");
                    boolean ownershipDelegated = false;

                    // Strings are immutable so a normal for loop needs to be used here in order to
                    // get the real Chatroom instance (and not a copy of it) so that we can
                    // modify its string-properties
                    for (int i = 0; i < chatrooms.size(); i++) {
                        if (chatrooms.get(i).getName().equals(newChatroom.getName()) && currentUser.getUsername().equals(chatrooms.get(i).getOwner().getUsername())) {
                            newChatroom.setPassword(chatrooms.get(i).getPassword());
                            newChatroom.setPolicy(chatrooms.get(i).getPolicy());
                            newChatroom.setDeletionTime(chatrooms.get(i).getDeletionTime());
                            newChatroom.setKickTime(chatrooms.get(i).getKickTime());
                            newChatroom.setUsers(chatrooms.get(i).getUsers());
                            //basically we try to create a new Chatroom instance and not just
                            //copy the reference
                            //after thought: a constructor could be used that takes as a parameter
                            //the old chatroom #toolazy

                            //for the users that belong to the chatroom:
                            for (User user : newChatroom.getUsers()) {
                                //if the new owner is included in them, then replace him with the original user
                                //that as an extra contains the network details
                                if (user.getUsername().equals(newChatroom.getOwner().getUsername())) {
                                    newChatroom.setOwner(user);
                                    chatrooms.set(i, newChatroom);
                                    ownershipDelegated = true;
                                    break;
                                }
                            }

                        }
                    }
                    if (ownershipDelegated) {
                        udpserver.send(SocketService.newQuickConfirmationPacket("ownerChanged"), clientNetworkDetails);
                        System.out.println("Ownership of '" + newChatroom.getName() + "' has been successfully delegated from '" + currentUser.getUsername() + "' to '" + newChatroom.getOwner().getUsername() + "'.");
                    }
                    //this could be turned into a more user-friendly error message
                    //and not just contain 3 error messages at the same time (client gets more detailed error message
                    //but still complicated enough)
                    if (!ownershipDelegated) {
                        udpserver.send(SocketService.newQuickConfirmationPacket("clientNotOwner"), clientNetworkDetails);
                        System.out.println("Ownership of chatroom '" + newChatroom.getName() + "' failed to be delegated.");
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
