package com.company;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.TCPSocketService;
import com.company.service.UDPSocketService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;

public class Client {
    public static void main(String[] args) throws IOException {
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
        //in case the user wants to change IP/Port without editing the Java files
        if (args.length > 0) {
            ipString = args[1];
            port = Integer.parseInt(args[0]);
        }
        InetAddress serverAddr = InetAddress.getByName(ipString);

        //maximum length of received/sent packets
        int datagramPacketMaxLength = 3500;
        //username of current client, can be received from server (if already registered)
        // or user inputs a new one
        String username;
        try {
            //so we know the client's address
            InetAddress thisMachine = InetAddress.getLocalHost();

            //first we check if the server is up through UDP
            String checkAvailability = "/availability";
            //Question asked in form of a String (usually)
            byte[] question;
            question = UDPSocketService.convertDistinguishableObjectToByteArray(5, checkAvailability);
            DatagramPacket testPacketToServer = new DatagramPacket(question, question.length, serverAddr, port);
            DatagramSocket UDPsocket = new DatagramSocket();
            UDPsocket.send(testPacketToServer);
            //then we get the answer, if we receive a message that says "true", then ping success
            //if we receive anything else ("false" in that case) warn that the connection might be weak
            //this happens because we might have received a corrupted packet
            byte[] answer = new byte[datagramPacketMaxLength];
            DatagramPacket answerFromServer = new DatagramPacket(answer, datagramPacketMaxLength);
            //waiting time for a packet to be received is set here and then reset back to 0
            UDPsocket.setSoTimeout(1000);
            UDPsocket.receive(answerFromServer);
            UDPsocket.setSoTimeout(0);
            String isServerOnline = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
            if (isServerOnline.equals("true")) {
                System.out.println("Succesfully pinged: " + ipString + " (" + serverAddr.getHostName() + ")");
            } else {
                System.out.println("Pinged server but connection may be weak.");
            }

            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            String isUserRegistered = "/isUserRegistered";
            User thisClient = null;
            Socket tcpSocket = new Socket("192.168.1.13", 1);
            TCPSocketService.sendObject(isUserRegistered, tcpSocket);
            boolean userIsRegistered = (boolean) TCPSocketService.receiveObject(tcpSocket);
            if (userIsRegistered) {
                username = (String) TCPSocketService.receiveObject(tcpSocket);
                thisClient = new User(username, thisMachine);
                System.out.println("You are already registered as '" + thisClient.getUsername() + "'");
            } else if (!userIsRegistered) {
                boolean usernameExists = false;
                while (true) {
                    System.out.println("What is your username going to be?");
                    username = userInput.readLine();
                    TCPSocketService.sendObject(username, tcpSocket);
                    usernameExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                    if (usernameExists) {
                        System.out.println("Username you have chosen already exists, please choose another.");
                        continue;
                    } else if (!usernameExists) {
                        thisClient = new User(username, thisMachine);
                        System.out.println("You have been successfully registered as '" + thisClient.getUsername() + "'.");
                        break;
                    }
                }
            }
            tcpSocket.close();

            //the user can now interact with the server in many ways, mainly with the help of the
            //existing commands
            String userInputSentToServer;
            while (true) {
                System.out.println("Type text to send to the server: ");
                userInputSentToServer = userInput.readLine();
                //user input is always lowercase, commands could be written any way
                //for example: '/showAllChatrooms' or '/showallchatrooms' or '/SHOWALLCHATROOMS'
                //all are accepted this way
                userInputSentToServer = userInputSentToServer.toLowerCase();
                //request to exit the application
                if (userInputSentToServer.equals("/exit")) {
                    break;
                    //request to see all chatrooms
                } else if (userInputSentToServer.equals("/showallchatrooms")) {
                    tcpSocket = new Socket("192.168.1.13", 1);
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    try {
                        ArrayList<Chatroom> chatrooms = (ArrayList<Chatroom>) TCPSocketService.receiveObject(tcpSocket);
                        tcpSocket.close();
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
                    tcpSocket = new Socket("192.168.1.13", 1);
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    try {
                        ArrayList<User> users = (ArrayList) TCPSocketService.receiveObject(tcpSocket);
                        tcpSocket.close();
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
                    tcpSocket = new Socket("192.168.1.13", 1);
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);

                    while (true) {
                        System.out.println("What is the name of the chatroom you want to see the users of?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        boolean chatroomExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        if (chatroomExists) {
                            break;
                        } else if (!chatroomExists) {
                            System.out.println("Chatroom does not exist.");
                            continue;
                        }
                    }
                    try {
                        ArrayList<User> chatroomUsers = (ArrayList) TCPSocketService.receiveObject(tcpSocket);
                        tcpSocket.close();
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
                }
                //this should be changed to TCP or more efficient algorithm needs to be designed
                //request to delegate chatroom ownership
                else if (userInputSentToServer.equals("/delegatechatroomownership")) {
                    tcpSocket = new Socket("192.168.1.13", 1);
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the chatroom?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        boolean chatroomExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        boolean userExists = false;
                        if (!chatroomExists) {
                            System.out.println("Chatroom name does not exist or you are not the owner.");
                            continue;
                        } else if (chatroomExists) {
                            while (true) {
                                System.out.println("What is the name of the new owner?");
                                userInputSentToServer = userInput.readLine();
                                TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                                userExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                                if (userExists) {
                                    System.out.println("Owner has changed.");
                                    break;
                                } else if (!userExists) {
                                    System.out.println("User does not exist in this group.");
                                    continue;
                                }

                            }
                        }
                        if (chatroomExists && userExists) {
                            break;
                        }
                    }
                }
//                    Chatroom chatroom = new Chatroom();
//                    System.out.println("What is the name of the chatroom?");
//                    userInputSentToServer = userInput.readLine();
//                    chatroom.setName(userInputSentToServer);
//                    System.out.println("What is the name of the new owner?");
//                    userInputSentToServer = userInput.readLine();
//                    User newOwner = new User(userInputSentToServer);
//                    chatroom.setOwner(newOwner);
//                    byte[] serializedChatroom = UDPSocketService.convertDistinguishableObjectToByteArray(2, chatroom);
//                    DatagramPacket chatroomPacket = new DatagramPacket(serializedChatroom, serializedChatroom.length, serverAddr, port);
//                    UDPsocket.send(chatroomPacket);
//                    UDPsocket.receive(answerFromServer);
//                    String msgFromServer = new String(answerFromServer.getData(), 0, answerFromServer.getLength());
//                    if (msgFromServer.equals("ownerChanged")) {
//                        System.out.println("The owner successfully changed.");
//                    } else if (msgFromServer.equals("clientNotOwner")) {
//                        System.out.println("You are not the current owner or either chatroom or user do not exist.");
//                    }
//                }
                //this should be changed to TCP or more efficient algorithm needs to be designed
                //request to create a new chatroom
                else if (userInputSentToServer.equals("/createchatroom")) {
                    tcpSocket = new Socket("192.168.1.13", 1);
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is going to be the name of the chatroom?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        boolean alreadyExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        if (alreadyExists) {
                            System.out.println("Chatroom name already exists, please choose another.");
                            continue;
                        } else if (!alreadyExists) {
                            System.out.println("What is going to be the policy of the chatroom? (1=Free, 2=Password-Protected, 3=Permission-Required)");
                            userInputSentToServer = userInput.readLine();
                            TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                            if (userInputSentToServer.equals("2")) {
                                System.out.println("What will the chatroom's password be?");
                                userInputSentToServer = userInput.readLine();
                                //this should be encrypted when sent
                                TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                            }
                            System.out.println("Your chatroom has been succesfully added on the server.");
                            break;
                        }
                    }
                    //request to delete a chatroom
                } else if (userInputSentToServer.equals("/deletechatroom")) {
                    tcpSocket = new Socket("192.168.1.13", 1);
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the chatroom you want to delete?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        boolean chatroomDeleted = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        if (chatroomDeleted) {
                            System.out.println("Chatroom has been successfully deleted.");
                            break;
                        } else if (!chatroomDeleted) {
                            System.out.println("Chatroom name you entered does not exist or you are not the owner.");
                            continue;
                        }
                    }
                    //request to see all the commands available in the application
                } else if (userInputSentToServer.equals("/help")) {
                    System.out.println(help);
                    //when the user has entered something the client does not understand
                    //and neither will the server (hopefully)
                } else {
                    question = UDPSocketService.convertDistinguishableObjectToByteArray(0, userInputSentToServer);
                    DatagramPacket packetToServer = new DatagramPacket(question, question.length, serverAddr, port);
                    UDPsocket.send(packetToServer);
                    UDPsocket.receive(answerFromServer);
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
