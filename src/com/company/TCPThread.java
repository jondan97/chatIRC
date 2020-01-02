package com.company;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.TCPSocketService;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class TCPThread extends Thread {

    User currentUser;
    private SocketChannel socket;
    private ArrayList<User> users;
    private ArrayList<Chatroom> chatrooms;


    public TCPThread(SocketChannel socket, ArrayList<User> users, ArrayList<Chatroom> chatrooms) {
        this.socket = socket;
        this.users = users;
        this.chatrooms = chatrooms;
    }

    public void run() {
        Socket client = socket.socket();
        for (User user : users) {
            //this detects if the user already exists, if he exists then set him as current user
            //ska is a test user
            //this would be safer if I could know the mac-address but going into too much security
            //also for some reason some host names are saved in lowercase (no idea why) while they are
            //capitalized when first received
            if (!user.getUsername().equals("ska") && client.getInetAddress().getHostName().toUpperCase().equals(user.getDetails().getHostName().toUpperCase())) {
                currentUser = user;
            }
        }
        //System.out.println("TCP request accepted from '" + currentUser.getUsername() + "'.");
        //to do: move some UDP functionalities here, implement new requirements
        String receivedMessage = null;
        try {
            receivedMessage = (String) TCPSocketService.receiveObject(client);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (receivedMessage.equals("/isUserRegistered")) {
            System.out.println("New user requested to register.");
            boolean userIsRegistered;
            if (currentUser != null) {
                userIsRegistered = true;
                try {
                    TCPSocketService.sendObject(userIsRegistered, client);
                    System.out.println("User is already registered as '" + currentUser.getUsername() + "'");
                    TCPSocketService.sendObject(currentUser.getUsername(), client);
                } catch (IOException e) {
                }
            } else {
                try {
                    userIsRegistered = false;
                    TCPSocketService.sendObject(userIsRegistered, client);
                    while (true) {
                        boolean usernameExists = false;
                        String usernameRequested = (String) TCPSocketService.receiveObject(client);
                        for (User user : users) {
                            if (user.getUsername().toLowerCase().equals(usernameRequested.toLowerCase())) {
                                usernameExists = true;
                                TCPSocketService.sendObject(usernameExists, client);
                                continue;
                            }
                        }
                        if (!usernameExists) {
                            User registeredUser = new User(usernameRequested, client.getInetAddress());
                            users.add(registeredUser);
                            TCPSocketService.sendObject(usernameExists, client);
                            break;
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                }
            }

        } else if (receivedMessage.equals("/showallchatrooms")) {
            try {
                TCPSocketService.sendObject(chatrooms, client);
            } catch (IOException e) {
            }
        } else if (receivedMessage.equals("/showallusers")) {
            try {
                TCPSocketService.sendObject(users, client);
            } catch (IOException e) {
            }
        } else if (receivedMessage.equals("/showchatroomusers")) {
            try {
                while (true) {
                    //receive the chatroom name
                    String chatroomName = (String) TCPSocketService.receiveObject(client);
                    Chatroom requestedChatroom = null;
                    boolean chatroomExists = false;
                    for (Chatroom chatroom : chatrooms) {
                        if (chatroom.getName().equals(chatroomName)) {
                            requestedChatroom = chatroom;
                            chatroomExists = true;
                        }
                    }
                    TCPSocketService.sendObject(chatroomExists, client);
                    if (!chatroomExists) {
                        continue;
                    } else {
                        TCPSocketService.sendObject(requestedChatroom.getUsers(), client);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else if (receivedMessage.equals("/deletechatroom")) {
            while (true) {
                try {
                    String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                    Chatroom toBeDeletedChatroom = new Chatroom();
                    toBeDeletedChatroom.setName(requestedChatroomName);
                    toBeDeletedChatroom.setOwner(currentUser);
                    boolean chatroomDeleted = false;
                    for (Chatroom room : chatrooms) {
                        if (room.equals(toBeDeletedChatroom)) {
                            chatrooms.remove(toBeDeletedChatroom);
                            chatroomDeleted = true;
                            TCPSocketService.sendObject(chatroomDeleted, client);
                            System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' has been deleted.");
                            break;
                        }
                    }
                    if (!chatroomDeleted) {
                        TCPSocketService.sendObject(chatroomDeleted, client);
                        System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' does not exist or user is not the owner for it to be deleted.");
                        continue;
                    } else if (chatroomDeleted) {
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Connection with client closed.");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } else if (receivedMessage.equals("/createchatroom")) {
            System.out.println("User '" + currentUser.getUsername() + "' requested to create a chatroom.");
            while (true) {
                try {
                    String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                    boolean alreadyExists = false;
                    for (Chatroom chatroom : chatrooms) {
                        if (chatroom.getName().toLowerCase().equals(requestedChatroomName.toLowerCase())) {
                            alreadyExists = true;
                            System.out.println("Chatroom '" + requestedChatroomName + "' already exists.");
                            TCPSocketService.sendObject(alreadyExists, client);
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        TCPSocketService.sendObject(alreadyExists, client);
                        Chatroom requestedChatroom = new Chatroom();
                        requestedChatroom.setOwner(currentUser);
                        requestedChatroom.setName(requestedChatroomName);
                        String policy = (String) TCPSocketService.receiveObject(client);
                        requestedChatroom.setPolicy(policy);
                        if (policy.equals("2")) {
                            //2=password required, which means that we need to get the password as well
                            String password = (String) TCPSocketService.receiveObject(client);
                            requestedChatroom.setPassword(password);
                        }
                        requestedChatroom.getUsers().add(currentUser);
                        requestedChatroom.getUsers().add(new User("ska"));
                        chatrooms.add(requestedChatroom);
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else if (receivedMessage.equals("/delegatechatroomownership")) {
            System.out.println("User '" + currentUser.getUsername() + "' requested to delegate ownership of a chatroom.");
            while (true) {
                try {
                    String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                    boolean chatroomExists = false;
                    Chatroom currentChatroom = null;
                    //if future owner exists in the current group
                    boolean userExists = false;
                    User newOwner = null;
                    for (Chatroom chatroom : chatrooms) {
                        if (chatroom.getName().toLowerCase().equals(requestedChatroomName.toLowerCase()) && currentUser.getUsername().equals(chatroom.getOwner().getUsername())) {
                            chatroomExists = true;
                            currentChatroom = chatroom;
                            TCPSocketService.sendObject(chatroomExists, client);
                            break;
                        }
                    }
                    if (!chatroomExists) {
                        //chatroom does not exist or not current owner
                        TCPSocketService.sendObject(chatroomExists, client);
                        continue;
                    } else if (chatroomExists) {
                        while (true) {
                            String newOwnerName = (String) TCPSocketService.receiveObject(client);
                            for (User user : currentChatroom.getUsers()) {
                                if (user.getUsername().toLowerCase().equals(newOwnerName.toLowerCase())) {
                                    userExists = true;
                                    newOwner = user;
                                    TCPSocketService.sendObject(userExists, client);
                                }
                            }
                            if (!userExists) {
                                //user does not exist
                                TCPSocketService.sendObject(userExists, client);
                                continue;
                            }
                            //we do this break out here, because if we do it inside the for loop, we will be stuck in the while loop for ever
                            else if (userExists) {
                                currentChatroom.setOwner(newOwner);
                                System.out.println("Owner has changed.");
                                break;
                            }
                        }

                    }
                    //same for this while loop, so we somehow have to break
                    if (chatroomExists && userExists) {
                        break;
                    }

                } catch (IOException e) {
                    System.out.println("Connection has been closed.");
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
            }
//                // Strings are immutable so a normal for loop needs to be used here in order to
//                // get the real Chatroom instance (and not a copy of it) so that we can
//                // modify its string-properties
//                for (int i = 0; i < chatrooms.size(); i++) {
//                    if (chatrooms.get(i).getName().equals(newChatroom.getName()) && currentUser.getUsername().equals(chatrooms.get(i).getOwner().getUsername())) {
//                        newChatroom.setPassword(chatrooms.get(i).getPassword());
//                        newChatroom.setPolicy(chatrooms.get(i).getPolicy());
//                        newChatroom.setDeletionTime(chatrooms.get(i).getDeletionTime());
//                        newChatroom.setKickTime(chatrooms.get(i).getKickTime());
//                        newChatroom.setUsers(chatrooms.get(i).getUsers());
//                        //basically we try to create a new Chatroom instance and not just
//                        //copy the reference
//                        //after thought: a constructor could be used that takes as a parameter
//                        //the old chatroom #toolazy
//
//                        //for the users that belong to the chatroom:
//                        for (User user : newChatroom.getUsers()) {
//                            //if the new owner is included in them, then replace him with the original user
//                            //that as an extra contains the network details
//                            if (user.getUsername().equals(newChatroom.getOwner().getUsername())) {
//                                newChatroom.setOwner(user);
//                                chatrooms.set(i, newChatroom);
//                                ownershipDelegated = true;
//                                break;
//                            }
//                        }
//
//                    }
//                }
//                if (ownershipDelegated) {
//                    udpserver.send(UDPSocketService.newQuickConfirmationPacket("ownerChanged"), clientNetworkDetails);
//                    System.out.println("Ownership of '" + newChatroom.getName() + "' has been successfully delegated from '" + currentUser.getUsername() + "' to '" + newChatroom.getOwner().getUsername() + "'.");
//                }
//                //this could be turned into a more user-friendly error message
//                //and not just contain 3 error messages at the same time (client gets more detailed error message
//                //but still complicated enough)
//                if (!ownershipDelegated) {
//                    udpserver.send(UDPSocketService.newQuickConfirmationPacket("clientNotOwner"), clientNetworkDetails);
//                    System.out.println("Ownership of chatroom '" + newChatroom.getName() + "' failed to be delegated.");
//                }
        }
//            try {
//                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//                while (!(data = in.readLine()).equals("exit")) {
//                    System.out.println("\r\nMessage from " + client.getInetAddress().getHostAddress() + ": " + data);
//                }
//                System.out.println("Connection closed.");
//                break;
//            } catch (IOException e) {
//                System.out.println("Connection closed.");
//                break;
//            }

    }
    }


