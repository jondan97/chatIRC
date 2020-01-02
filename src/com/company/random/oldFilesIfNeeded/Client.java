package com.company.random.oldFilesIfNeeded;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.UDPSocketService;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Client {
    public static void main(String[] args) {
        String help = "" +
                "/showAllUsers - Shows all users currently registered on the server. \n" +
                "/showAllChatrooms - Shows all chatrooms that currently exist on  the server \n" +
                "/createChatroom - Creates a new chatroom \n" +
                "/deleteChatroom - Deletes an existing chatroom (provided you are the owner) \n" +
                "/help - Shows all available commands again. \n" +
                "/exit - Exits the application. \n";
        System.out.println(help);
        String ipString = "192.168.1.13";
        int port = 1;
        int maxLength = 3500;
        String username;
        try {
            InetAddress serverAddr = InetAddress.getByName(ipString);
            InetAddress thisMachine = InetAddress.getLocalHost();

            String checkAvailability = "/availability";
            byte[] question;
            question = UDPSocketService.convertDistinguishableObjectToByteArray(5, checkAvailability);
            DatagramPacket testPacketToServer = new DatagramPacket(question, question.length, serverAddr, port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(testPacketToServer);

            byte[] answer = new byte[maxLength];
            DatagramPacket answerFromServer = new DatagramPacket(answer, maxLength);
            socket.setSoTimeout(1000);
            socket.receive(answerFromServer);
            socket.setSoTimeout(0);
            String isServerOnline = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
            if (isServerOnline.equals("true")) {
                System.out.println("Succesfully pinged: " + ipString + " (" + serverAddr.getHostName() + ")");
            } else {
                System.out.println("Connected to server but connection may be weak.");
            }

            String isUserRegistered = "/isUserRegistered";
            question = UDPSocketService.convertDistinguishableObjectToByteArray(0, isUserRegistered);
            DatagramPacket isUserRegisteredPacketToServer = new DatagramPacket(question, question.length, serverAddr, port);
            socket.send(isUserRegisteredPacketToServer);
            socket.receive(answerFromServer);

            isUserRegistered = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
            User thisClient;
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            if (isUserRegistered.equals("false")) {
                while (true) {
                    System.out.println("What is your username going to be?");
                    username = userInput.readLine();
                    thisClient = new User(username, thisMachine);

                    byte[] serializedUser = UDPSocketService.convertDistinguishableObjectToByteArray(2, username);
                    DatagramPacket userPacket = new DatagramPacket(serializedUser, serializedUser.length, serverAddr, port);
                    socket.send(userPacket);
                    socket.receive(answerFromServer);
                    //this checks if user with the same alias already exists
                    isUserRegistered = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (isUserRegistered.equals("alreadyExists")) {
                        System.out.println("Username you have chosen already exists, please choose another.");
                        continue;
                    } else if (isUserRegistered.equals("userAdded")) {
                        System.out.println("You have been successfully registered as '" + thisClient.getUsername() + "'.");
                        break;
                    }
                }
            } else {
                socket.receive(answerFromServer);
                username = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                thisClient = new User(username, thisMachine);
                System.out.println("You are already registered as '" + thisClient.getUsername() + "'");
            }

            String userInputSentToServer;
            while (true) {
                System.out.println("Type text to send to the server: ");
                userInputSentToServer = userInput.readLine();
                userInputSentToServer = userInputSentToServer.toLowerCase();
                //request to exit the application
                if (userInputSentToServer.equals("/exit")) {
                    break;
                    //request to see all chatrooms
                } else if (userInputSentToServer.equals("/showallchatrooms")) {
                    question = UDPSocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
                    DatagramPacket packetToServer = new DatagramPacket(question, question.length, serverAddr, port);
                    socket.send(packetToServer);
                    socket.receive(answerFromServer);
                    ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(answerFromServer.getData()));
                    try {
                        ArrayList<Chatroom> chatrooms = (ArrayList) iStream.readObject();
                        iStream.close();
                        if (chatrooms.isEmpty()) {
                            System.out.println("No chatrooms exist yet.");
                        } else {
                            for (Chatroom chatroom : chatrooms) {
                                System.out.println(chatroom);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    //request to see all users
                } else if (userInputSentToServer.equals("/showallusers")) {
                    question = UDPSocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
                    DatagramPacket packetToServer = new DatagramPacket(question, question.length, serverAddr, port);
                    socket.send(packetToServer);
                    socket.receive(answerFromServer);
                    ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(answerFromServer.getData()));
                    try {
                        ArrayList<User> users = (ArrayList) iStream.readObject();
                        iStream.close();
                        if (users.isEmpty()) {
                            System.out.println("No users exist yet.");
                        } else {
                            for (User user : users) {
                                System.out.println(user.getUsername());
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //request to see all users of a particular chatroom
                else if (userInputSentToServer.equals("/showchatroomusers")) {
                    System.out.println("What is the name of the chatroom you want to see the users of?");
                    userInputSentToServer = userInput.readLine();
                    //based on the architecture implemented on the server, we need to have a second
                    //part in our packet (an object), so we send the String object along even though we
                    //don't have to
                    question = UDPSocketService.convertDistinguishableObjectToByteArray(1, userInputSentToServer);
                    DatagramPacket packetToServer = new DatagramPacket(question, question.length, serverAddr, port);
                    socket.send(packetToServer);
                    socket.receive(answerFromServer);
                    ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(answerFromServer.getData()));
                    try {
                        ArrayList<User> chatroomUsers = (ArrayList) iStream.readObject();
                        iStream.close();
                        if (chatroomUsers.isEmpty()) {
                            System.out.println("No users exist in this chatroom.");
                        } else {
                            for (User user : chatroomUsers) {
                                System.out.println(user.getUsername());
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    //here, we need to create the new chatroom because we can't send to the server
                    //the name of the chatroom and the new owner at the same time, so we need to
                    //do something "smart", by sending a new chatroom to the server and then
                    //just swapping values
                }
                //this should be changed to TCP or new algorithm needs to be designed
                //request to delegate chatroom ownership
                else if (userInputSentToServer.equals("/delegatechatroomownership")) {
                    Chatroom chatroom = new Chatroom();
                    System.out.println("What is the name of the chatroom?");
                    userInputSentToServer = userInput.readLine();
                    chatroom.setName(userInputSentToServer);
                    System.out.println("What is the name of the new owner?");
                    userInputSentToServer = userInput.readLine();
                    User newOwner = new User(userInputSentToServer);
                    chatroom.setOwner(newOwner);
                    byte[] serializedChatroom = UDPSocketService.convertDistinguishableObjectToByteArray(2, chatroom);
                    DatagramPacket chatroomPacket = new DatagramPacket(serializedChatroom, serializedChatroom.length, serverAddr, port);
                    socket.send(chatroomPacket);
                    socket.receive(answerFromServer);
                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (msgFromServer.equals("ownerChanged")) {
                        System.out.println("The owner successfully changed.");
                    } else if (msgFromServer.equals("clientNotOwner")) {
                        System.out.println("You are not the current owner or either chatroom or user does not exist.");
                    }
                }
                //request to create a new chatroom
                else if (userInputSentToServer.equals("/createchatroom")) {
                    Chatroom chatroom = new Chatroom();
                    System.out.println("What is going to be the name of the chatroom?");
                    userInputSentToServer = userInput.readLine();
                    chatroom.setName(userInputSentToServer);
                    System.out.println("What is going to be the policy of the chatroom? (1=Free, 2=Password-Protected, 3=Permission-Required)");
                    userInputSentToServer = userInput.readLine();
                    chatroom.setPolicy(userInputSentToServer);
                    if (userInputSentToServer.equals("2")) {
                        System.out.println("What will the chatroom's password be?");
                        userInputSentToServer = userInput.readLine();
                        chatroom.setPassword(userInputSentToServer);
                    }
                    chatroom.setOwner(thisClient);
                    chatroom.getUsers().add(thisClient);
                    chatroom.getUsers().add(new User("ska"));
                    byte[] serializedChatroom = UDPSocketService.convertDistinguishableObjectToByteArray(0, chatroom);
                    DatagramPacket chatroomPacket = new DatagramPacket(serializedChatroom, serializedChatroom.length, serverAddr, port);
                    socket.send(chatroomPacket);
                    socket.receive(answerFromServer);
                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (msgFromServer.equals("chatroomAdded")) {
                        System.out.println("Your chatroom has been added on the server.");
                    } else if (msgFromServer.equals("alreadyExists")) {
                        System.out.println("The chatroom name you chose already exists.");
                    }
                    //request to delete a chatroom
                } else if (userInputSentToServer.equals("/deletechatroom")) {
                    System.out.println("What is the name of the chatroom you want to delete?");
                    userInputSentToServer = userInput.readLine();
                    byte[] serializedChatroom = UDPSocketService.convertDistinguishableObjectToByteArray(3, userInputSentToServer);
                    DatagramPacket packetPacket = new DatagramPacket(serializedChatroom, serializedChatroom.length, serverAddr, port);
                    socket.send(packetPacket);
                    socket.receive(answerFromServer);
                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (msgFromServer.equals("chatroomDeleted")) {
                        System.out.println("Your chatroom has been deleted from the server.");
                    } else if (msgFromServer.equals("chatroomNotDeleted")) {
                        System.out.println("You are not the owner or the chatroom does not exist so you cannot delete it.");
                    }
                    //request to see all the commands available in the application
                } else if (userInputSentToServer.equals("/help")) {
                    System.out.println(help);
                    //when the user has entered something the client does not understand and the
                    //neither will the server
                } else {
                    question = UDPSocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
                    DatagramPacket packetToServer = new DatagramPacket(question, question.length, serverAddr, port);
                    socket.send(packetToServer);
                    socket.receive(answerFromServer);
                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (msgFromServer.equals("unknownMessage")) {
                        System.out.println("The server did not understand your question.");
                    }
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Unable to connect to IP/Host-Name given. ");
        } catch (SocketTimeoutException e) {
            System.out.println("Looks like the server is not up. Try again later.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Exiting...");

    }
}
