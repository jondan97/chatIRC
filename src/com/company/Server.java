package com.company;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.User;
import com.company.thread.MulticastPublisher;
import com.company.thread.TCPThread;
import com.company.thread.UDPThread;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class Server {
//    static Multimap<Chatroom, Message> pendingChatroomMessages = ArrayListMultimap.create();
//    static ArrayList<Chatroom> chatrooms = new ArrayList<>();

    public static void main(String args[]) {
        try {
            //port that the server will run on, it is auto-configured below
            int port;
            int startPort = 1;
            int stopPort = 65535;

            if (args.length > 0)
                port = Integer.parseInt(args[0]);
            else {
                //we dont actually need a datagramSocket, but we use it here in order to find a free port
                //for our server
                for (port = startPort; port <= stopPort; port += 1) {
                    try {
                        System.out.println("Searching for a free port to start server on....");
                        DatagramSocket datagramSocket = new DatagramSocket(port);
                        System.out.println("Started server on port: " + port);
                        datagramSocket.close();
                        break;
                    } catch (IOException e) {
                    }
                }
            }

            //allows us to know the server's IP
            InetAddress thisMachine = null;
            try {
                thisMachine = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            System.out.println("Server's IP Address is: " + thisMachine.getHostAddress() + "\n");




            // The port we'll listen on
            SocketAddress localport = new InetSocketAddress(port);

            // Create and bind a tcp channel to listen for connections on.
            ServerSocketChannel tcpServer = ServerSocketChannel.open();
            tcpServer.socket().bind(localport);

//            int chattingPort;
//            for (chattingPort = startPort; chattingPort <= stopPort; chattingPort += 1) {
//                try {
//                    System.out.println("Searching for a free port to start server on....");
//                    DatagramSocket datagramSocket = new DatagramSocket(chattingPort);
//                    System.out.println("Started server on port: " + chattingPort);
//                    datagramSocket.close();
//                    break;
//                } catch (IOException e) {
//                }
//            }
            // The port we'll listen on
//            SocketAddress localChattingPort = new InetSocketAddress(5678);
//            ServerSocketChannel tcpServerChat = ServerSocketChannel.open();
//            tcpServerChat.socket().bind(localChattingPort);


            // Also create and bind a DatagramChannel to listen on.
            DatagramChannel udpServer = DatagramChannel.open();
            udpServer.socket().bind(localport);

            // Specify non-blocking mode for both channels, since our
            // Selector object will be doing the blocking for us.
            tcpServer.configureBlocking(false);
            udpServer.configureBlocking(false);

            // The Selector object is what allows us to block while waiting
            // for activity on either of the two channels.
            Selector selector = Selector.open();

            // Register the channels with the selector, and specify what
            // conditions (a connection ready to accept, a datagram ready
            // to read) we'd like the Selector to wake up for.
            tcpServer.register(selector, SelectionKey.OP_ACCEPT);
            udpServer.register(selector, SelectionKey.OP_READ);
            //all the users of the chat application
            ArrayList<User> users = new ArrayList<>();
            //test user
            users.add(new User("ska"));
            //-----------------------------------------
            Multimap<User, Message> pendingChatMessages = ArrayListMultimap.create();
            Multimap<Chatroom, Message> pendingChatroomMessages = ArrayListMultimap.create();
            ArrayList<Chatroom> chatrooms = new ArrayList<>();
            MulticastPublisher chatroomMessagePublisher = new MulticastPublisher();
            //all the chatrooms of the application
            chatroomMessagePublisher.setChatrooms(chatrooms);
            chatroomMessagePublisher.setPendingChatroomMessages(pendingChatroomMessages);
            chatroomMessagePublisher.setUsers(users);
            //pendingChatroomMessages.put(new Chatroom(), new Message("ESKETIT", new User("ska")));
            chatroomMessagePublisher.start();
            //-----------------------------------------


            //maximum length of received/sent packets
            int datagramPacketMaxLength = 3500;
            // Now loop forever, processing client connections
            while (true) {
                try { // Handle per-connection problems below
                    //System.out.println(selector.select() + " (selected)");
                    // Wait for a client to connect
                    selector.select();
                    // Get the SelectionKey objects for the channels that have
                    // activity on them. These are the keys returned by the
                    // register() methods above. They are returned in a
                    // java.util.Set.
                    Set<SelectionKey> keys = selector.selectedKeys();
                    // Iterate through the Set of keys.
                    for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
                        // Get a key from the set, and remove it from the set
                        SelectionKey key = i.next();
                        //System.out.println(key+ " (key)");
                        i.remove();

                        // Get the channel associated with the key
                        Channel c = key.channel();
                        //System.out.println(c.toString() + " (channel)");

                        // Now test the key and the channel to find out
                        // whether something happend on the TCP or UDP channel
                        if (key.isAcceptable() && c == tcpServer) {
                            Chatroom chatroom = new Chatroom();
                            chatroom.setName("mc");
                            chatroom.setPolicy("2");
                            chatroom.setPassword("cM");
                            chatroom.setOwner(new User("adolfos", InetAddress.getByName("192.168.1.13")));
                            chatroom.setMulticastAddress(InetAddress.getByName("239.0.0.1"));
                            if (!chatrooms.contains(chatroom)) {
                                chatrooms.add(chatroom);
                            }

                            new TCPThread(tcpServer.accept(), users, chatrooms, pendingChatMessages, pendingChatroomMessages).start();
                        } else if (key.isReadable() && c == udpServer) {
                            //if we don't declare the buffer inside the loop, then in the next iteration of the while loop,
                            //the buffer contents will be lost and the active thread will be left with a null instance,
                            ByteBuffer receiveBuffer = ByteBuffer.allocate(datagramPacketMaxLength);
                            // we save the address it was received from and
                            //we convert it to a InetSocketAddress
                            SocketAddress s = udpServer.receive(receiveBuffer);
                            InetSocketAddress clientNetworkDetails = (InetSocketAddress) s;
                            //we pass into the thread all the info that we need
                            new UDPThread(udpServer, receiveBuffer.array(), clientNetworkDetails, users, chatrooms).start();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.exit(1);
        }
    }
}