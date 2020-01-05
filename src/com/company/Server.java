package com.company;

import com.company.checker.ChatroomActivityChecker;
import com.company.checker.UserActivityChecker;
import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.Record;
import com.company.entity.User;
import com.company.thread.MulticastPublisher;
import com.company.thread.TCPThread;
import com.company.thread.UDPThread;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    public static void main(String[] args) {
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

            //allows us to know the server's IP so we can show it on the console, then have clients connect to it
            InetAddress thisMachine = null;
            try {
                thisMachine = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            assert thisMachine != null;
            System.out.println("Server's IP Address is: " + thisMachine.getHostAddress() + "\n");


            // The port we'll listen on
            SocketAddress localport = new InetSocketAddress(port);

            // Create and bind a tcp channel to listen for connections on.
            ServerSocketChannel tcpServer = ServerSocketChannel.open();
            tcpServer.socket().bind(localport);

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

            //-----------------------------------------
            //we are turning the following data structures into synchronized lists so that we can do operations such as add and remove
            // when multiple threads are using them
            //-----------------------------------------

            //all the users of the chat application
            ArrayList<User> usersUnsynced = new ArrayList<>();
            List<User> users = Collections.synchronizedList(usersUnsynced);

            //all pending (private) messages that the application receives from the user
            //key is the user that it is meant for, and value is the message that contains the String sentence and the user who sent it (sender)
            //requires google collections/guava library
            Multimap<User, Message> pendingChatMessagesUnsynced = ArrayListMultimap.create();
            Multimap<User, Message> pendingChatMessages = Multimaps.synchronizedMultimap(pendingChatMessagesUnsynced);

            //similarly to the above, this concerns all the chatroom messages from users
            //key is the chatroom it concerns, and value is the message that contains String sentence and the sender
            Multimap<Chatroom, Message> pendingChatroomMessagesUnsynced = ArrayListMultimap.create();
            Multimap<Chatroom, Message> pendingChatroomMessages = Multimaps.synchronizedMultimap(pendingChatroomMessagesUnsynced);

            //all the chatrooms/groups of the application
            ArrayList<Chatroom> chatroomsUnsynced = new ArrayList<>();
            List<Chatroom> chatrooms = Collections.synchronizedList(chatroomsUnsynced);

            //a set of "records" that contain the active of each user (when was the last time they sent a message in a particular chatroom)
            ArrayList<Record> userActivityUnsynced = new ArrayList<>();
            List<Record> userActivity = Collections.synchronizedList(userActivityUnsynced);

            //a sort of 'custom latch' made for communication between chatroomActivityChecker thread and MulticastPublisher
            //the Checkers send a multimap key/value pair to the Publisher and then 'locks', waiting for the Publisher to finish with the pair.
            //When the Publisher finishes with the pair, it sets the latch to 0 and the Checker 'unlocks' and sets the next pair
            AtomicInteger latch = new AtomicInteger(0);

            //main thread that handles all the multicast messages, mainly handling group messages and notifications to users
            MulticastPublisher chatroomMessagePublisher = new MulticastPublisher(chatrooms, pendingChatroomMessages, users, latch);
            chatroomMessagePublisher.start();

            //main thread that concerns user activity, checks for expired records and manages/kicks users that have not messaged in a chat for a threshold
            UserActivityChecker userActivityChecker = new UserActivityChecker(userActivity, chatrooms, pendingChatroomMessages);
            userActivityChecker.start();

            //similarly to the userActivityChecker, this main thread handles chatrooms that are inactive, all inactive chatrooms are deleted
            ChatroomActivityChecker chatroomActivityChecker = new ChatroomActivityChecker(chatrooms, pendingChatroomMessages, latch);
            chatroomActivityChecker.start();
            //-----------------------------------------


            //maximum length of received/sent UDP packets
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
                        // whether something happened on the TCP or UDP channel
                        if (key.isAcceptable() && c == tcpServer) {
                            //new TCP request accepted and a new thread that will handle the client connection is accepted
                            new TCPThread(tcpServer.accept(), users, chatrooms, pendingChatMessages, pendingChatroomMessages, userActivity, chatroomActivityChecker).start();
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