package com.company;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.User;
import com.company.listener.MulticastListener;
import com.company.listener.PrivateChatListener;
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
import java.util.Iterator;
import java.util.List;

public class Client {
    //used for keystrokes
    private static boolean run = false;

    public static void main(String[] args) throws IOException {

        //server IP
        String ipString = "192.168.1.13";
        //server port
        int port = 1;
        int startPort = 1;
        int stopPort = 65535;
        //port for receiving group messages
        int multicastPort;

        //in case the user wants to change IP/Port without editing the Java files
        if (args.length > 0) {
            ipString = args[0];
            port = Integer.parseInt(args[1]);
            multicastPort = Integer.parseInt(args[2]);
        } else {
            for (multicastPort = startPort; multicastPort <= stopPort; multicastPort += 1) {
                try {
                    System.out.println("Searching for a free port for multicast receiver....");
                    DatagramSocket datagramSocket = new DatagramSocket(multicastPort);
                    System.out.println("Found free port on: " + multicastPort);
                    datagramSocket.close();
                    break;
                } catch (IOException e) {
                }
            }
        }
        InetAddress serverAddr = InetAddress.getByName(ipString);

        String help = "\n" +
                "/showAllUsers - Shows all users currently registered on the server. \n" +
                "/showAllChatrooms - Shows all chatrooms that currently exist on  the server. \n" +
                "/createChatroom - Creates a new chatroom. \n" +
                "/deleteChatroom - Deletes an existing chatroom (Provided you are the owner). \n" +
                "/whisper - Send a message to someone in private. \n" +
                "/chatroom - Send a message to a chatroom. All other users that belong to the group will be able to read it. \n" +
                "/createChatroom - Create a new chatroom for messaging with multiple users. \n" +
                "/chatroomDeletionAdmin - Set the Universal Deletion Time (Reserved for admins). \n" +
                "/joinChatroom - Join an already existing chatroom and send and read messages from other users. \n" +
                "/kickFromChatroom - Kick a user out of a chatroom (Provided you are the owner). \n" +
                "/delegateChatroomOwnership - Delegate the ownership of a chatroom to another user (Provided you are the owner). \n" +
                "/permissions - Check for notifications concerning users that want to join one of your prohibited chatrooms. \n" +
                "/keystrokes - Gives the ability to see live in real-time the keystrokes of the other user (Support for one user only)  \n" +
                "/help - Shows all available commands again. \n" +
                "/exit - Exits the application. \n";
        System.out.println(help);

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
            //NOTICE: UDP most functionalities have changed to TCP now, so this UDP design is "overkill" but left none-the-less
            byte[] question;
            question = UDPSocketService.convertDistinguishableObjectToByteArray(5, checkAvailability);
            DatagramPacket testPacketToServer = new DatagramPacket(question, question.length, serverAddr, port);
            DatagramSocket UDPsocket = new DatagramSocket();
            UDPsocket.send(testPacketToServer);
            //then we get the answer, if we receive a message that says "true", then ping success
            //if we receive anything else ("false" in the 'perfect' case) warn that the connection might be weak
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

            //the main buffered reader, needs to be passed in other classes so that they also know,
            //when the user is actually typing and when the user is ready to see new messages
            //this application works in CMD, so there is no GUI and all messages are displayed in one line
            //by passing this to other classes, the classes that also print to the screen need to know
            //when the user is not typing anything and ready to see new notifications
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            //for all the users that requested to join a chatroom, the client will receive them one by one through multicasting
            //and then will be able to accept or deny requests/permissions
            List<Message> permissions = new ArrayList<>();

            //chatroom listener, the main thread listens for all the UDP multicasts, receives messages and prints them
            MulticastListener chatroomListener = new MulticastListener(multicastPort, permissions, userInput, datagramPacketMaxLength);
            chatroomListener.start();


            //first TCP request to the server, if user is registered then the client takes the username that was already saved in the server
            //and saves it as his
            //if user is not registered, then he is prompted to enter a unique username and register with it
            String isUserRegistered = "/isUserRegistered";
            User thisClient;
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
                        TCPSocketService.sendObject(multicastPort, tcpSocket);
                        thisClient = new User(username, thisMachine);
                        System.out.println("You have been successfully registered as '" + thisClient.getUsername() + "'.");
                        break;
                    }
                }
            }

            //NOTE TO LECTURERS: TURN TO FALSE IF THIS DOESN'T WORK OR REMOVE THIS COMPLETELY
            // Use false here to switch to hook instead of raw input (for the terminal)
            //key listener than records all the keys pressed by the client
            //once again, this is CMD so no support was found, and therefore a external library was needed called
            //'system-hook' for this to work
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

            //the user can now interact with the server in many ways, mainly with the help of the
            //existing commands
            String userInputSentToServer;
            //private chat listener, this class listens for all the private messages sent to the user (see /chattingPort in the server side for more info)
            //another connection made to the with the server, the other connection handles requests such as 'show all users' whereas this one listens for
            //messages
            PrivateChatListener chat = new PrivateChatListener(ipString, port, userInput);
            chat.start();

            //more info about the requests can be found in the server-side of this application
            while (true) {
                //always checks at the beginning if there are new permissions
                if (!permissions.isEmpty()) {
                    System.out.println("You have [" + permissions.size() + "] users asking for permission to join a group of yours.");
                }
                //waiting for input
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
                    //this makes the buffered reader ready, so it allows other classes to send any notifications they might have
                } else if (userInputSentToServer.equals("/refresh")) {
                    continue;
                    //for chatting in a group/chatroom
                    //the client is prompted to enter the chatroom name, if it exists and he is a member of it, he can proceed with sending a message
                } else if (userInputSentToServer.equals("/chatroom")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the chatroom?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        boolean chatroomExists = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        boolean userIsMember = (boolean) TCPSocketService.receiveObject(tcpSocket);
                        if (!chatroomExists) {
                            System.out.println("Chatroom name does not exist.");
                            continue;
                        } else if (chatroomExists) {
                            if (!userIsMember) {
                                System.out.println("You are not a member of this group");
                            } else if (userIsMember) {
                                while (true) {
                                    System.out.println("What is the message you want to send?");
                                    userInputSentToServer = userInput.readLine();
                                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                                    break;
                                }
                                break;
                            }
                        }
                    }
                }
                //here, we don't need to do any checking for permissions as user and group have been checked that they exist
                //so any more checking is unnecessary (this is checked in other parts of the code for example 'joinchatroom')
                else if (userInputSentToServer.equals("/permissions")) {
                    Iterator<Message> iter = permissions.iterator();
                    //normal iterator used here so we can also remove an element
                    while (iter.hasNext()) {
                        Message message = iter.next();
                        //basically asking the server the same thing (this allows the server-client to be in a synced loop)
                        userInputSentToServer = "/permissions";
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        System.out.println("'" + message.getSender().getUsername() + "' requested to join [" + message.getMessage() + "]. [y/n]?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        if (userInputSentToServer.equals("y")) {
                            TCPSocketService.sendObject(message.getSender().getUsername(), tcpSocket);
                            TCPSocketService.sendObject(message.getMessage(), tcpSocket);
                            System.out.println("User has now joined your chatroom.");
                        } else if (userInputSentToServer.equals("n")) {
                            System.out.println("User denied.");
                        }
                        iter.remove();
                    }
                }
                //reserved only for the admin
                else if (userInputSentToServer.equals("/chatroomdeletiontime")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    System.out.println("What is the new chatroom deletion time?");
                    userInputSentToServer = userInput.readLine();
                    int universalChatroomDeletionTime = Integer.parseInt(userInputSentToServer);
                    TCPSocketService.sendObject(universalChatroomDeletionTime, tcpSocket);
                    System.out.println("New time is set. (If you are the admin)");
                    //this enables/disables keystrokes,
                    //when enabled, the client will received the keystrokes of other users in real time
                    //when disabled, the torture is also stopped
                } else if (userInputSentToServer.equals("/keystrokes")) {
                    if (chat.isShowKeystrokes()) {
                        chat.setShowKeystrokes(false);
                        System.out.println("You can no longer see what other users type to you.");
                    } else if (!chat.isShowKeystrokes()) {
                        chat.setShowKeystrokes(true);
                        //supports only one person typing at the same time
                        //since the application has no GUI and it uses the command line
                        System.out.println("You can now see all the characters typed to you.");
                    }
                    //in case the client wants to whisper someone privately
                    //the name of the user is asked first, if he exists then the message is asked afterwards
                } else if (userInputSentToServer.equals("/whisper")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the user you want to send a private message to?");
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
                    //shows all chatrooms that exist on the server
                } else if (userInputSentToServer.equals("/showallchatrooms")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    try {
                        List<Chatroom> chatrooms = (List<Chatroom>) TCPSocketService.receiveObject(tcpSocket);
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
                        List<User> users = (List) TCPSocketService.receiveObject(tcpSocket);
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
                        List<User> chatroomUsers = (List) TCPSocketService.receiveObject(tcpSocket);
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
                //request to delegate chatroom ownership to a new owner provided the client is the current owner and the new owner exists in the group
                //the chatroom name is asked first, if it exists then the server proceeds to ask for new owner name, if new owner exists and is part of the chatroom
                //make him the new owner
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
                    //in case the client wants to join a new chatroom
                    //the chatroom name is asked first
                    //then return the policy, if policy is 0 it means that the chatroom does not exist
                    //if policy is 1 then the client has joined the chatroom
                    //if the policy is 2 then ask for password
                    //if policy is 3 notify client that the permission will be notified with the new request
                } else if (userInputSentToServer.equals("/joinchatroom")) {
                    TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                    while (true) {
                        System.out.println("What is the name of the chatroom?");
                        userInputSentToServer = userInput.readLine();
                        TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                        String chatroomNameExists = (String) TCPSocketService.receiveObject(tcpSocket);
                        System.out.println(chatroomNameExists);
                        //0 means chatroom does not exist, 1-3 are the respective policies
                        if (chatroomNameExists.equals("0")) {
                            System.out.println("Chatroom name does not exist, please input the correct name.");
                            continue;
                        } else if (chatroomNameExists.equals("1")) {
                            InetAddress multicastAddress = (InetAddress) TCPSocketService.receiveObject(tcpSocket);
                            System.out.println(multicastAddress);
                            chatroomListener.addChatroomMulticastAddress(multicastAddress);
                            System.out.println("You have successfully joined the chatroom.");
                        } else if (chatroomNameExists.equals("2")) {
                            while (true) {
                                System.out.println("What is the password of the chatroom?");
                                userInputSentToServer = userInput.readLine();
                                TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                                boolean joined = (boolean) TCPSocketService.receiveObject(tcpSocket);
                                if (joined) {
                                    InetAddress multicastAddress = (InetAddress) TCPSocketService.receiveObject(tcpSocket);
                                    System.out.println(multicastAddress);
                                    chatroomListener.addChatroomMulticastAddress(multicastAddress);
                                    System.out.println("You have successfully joined the chatroom.");
                                    break;
                                } else if (!joined) {
                                    System.out.println("Password is incorrect, please type the correct password.");
                                    continue;
                                }
                            }
                            //this is done through UDP multicast now
                        } else if (chatroomNameExists.equals("3")) {
                            System.out.println("Permission request has been sent to owner. You will be notified.");
                        }
                        break;
                    }
                    //in case the owner wants to kick someone from their group
                    //ask for chatroom name first,
                    //if it exists, ask for member name then
                    //if member exists then the server kicks him
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
                    //mainly for debugging, allows the client to know the multicast IPs of the chatrooms he is a part of
                } else if (userInputSentToServer.equals("/multicast")) {
                    chatroomListener.showMulticast();
                }
                //request to create a new chatroom
                //ask for chatroom name first, if it is not taken then
                //ask for policy, if policy is 2 (password-protected) ask for password
                //finally, ask for time limit (in minutes) for a user to send a message before he gets kicked
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
                                //this should be encrypted when sent (unless it already is [?])
                                TCPSocketService.sendObject(userInputSentToServer, tcpSocket);
                            }
                            InetAddress multicastAddress = (InetAddress) TCPSocketService.receiveObject(tcpSocket);
                            chatroomListener.addChatroomMulticastAddress(multicastAddress);
                            System.out.println("What is the time limit (in minutes) for a user to send a message before he gets kicked?");
                            userInputSentToServer = userInput.readLine();
                            int kickTime = Integer.parseInt(userInputSentToServer);
                            TCPSocketService.sendObject(kickTime, tcpSocket);
                            System.out.println("Your chatroom has been succesfully added on the server.");
                            break;
                        }
                    }
                    //request to delete a chatroom
                    //provided the chatroom exists and that the owner is the currect admin,
                    //the server will delete the chatroom when it receives this request
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
                    // a UDP packet is sent to the server (just a show-case of the authors skill of developing both UDP [along with /availability command] and TCP)
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
