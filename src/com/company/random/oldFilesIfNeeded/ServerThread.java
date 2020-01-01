package com.company.random.oldFilesIfNeeded;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.SocketService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;


public class ServerThread extends Thread {
    DatagramPacket receivedPacket;
    DatagramSocket serverSocket;
    ArrayList<User> users;
    ArrayList<Chatroom> chatrooms;
    User currentUser;

    public ServerThread(DatagramSocket serverSocket, DatagramPacket receivedPacket, ArrayList<User> users, ArrayList<Chatroom> chatrooms) {
        this.receivedPacket = receivedPacket;
        this.serverSocket = serverSocket;
        this.users = users;
        this.chatrooms = chatrooms;
    }

    @Override
    public void run() {
        try {
            for (User user : users) {
                //this would be safer if I could know the mac-address but going into too much security
                //also for some reason some host names are saved in lowercase (no idea why)
                if (!user.getUsername().equals("ska") && receivedPacket.getAddress().getHostName().toUpperCase().equals(user.getDetails().getHostName().toUpperCase())) {
                    currentUser = user;
                }
            }
            int index = 0;
            Serializable messageReceived = null;
            try {
                //we convert the array back to the object, so we know what to do with it
                System.out.println(receivedPacket.getLength());
                ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(receivedPacket.getData()));
                index = iStream.readInt();
                messageReceived = (Serializable) iStream.readObject();
                iStream.close();
            } catch (EOFException e) {
                if (currentUser != null) {
                    System.out.println("New message from a user '" + currentUser.getUsername() + "' received.");
                } else {
                    System.out.println("New message from a user received.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (messageReceived instanceof String) {
                String messageReceivedString = (String) messageReceived;
                //request to see if server is up
                if (messageReceivedString.equals("/availability")) {
                    serverSocket.send(SocketService.quickConfirmationPacket("true", receivedPacket.getAddress()
                            , receivedPacket.getPort()));
                    //request to check if a user is already registered
                } else if (messageReceivedString.equals("/isUserRegistered")) {
                    System.out.println("New user requested to register.");
                    if (currentUser != null) {
                        serverSocket.send(SocketService.quickConfirmationPacket("true", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                        System.out.println("User is already registered as '" + currentUser.getUsername() + "'");
                        serverSocket.send(SocketService.quickConfirmationPacket(currentUser.getUsername(), receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                    } else {
                        serverSocket.send(SocketService.quickConfirmationPacket("false", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                    }
                    //request to see all chatrooms
                } else if (messageReceivedString.equals("/showallchatrooms")) {
                    byte[] serializedAllChatrooms = SocketService.convertObjectToByteArray(chatrooms);
                    DatagramPacket chatroomsPacket = new DatagramPacket(serializedAllChatrooms, serializedAllChatrooms.length,
                            receivedPacket.getAddress(), receivedPacket.getPort());
                    serverSocket.send(chatroomsPacket);
                    System.out.println("'" + currentUser.getUsername() + "' requested to see all chatrooms.");
                    //request to see all users
                } else if (messageReceivedString.equals("/showallusers")) {
                    byte[] serializedAllUsers = SocketService.convertObjectToByteArray(users);
                    DatagramPacket usersPacket = new DatagramPacket(serializedAllUsers, serializedAllUsers.length,
                            receivedPacket.getAddress(), receivedPacket.getPort());
                    serverSocket.send(usersPacket);
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
                    DatagramPacket chatroomUsersPacket = new DatagramPacket(serializedChatroomUsers, serializedChatroomUsers.length,
                            receivedPacket.getAddress(), receivedPacket.getPort());
                    serverSocket.send(chatroomUsersPacket);
                    System.out.println("'" + currentUser.getUsername() + "' requested to see all users of chatroom '" + requestedChatroom.getName() + "'");
                }
                //request to register new user
                else if (index == 2) {
                    String username = messageReceivedString;
                    boolean alreadyExists = false;
                    for (User existingUser : users) {
                        if (username.toLowerCase().equals(existingUser.getUsername().toLowerCase())) {
                            serverSocket.send(SocketService.quickConfirmationPacket("alreadyExists", receivedPacket.getAddress()
                                    , receivedPacket.getPort()));
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        User newUser = new User(username, receivedPacket.getAddress());
                        users.add(newUser);
                        System.out.println("New user registered as '" + newUser.getUsername() + "' (" + newUser.getDetails().getHostName() + ")");
                        serverSocket.send(SocketService.quickConfirmationPacket("userAdded", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                    }
                }
                //request to delete a chatroom
                else if (index == 3) {
                    System.out.println("User '" + currentUser.getUsername() + "' requested to delete a chatroom.");
                    Chatroom toBeDeletedChatroom = new Chatroom();
                    toBeDeletedChatroom.setName(messageReceivedString);
                    toBeDeletedChatroom.setOwner(currentUser);
                    boolean chatroomDeleted = false;
                    for (Chatroom room : chatrooms) {
                        if (room.equals(toBeDeletedChatroom)) {
                            chatrooms.remove(toBeDeletedChatroom);
                            serverSocket.send(SocketService.quickConfirmationPacket("chatroomDeleted", receivedPacket.getAddress()
                                    , receivedPacket.getPort()));
                            chatroomDeleted = true;
                            System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' has been deleted.");
                            break;
                        }
                    }
                    if (!chatroomDeleted) {
                        serverSocket.send(SocketService.quickConfirmationPacket("chatroomNotDeleted", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                        System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' does not exist or user is not the owner for it to be deleted.");
                    }
                }
                //server received an unknown message, so lets user know
                else {
                    System.out.println("Received an unknown message '" + messageReceivedString + "' from user '" + currentUser.getUsername() + "'");
                    serverSocket.send(SocketService.quickConfirmationPacket("unknownMessage", receivedPacket.getAddress()
                            , receivedPacket.getPort()));

                }
            } else if (messageReceived instanceof Chatroom) {
                Chatroom newChatroom = (Chatroom) messageReceived;
                //if the user requested to create a chatroom
                if (index == 0) {
                    System.out.println("User '" + currentUser.getUsername() + "' requested to create a chatroom.");
                    boolean alreadyExists = false;
                    for (Chatroom room : chatrooms) {
                        if (room.getName().toLowerCase().equals(newChatroom.getName().toLowerCase())) {
                            serverSocket.send(SocketService.quickConfirmationPacket("alreadyExists", receivedPacket.getAddress()
                                    , receivedPacket.getPort()));
                            alreadyExists = true;
                            System.out.println("Chatroom '" + newChatroom.getName() + "' already exists.");
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        newChatroom.setOwner(currentUser);
                        chatrooms.add(newChatroom);
                        System.out.println("Chatroom '" + newChatroom.getName() + "' has been created.");
                        serverSocket.send(SocketService.quickConfirmationPacket("chatroomAdded", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
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
                            for (User user : users) {
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
                        serverSocket.send(SocketService.quickConfirmationPacket("ownerChanged", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                        System.out.println("Ownership of '" + newChatroom.getName() + "' has been successfully delegated from '" + currentUser.getUsername() + "' to '" + newChatroom.getOwner().getUsername() + "'.");
                    }
                    if (!ownershipDelegated) {
                        serverSocket.send(SocketService.quickConfirmationPacket("clientNotOwner", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
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
