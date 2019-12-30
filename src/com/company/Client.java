package com.company;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.SocketService;

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
        String ipString = "192.168.1.15";
        int port = 1;
        int maxLength = 3500;
        String username;
        try {
            InetAddress serverAddr = InetAddress.getByName(ipString);
            InetAddress thisMachine = InetAddress.getLocalHost();

            String checkAvailability = "/availability";
            byte[] question;
            question = SocketService.convertDistinguishableObjectToByteArray(5, checkAvailability);
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
            question = SocketService.convertDistinguishableObjectToByteArray(0, isUserRegistered);
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

                    byte[] serializedUser = SocketService.convertDistinguishableObjectToByteArray(0, thisClient);
                    DatagramPacket userPacket = new DatagramPacket(serializedUser, serializedUser.length, serverAddr, port);
                    socket.send(userPacket);
                    socket.receive(answerFromServer);
                    //this checks if user with the same alias already exists
                    isUserRegistered = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (isUserRegistered.equals("alreadyExists")) {
                        System.out.println("Username you have chosen already exists, please choose another.");
                        continue;
                    } else if (isUserRegistered.equals("userAdded")) {
                        System.out.println("You have been successfully registered as '" + thisClient.getAlias() + "'.");
                        break;
                    }
                }
            } else {
                socket.receive(answerFromServer);

                ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(answerFromServer.getData()));
                thisClient = (User) iStream.readObject();
                iStream.close();
                System.out.println("You are already registered as '" + thisClient.getAlias() + "'");
            }

            String userInputSentToServer;
            while (true) {
                System.out.println("Type text to send to the server: ");
                userInputSentToServer = userInput.readLine();
                userInputSentToServer = userInputSentToServer.toLowerCase();
                if (userInputSentToServer.equals("/exit")) {
                    break;
                } else if (userInputSentToServer.equals("/showallchatrooms")) {
                    question = SocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
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
                } else if (userInputSentToServer.equals("/showallusers")) {
                    question = SocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
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
                                System.out.println(user.getAlias());
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else if (userInputSentToServer.equals("/createchatroom")) {
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
                    }
                    chatroom.setOwner(thisClient);
                    byte[] serializedChatroom = SocketService.convertDistinguishableObjectToByteArray(0, chatroom);
                    DatagramPacket chatroomPacket = new DatagramPacket(serializedChatroom, serializedChatroom.length, serverAddr, port);
                    socket.send(chatroomPacket);
                    socket.receive(answerFromServer);
                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (msgFromServer.equals("chatroomAdded")) {
                        System.out.println("Your chatroom has been added on the server.");
                    } else if (msgFromServer.equals("alreadyExists")) {
                        System.out.println("The chatroom name you chose already exists.");
                    }

                } else if (userInputSentToServer.equals("/deletechatroom")) {
                    Chatroom chatroom = new Chatroom();
                    System.out.println("What is the name of the chatroom you want to delete?");
                    userInputSentToServer = userInput.readLine();
                    chatroom.setName(userInputSentToServer);
                    //so we can distinguish between
                    chatroom.setOwner(thisClient);
                    byte[] serializedChatroom = SocketService.convertDistinguishableObjectToByteArray(1, chatroom);
                    DatagramPacket chatroomPacket = new DatagramPacket(serializedChatroom, serializedChatroom.length, serverAddr, port);
                    socket.send(chatroomPacket);
                    socket.receive(answerFromServer);
                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
                    if (msgFromServer.equals("chatroomDeleted")) {
                        System.out.println("Your chatroom has been deleted from the server.");
                    } else if (msgFromServer.equals("chatroomNotDeleted")) {
                        System.out.println("You are not the owner or the chatroom does not exist so you cannot delete it.");
                    }
                } else if (userInputSentToServer.equals("/help")) {
                    System.out.println(help);
                } else {
                    question = SocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Exiting...");

    }
}
