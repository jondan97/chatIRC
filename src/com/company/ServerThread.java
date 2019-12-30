package com.company;

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
                if (receivedPacket.getAddress().getHostName().toUpperCase().equals(user.getDetails().getHostName().toUpperCase())) {
                    currentUser = user;
                }
            }
            int index = 0;
            Serializable messageReceived = null;
            try {
                //we convert the array back to the object, so we know what to do with it
                ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(receivedPacket.getData()));
                index = iStream.readInt();
                messageReceived = (Serializable) iStream.readObject();
                iStream.close();
            } catch (EOFException e) {
                if (currentUser != null) {
                    System.out.println("New message from a user '" + currentUser.getAlias() + "' received.");
                } else {
                    System.out.println("New message from a user received.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (messageReceived instanceof String) {
                String messageReceivedString = (String) messageReceived;
                if (messageReceivedString.equals("/availability")) {
                    serverSocket.send(SocketService.quickConfirmationPacket("true", receivedPacket.getAddress()
                            , receivedPacket.getPort()));
                } else if (messageReceivedString.equals("/isUserRegistered")) {
                    System.out.println("New user requested to register.");
                    if (currentUser != null) {
                        serverSocket.send(SocketService.quickConfirmationPacket("true", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                        System.out.println("User is already registered as '" + currentUser.getAlias() + "'");
                        byte[] serializedUser = SocketService.convertObjectToByteArray(currentUser);

                        DatagramPacket userPacket = new DatagramPacket(serializedUser, serializedUser.length,
                                receivedPacket.getAddress(), receivedPacket.getPort());
                        serverSocket.send(userPacket);
                    } else {
                        serverSocket.send(SocketService.quickConfirmationPacket("false", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                    }

                } else if (messageReceivedString.equals("/showallchatrooms")) {
                    byte[] serializedAllChatrooms = SocketService.convertObjectToByteArray(chatrooms);
                    DatagramPacket chatroomsPacket = new DatagramPacket(serializedAllChatrooms, serializedAllChatrooms.length,
                            receivedPacket.getAddress(), receivedPacket.getPort());
                    serverSocket.send(chatroomsPacket);
                    System.out.println("'" + currentUser.getAlias() + "' requested to see all chatrooms.");
                } else if (messageReceivedString.equals("/showallusers")) {
                    byte[] serializedAllUsers = SocketService.convertObjectToByteArray(users);
                    DatagramPacket usersPacket = new DatagramPacket(serializedAllUsers, serializedAllUsers.length,
                            receivedPacket.getAddress(), receivedPacket.getPort());
                    serverSocket.send(usersPacket);
                    System.out.println("'" + currentUser.getAlias() + "' requested to see all users.");
                } else {
                    System.out.println("Received an unknown message '" + messageReceivedString + "' from user '" + currentUser.getAlias() + "'");
                    serverSocket.send(SocketService.quickConfirmationPacket("unknownMessage", receivedPacket.getAddress()
                            , receivedPacket.getPort()));

                }
            } else if (messageReceived instanceof User) {
                try {
                    User user = (User) messageReceived;
                    boolean alreadyExists = false;
                    for (User existingUser : users) {
                        if (user.getAlias().toLowerCase().equals(existingUser.getAlias().toLowerCase())) {
                            serverSocket.send(SocketService.quickConfirmationPacket("alreadyExists", receivedPacket.getAddress()
                                    , receivedPacket.getPort()));
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        users.add(user);
                        System.out.println("New user registered as '" + user.getAlias() + "' (" + user.getDetails().getHostName() + ")");
                        serverSocket.send(SocketService.quickConfirmationPacket("userAdded", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (messageReceived instanceof Chatroom) {
                Chatroom chatroom = (Chatroom) messageReceived;
                //if the user requested to create a chatroom
                if (index == 0) {
                    System.out.println("User '" + currentUser.getAlias() + "' requested to create a chatroom.");
                    boolean alreadyExists = false;
                    for (Chatroom room : chatrooms) {
                        if (room.getName().toLowerCase().equals(chatroom.getName().toLowerCase())) {
                            serverSocket.send(SocketService.quickConfirmationPacket("alreadyExists", receivedPacket.getAddress()
                                    , receivedPacket.getPort()));
                            alreadyExists = true;
                            System.out.println("Chatroom '" + chatroom.getName() + "' already exists.");
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        chatrooms.add(chatroom);
                        System.out.println("Chatroom '" + chatroom.getName() + "' has been created.");
                        serverSocket.send(SocketService.quickConfirmationPacket("chatroomAdded", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                    }
                }
                //if the user requested to delete a chatroom
                else if (index == 1) {
                    System.out.println("User '" + currentUser.getAlias() + "' requested to delete a chatroom.");
                    boolean chatroomDeleted = false;
                    for (Chatroom room : chatrooms) {
                        if (room.equals(chatroom)) {
                            chatrooms.remove(chatroom);
                            serverSocket.send(SocketService.quickConfirmationPacket("chatroomDeleted", receivedPacket.getAddress()
                                    , receivedPacket.getPort()));
                            chatroomDeleted = true;
                            System.out.println("Chatroom '" + chatroom.getName() + "' has been deleted.");
                            break;
                        }
                    }
                    if (!chatroomDeleted) {
                        serverSocket.send(SocketService.quickConfirmationPacket("chatroomNotDeleted", receivedPacket.getAddress()
                                , receivedPacket.getPort()));
                        System.out.println("Chatroom '" + chatroom.getName() + "' does not exist or user is not the owner for it to be deleted.");
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
