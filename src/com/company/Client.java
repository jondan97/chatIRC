package com.company;

import com.company.entity.Chatroom;
import com.company.entity.PrivateChatListener;
import com.company.entity.User;
import com.company.service.TCPSocketService;
import com.company.service.UDPSocketService;
import lc.kra.system.keyboard.GlobalKeyboardHook;
import lc.kra.system.keyboard.event.GlobalKeyAdapter;
import lc.kra.system.keyboard.event.GlobalKeyEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;

public class Client {
    //used for keystrokes
    private static boolean run = false;

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
            Socket tcpSocket = new Socket(ipString, port);
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

            //NOTE TO LECTURERS: TURN TO FALSE IF THIS DOESN'T WORK
            // Use false here to switch to hook instead of raw input
            GlobalKeyboardHook keyboardHook = new GlobalKeyboardHook(true);
            keyboardHook.addKeyListener(new GlobalKeyAdapter() {
                @Override
                public void keyPressed(GlobalKeyEvent event) {
                    try {
                        if (run == true) {
                            String keyChar = String.valueOf(event.getKeyChar());
                            TCPSocketService.sendObject(keyChar, tcpSocket);
                            TCPSocketService.sendObject(event.getVirtualKeyCode(), tcpSocket);
                            if (event.getVirtualKeyCode() == 13) {
                                run = false;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            run = false;

            //the user can now interact with the server in many ways, mainly with the help of the
            //existing commands
            String userInputSentToServer;
            PrivateChatListener chat = new PrivateChatListener(ipString, port, userInput);
            chat.start();
            while (true) {
                System.out.println("Type text to send to the server: ");
                userInputSentToServer = userInput.readLine();
                //user input is always lowercase, commands could be written any way
                //for example: '/showAllChatrooms' or '/showallchatrooms' or '/SHOWALLCHATROOMS'
                //all are accepted this way
                userInputSentToServer = userInputSentToServer.toLowerCase();
                //request to exit the application
                if (userInputSentToServer.equals("/exit")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    System.out.println("Disconnected from server.");
                    tcpSocket.close();
                    break;
                } else if (userInputSentToServer.equals("/chatoff")) {
                    if (chat.isAlive()) {
                        chat.wait();
                        System.out.println("No longer receiving notifications from other users.");
                    }
                } else if (userInputSentToServer.equals("/chaton")) {
                    if (!chat.isAlive()) {
                        chat.notify();
                        System.out.println("Other users can now message you.");
                    }
                } else if (userInputSentToServer.equals("/keystrokeson")) {
                    chat.setShowKeystrokes(true);
                    System.out.println("You can now see all the characters typed to you.");
                } else if (userInputSentToServer.equals("/keystrokesoff")) {
                    chat.setShowKeystrokes(false);
                    System.out.println("You can no longer see what other users type to you.");
                } else if (userInputSentToServer.equals("/whisper")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the user you want to chat with?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        User wantedUser = (User) TCPSocketService.receiveObject(tcpSocket);
                        if (wantedUser != null) {
                            System.out.println("What is the message you want to sent to him?");
                            run = true;
                            //not really needed but simulating that user is typing:
                            userInputSentToServer = userInput.readLine();
                            break;
                        } else if (wantedUser == null) {
                            System.out.println("User does not exist.");
                            continue;
                        }
                    }
                } else if (userInputSentToServer.equals("/showallchatrooms")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    try {
                        ArrayList<Chatroom> chatrooms = (ArrayList<Chatroom>) TCPSocketService.receiveObject(tcpSocket);
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
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    try {
                        ArrayList<User> users = (ArrayList) TCPSocketService.receiveObject(tcpSocket);
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
                } else if (userInputSentToServer.equals("/joinchatroom")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the chatroom?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        String chatroomNameExists = (String) TCPSocketService.receiveObject(tcpSocket);
                        if (chatroomNameExists.equals("0")) {
                            System.out.println("Chatroom name does not exist, please input the correct name.");
                            continue;
                        } else if (chatroomNameExists.equals("1")) {
                            System.out.println("You have successfully joined the chatroom.");
                            break;
                        } else if (chatroomNameExists.equals("2")) {
                            while (true) {
                                System.out.println("What is the password of the chatroom?");
                                userInputSentToServer = userInput.readLine();
                                TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                                boolean joined = (boolean) TCPSocketService.receiveObject(tcpSocket);
                                if (joined) {
                                    System.out.println("You have successfully joined the chatroom.");
                                    break;
                                } else if (!joined) {
                                    System.out.println("Password is incorrect, please type the correct password.");
                                    continue;
                                }
                            }
                            break;
                        }
                    }
                } else if (userInputSentToServer.equals("/kickfromchatroom")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the chatroom?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        boolean chatroomNameExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        if (chatroomNameExists) {
                            while (true) {
                                System.out.println("What is the name of the user you want to kick?");
                                userInputSentToServer = userInput.readLine();
                                TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                                boolean userKicked = (boolean) TCPSocketService.receiveObject(tcpSocket);
                                if (userKicked) {
                                    System.out.println("User has successfully been kicked from the chatroom.");
                                    break;
                                } else if (!userKicked) {
                                    System.out.println("User does not exist in this group or you cannot kick yourself.");
                                    continue;
                                }
                            }
                            break;
                        } else if (!chatroomNameExists) {
                            System.out.println("Chatroom name does not exist or you are not the owner.");
                            continue;
                        }
                    }
                }

                //this should be changed to TCP or more efficient algorithm needs to be designed
                //request to create a new chatroom
                else if (userInputSentToServer.equals("/createchatroom")) {
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Exiting...");


    }
}
