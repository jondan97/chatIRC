package com.company;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.User;
import com.company.service.TCPSocketService;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class TCPThread extends Thread {

    User currentUser;
    private SocketChannel socket;
    private ArrayList<User> users;
    private ArrayList<Chatroom> chatrooms;
    private Multimap<User, Message> pendingChats;
    private ServerSocketChannel chattingServer;


    public TCPThread(SocketChannel socket, ArrayList<User> users, ArrayList<Chatroom> chatrooms, Multimap<User, Message> pendingChats) {
        this.socket = socket;
        this.users = users;
        this.chatrooms = chatrooms;
        this.pendingChats = pendingChats;
        this.chattingServer = chattingServer;
    }

    public void run() {
        Socket client = socket.socket();
//        try {
//            TCPSocketService.sendObject(client.getPort(), test);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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

        while (true) {
//            if (currentUser != null && currentUser.getChattingPort() != 0){
//                try {
//                    System.out.println("testing");
//                    Socket test = new Socket(currentUser.getDetails(),currentUser.getChattingPort());
//                    TCPSocketService.sendObject("test", test);
//                    test.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            //System.out.println("TCP request accepted from '" + currentUser.getUsername() + "'.");
            //to do: move some UDP functionalities here, implement new requirements
            String receivedMessage = null;
            try {
                receivedMessage = (String) TCPSocketService.receiveObject(client);
                System.out.println("Message received: " + receivedMessage);
            } catch (IOException | ClassNotFoundException e) {
                break;
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
                                currentUser = registeredUser;
                                TCPSocketService.sendObject(usernameExists, client);
                                break;
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                    }
                }

            } else if (receivedMessage.equals("/whisper")) {
                System.out.println("Request for a user to whisper has been accepted.");
                User wantedUser = null;
                while (true) {
                    try {
                        String requestedUserName = (String) TCPSocketService.receiveObject(client);
                        boolean userExists = false;
                        for (User user : users) {
                            if (user.getUsername().toLowerCase().equals(requestedUserName.toLowerCase())) {
                                wantedUser = user;
                                TCPSocketService.sendObject(wantedUser, client);
                                //Socket toWantedUser = new Socket(wantedUser.getDetails(),wantedUser.getChattingPort());
                                //TCPSocketService.sendObject("hi", toWantedUser);
                                System.out.println("User '" + wantedUser.getUsername() + "' exists.");
                                break;
                            }
                            wantedUser = null;
                        }
                        if (wantedUser != null) {
                            while (true) {
                                String character = (String) TCPSocketService.receiveObject(client);
                                int virtualKeyCode = (int) TCPSocketService.receiveObject(client);
                                System.out.println(character);
                                System.out.println(virtualKeyCode);

                                Message message = new Message(character, currentUser, virtualKeyCode);
                                pendingChats.put(wantedUser, message);
                                if (virtualKeyCode == 13) {
                                    break;
                                }
//                            for(Map.Entry<User,User> e: pendingChats.entries()){
//                                System.out.println(e.getKey().getUsername() + "," + e.getValue().getUsername());
//                            }
                            }
                        }


                        if (wantedUser == null) {
                            TCPSocketService.sendObject(wantedUser, client);
                            System.out.println("User that has been requested to whisper does not exist.");
                            continue;
                        }
                        break;
                    } catch (IOException e) {
                        System.out.println("Connection with client closed.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
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
            } else if (receivedMessage.equals("/joinchatroom")) {
                System.out.println("User '" + currentUser.getUsername() + "' requested to join a chatroom.");
                while (true) {
                    try {
                        String policy = "0";
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        Chatroom requestedChatroom = null;
                        for (Chatroom chatroom : chatrooms) {
                            if (chatroom.getName().toLowerCase().equals(requestedChatroomName.toLowerCase())) {
                                policy = chatroom.getPolicy();
                                requestedChatroom = chatroom;
                                break;
                            }
                        }
                        if (policy.equals("0")) {
                            TCPSocketService.sendObject(policy, client);
                            continue;
                        } else if (policy.equals("1")) {
                            TCPSocketService.sendObject(policy, client);
                            requestedChatroom.getUsers().add(currentUser);
                        } else if (policy.equals("2")) {
                            TCPSocketService.sendObject(policy, client);
                            boolean joined = false;
                            while (true) {
                                String password = (String) TCPSocketService.receiveObject(client);

                                if (requestedChatroom.getPassword().equals(password)) {
                                    requestedChatroom.getUsers().add(currentUser);
                                    joined = true;
                                    TCPSocketService.sendObject(joined, client);
                                    break;
                                } else {
                                    TCPSocketService.sendObject(joined, client);
                                    continue;
                                }
                            }
                            break;
                        }


                    } catch (IOException e) {
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }


            } else if (receivedMessage.equals("/kickfromchatroom")) {
                System.out.println("User '" + currentUser.getUsername() + "' requested to kick a user from a chatroom.");
                while (true) {
                    try {
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        boolean chatroomExists = false;
                        Chatroom currentChatroom = null;
                        //if future owner exists in the current group
                        boolean userIsOwner = false;
                        for (Chatroom chatroom : chatrooms) {
                            if (chatroom.getName().toLowerCase().equals(requestedChatroomName.toLowerCase()) && currentUser.getUsername().equals(chatroom.getOwner().getUsername())) {
                                chatroomExists = true;
                                currentChatroom = chatroom;
                                TCPSocketService.sendObject(chatroomExists, client);
                                break;
                            }
                        }
                        if (!chatroomExists) {
                            System.out.println("Chatroom name does not exist.");
                            //chatroom does not exist or not current owner
                            TCPSocketService.sendObject(chatroomExists, client);
                            continue;
                        } else if (chatroomExists) {
                            while (true) {
                                String userToBeKickedName = (String) TCPSocketService.receiveObject(client);
                                boolean userKicked = false;
                                for (User user : currentChatroom.getUsers()) {
                                    if (user.getUsername().toLowerCase().equals(userToBeKickedName.toLowerCase()) && !user.equals(currentUser)) {
                                        currentChatroom.getUsers().remove(user);
                                        userKicked = true;
                                        TCPSocketService.sendObject(userKicked, client);
                                        break;
                                    }
                                }
                                if (!userKicked) {
                                    //user does not exist
                                    TCPSocketService.sendObject(userKicked, client);
                                    continue;
                                }
                                break;
                            }
                            System.out.println("User has been successfully kicked from the chatroom.");
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

            } else if (receivedMessage.equals(("/chattingPort"))) {
                currentUser.setChattingPort(client.getPort());
                System.out.println(currentUser.getChattingPort());
                while (true) {
//                    if(!pendingChats.isEmpty()){
//                               for(Map.Entry<User,User> e: pendingChats.entries()){
//                                  System.out.println(e.getKey().getUsername() + "," + e.getValue().getUsername());
//                             }
//                    }
                    if (pendingChats.containsKey(currentUser)) {
                        Message msg = null;
                        for (Message message : pendingChats.get(currentUser)) {
                            try {
                                TCPSocketService.sendObject(message, client);
                                //int port = 5555;
                                //MessageService ms = new MessageService(client, chattingServer.socket().accept());
                                //ms.start();
                                msg = message;
                                System.out.println("Permission sent?");
                                break;
                            } catch (IOException e) {
                                System.out.println("Connection closed.");
                            }
                        }
                        pendingChats.remove(currentUser, msg);
//                        for()
//
//                        assertThat((Collection<String>) map.get("key1"))
//                                .containsExactly("value2", "value1");
                    }
                }
//                try {
//                    while(true){
//                        this.sleep(100000);
//                        TCPSocketService.sendObject("POU SE RE", client);
//                    }
//                    //break;
//                } catch (IOException | InterruptedException e) {
//                    e.printStackTrace();
//                }
            } else if (receivedMessage.equals("/exit")) {
                try {
                    socket.close();
                    System.out.println("Connection has been closed!!!.");
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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


