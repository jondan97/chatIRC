package com.company.thread;

import com.company.checker.ChatroomActivityChecker;
import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.Record;
import com.company.entity.User;
import com.company.service.TCPSocketService;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */

public class TCPThread extends Thread {

    //current client serving
    User currentUser;
    //conncetion channel that handles the current connection
    private SocketChannel socket;
    //all users in application
    private List<User> users;
    //all chatrooms in application
    private List<Chatroom> chatrooms;
    //all pending (private) chat messages that need to be sent
    private Multimap<User, Message> pendingChats;
    //all pending chatroom/group messages that need to be sent
    private Multimap<Chatroom, Message> pendingChatroomMessages;
    //user activity containing records
    private List<Record> userActivity;
    //this is used in case the server admin decides to change the idle time for a chatroom before it gets deleted
    private ChatroomActivityChecker chatroomActivityChecker;


    //main constructor and only constructor
    public TCPThread(SocketChannel socket, List<User> users, List<Chatroom> chatrooms, Multimap<User, Message> pendingChats, Multimap<Chatroom, Message> pendingChatroomMessages, List<Record> userActivity, ChatroomActivityChecker chatroomActivityChecker) {
        this.socket = socket;
        this.users = users;
        this.chatrooms = chatrooms;
        this.pendingChats = pendingChats;
        this.pendingChatroomMessages = pendingChatroomMessages;
        this.userActivity = userActivity;
        this.chatroomActivityChecker = chatroomActivityChecker;
    }

    @Override
    public void run() {
        //for better understanding and for better handling (as the socket is what we will mainly use and not the channel itself)
        Socket client = socket.socket();

        //this is not the best solution but in order to avoid concurrent modification exceptions
        //on the same list, we will have to copy it and work on searching on that
        for (User user : new ArrayList<>(users)) {
            //this detects if the user already exists, if he exists then set him as current user
            //this would be safer if I could know the mac-address but going into too much security
            //also for some reason some host names are saved in lowercase (no idea why) while they are
            //capitalized when first received
            if (client.getInetAddress().getHostName().toUpperCase().equals(user.getDetails().getHostName().toUpperCase())) {
                currentUser = user;
            }
        }

        while (true) {
            //the initial received message will always be a String as the user is expected to insert various commands such as /whisper
            String receivedMessage;
            try {
                //the actual command is being read here
                receivedMessage = (String) TCPSocketService.receiveObject(client);
                if (currentUser != null) {
                    System.out.println("Message received: " + receivedMessage + " from " + currentUser.getUsername() + ".");
                } else if (currentUser == null) {
                    System.out.println("Message received: " + receivedMessage + " from an unknown user");

                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("User disconnected.");
                break;
            }
            //we check here if the user is registered, if he is already registered then the server sends the client the saved credentials
            //if the user is not registered, then a username is asked and the client is registered with that (unique) username
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
                        System.out.println("User disconnected.");
                        break;
                    }
                } else {
                    try {
                        userIsRegistered = false;
                        TCPSocketService.sendObject(userIsRegistered, client);
                        while (true) {
                            boolean usernameExists = false;
                            String usernameRequested = (String) TCPSocketService.receiveObject(client);
                            for (User user : new ArrayList<>(users)) {
                                //we need to check here that the client has not inserted the same sequence of characters but differently capitalized
                                if (user.getUsername().toLowerCase().equals(usernameRequested.toLowerCase())) {
                                    usernameExists = true;
                                    TCPSocketService.sendObject(usernameExists, client);
                                }
                            }
                            if (!usernameExists) {
                                User registeredUser = new User(usernameRequested, client.getInetAddress());
                                users.add(registeredUser);
                                currentUser = registeredUser;
                                TCPSocketService.sendObject(usernameExists, client);
                                int userMulticastPort = (int) TCPSocketService.receiveObject(client);
                                registeredUser.setMulticastPort(userMulticastPort);
                                break;
                            }
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("User disconnected.");
                        break;
                    }
                }
                //in case the client wants to whisper someone, firstly the recipient username is asked, and then the message the client wants to send
            } else if (receivedMessage.equals("/whisper")) {
                System.out.println("'" + currentUser.getUsername() + "' wants to whisper someone.");
                User wantedUser = null;
                while (true) {
                    try {
                        String requestedUserName = (String) TCPSocketService.receiveObject(client);
                        //checking if user exists
                        for (User user : new ArrayList<>(users)) {
                            if (user.getUsername().toLowerCase().equals(requestedUserName.toLowerCase())) {
                                wantedUser = user;
                                TCPSocketService.sendObject(wantedUser, client);
                                System.out.println("User '" + wantedUser.getUsername() + "' exists so he will be whispered.");
                                break;
                            }
                            wantedUser = null;
                        }
                        if (wantedUser != null) {
                            //asks for the message now, since a 'keystroke' functionality was asked, the server receives the character one by one, and adds them to the
                            //pending messages map
                            while (true) {
                                String character = (String) TCPSocketService.receiveObject(client);
                                int virtualKeyCode = (int) TCPSocketService.receiveObject(client);
                                Message message = new Message(character, currentUser, virtualKeyCode);
                                pendingChats.put(wantedUser, message);
                                //13 equals to "enter"
                                if (virtualKeyCode == 13) {
                                    break;
                                }
                            }
                        }
                        if (wantedUser == null) {
                            TCPSocketService.sendObject(wantedUser, client);
                            System.out.println("User that was requested to be whispered by '" + currentUser.getUsername() + "' does not exist.");
                            continue;
                        }
                        break;
                    } catch (IOException e) {
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //sends a list with all the chatrooms to the client
            } else if (receivedMessage.equals("/showallchatrooms")) {
                System.out.println("Request to show all chatrooms was done by '" + currentUser.getUsername() + "'.");
                try {
                    TCPSocketService.sendObject(chatrooms, client);
                } catch (IOException e) {
                    System.out.println("User disconnected.");
                    break;
                }
                //send a list with all users to the client
            } else if (receivedMessage.equals("/showallusers")) {
                System.out.println("Request to show all users was done by '" + currentUser.getUsername() + "'.");
                try {
                    TCPSocketService.sendObject(users, client);
                } catch (IOException e) {
                    System.out.println("User disconnected.");
                    break;
                }
                //sends a list with all users belonging to a particular chatroom
            } else if (receivedMessage.equals("/showchatroomusers")) {
                System.out.println("Request to see all users of a particular chatroom was done by '" + currentUser.getUsername() + "'.");
                try {
                    while (true) {
                        //receive the chatroom name
                        String chatroomName = (String) TCPSocketService.receiveObject(client);
                        Chatroom requestedChatroom = null;
                        boolean chatroomExists = false;
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
                            if (chatroom.getName().equals(chatroomName)) {
                                requestedChatroom = chatroom;
                                chatroomExists = true;
                            }
                        }
                        TCPSocketService.sendObject(chatroomExists, client);
                        if (chatroomExists) {
                            TCPSocketService.sendObject(requestedChatroom.getUsers(), client);
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("User disconnected.");
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                //if the client wants to delete a chatroom he is an owner of, firstly, the chatroom is checked if it exists, then a notification is sent to the
                //MulticastPublisher class which handles the notification that will be sent and which also deletes the chatroom (needs to get the multicast address
                //before it gets deleted so the actual deletion cannot be done here)
            } else if (receivedMessage.equals("/deletechatroom")) {
                System.out.println("Request to delete a chatroom was done by '" + currentUser.getUsername() + "'.");
                while (true) {
                    try {
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        Chatroom toBeDeletedChatroom = new Chatroom();
                        toBeDeletedChatroom.setName(requestedChatroomName);
                        //as if saying "if the owner of the room is the same user who requested to delete it"
                        toBeDeletedChatroom.setOwner(currentUser);
                        boolean chatroomDeleted = false;
                        for (Chatroom room : new ArrayList<>(chatrooms)) {
                            if (room.equals(toBeDeletedChatroom)) {
                                //notifying all users belonging to this group that it does not exist anymore, so they can remove the multicast ip from their sockets
                                //lets say we insert a unique sequence as the message for 'deletion'
                                Message notification = new Message("[{[FOR_DELETION]}]|><|" + room.getName(), currentUser);
                                pendingChatroomMessages.put(room, notification);
                                //actual deletion is done in the class: "MulticastPublisher", the server needs to notify the users first
                                chatroomDeleted = true;
                                TCPSocketService.sendObject(chatroomDeleted, client);
                                System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' has been deleted.");
                                break;
                            }
                        }
                        if (!chatroomDeleted) {
                            TCPSocketService.sendObject(chatroomDeleted, client);
                            System.out.println("Chatroom '" + toBeDeletedChatroom.getName() + "' does not exist or user is not the owner for it to be deleted.");
                        } else if (chatroomDeleted) {
                            break;
                        }
                    } catch (IOException e) {
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //in case the client wants to send(multicast) a message to a chatroom/group
                //the name of the chatroom is firstly asked, if it exists then the message is multicasted to all users that are connected to that group
            } else if (receivedMessage.equals("/chatroom")) {
                System.out.println("User '" + currentUser.getUsername() + "' requested to send a message to a chatroom.");
                while (true) {
                    try {
                        boolean chatroomExists = false;
                        boolean userIsMember = false;
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        Chatroom requestedChatroom = null;
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
                            //in case the client inserted it differently that the one that was initially registered with
                            if (chatroom.getName().toLowerCase().equals(requestedChatroomName.toLowerCase())) {
                                chatroomExists = true;
                                for (User u : new ArrayList<>(chatroom.getUsers())) {
                                    if (u.equals(currentUser)) {
                                        userIsMember = true;
                                        requestedChatroom = chatroom;
                                        break;
                                    }
                                }
                            }
                        }
                        TCPSocketService.sendObject(chatroomExists, client);
                        TCPSocketService.sendObject(userIsMember, client);
                        if (chatroomExists && userIsMember) {
                            String messageString = (String) TCPSocketService.receiveObject(client);
                            Message message = new Message(messageString, currentUser);
                            //new message was sent to that particular chatroom, so the record of that user within that chatroom needs to be updated
                            //so that he doesn't get kicked
                            Record updatedRecord = new Record(currentUser, requestedChatroom);
                            ListIterator<Record> iter = userActivity.listIterator();
                            //an actual iterator is used here so we can replace the record element
                            while (iter.hasNext()) {
                                Record record = iter.next();
                                if (record.equals(updatedRecord)) {
                                    //Replace element
                                    iter.set(updatedRecord);
                                    break;
                                }
                            }
                            //similarly to the updated record, the chatroom itself needs to also be updated that there was an activity, so it does not
                            //get deleted by the ChatroomActivityChecker class
                            Iterator<Chatroom> chatIter = chatrooms.iterator();
                            while (chatIter.hasNext()) {
                                Chatroom chatroom = chatIter.next();
                                if (chatroom.equals(requestedChatroom)) {
                                    chatroom.setLastActive(LocalDateTime.now());
                                    break;
                                }
                            }
                            pendingChatroomMessages.put(requestedChatroom, message);
                            break;
                        }
                    } catch (IOException e) {
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //in case the client wants to create a new chatroom, firstly the name is checked (if it already exists) and then the policy
                //and the time allowed for a user to be AFK without typing anything to the chat (if that time is passed, he gets kicked)
            } else if (receivedMessage.equals("/createchatroom")) {
                System.out.println("User '" + currentUser.getUsername() + "' requested to create a chatroom.");
                while (true) {
                    try {
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        boolean alreadyExists = false;
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
                            //again, ignoring differences
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
                            //check chatroom class for more info, 230.0.0.0 is reserved for 'permission notifications' sent to the users
                            //the actual IP starts from 230.0.0.1 and goes up to the final multicast available address (which is far away)
                            //if this is the first chatroom, add the first ip, if not, take the final chatroom entry, take the chatroom IP and find the next
                            //available IP
                            if (chatrooms.isEmpty()) {
                                InetAddress firstMulticastAddress = InetAddress.getByName("230.0.0.1");
                                TCPSocketService.sendObject(firstMulticastAddress, client);
                                requestedChatroom.setMulticastAddress(firstMulticastAddress);
                            } else if (!chatrooms.isEmpty()) {
                                InetAddress previousMulticastAddress = chatrooms.get(chatrooms.size() - 1).getMulticastAddress();
                                String nextMulticastAddressString = MulticastPublisher.getNextIPV4Address(previousMulticastAddress);
                                InetAddress nextMulticastAddress = InetAddress.getByName(nextMulticastAddressString);
                                requestedChatroom.setMulticastAddress(nextMulticastAddress);
                                TCPSocketService.sendObject(nextMulticastAddress, client);
                            }
                            int kickTime = (int) TCPSocketService.receiveObject(client);
                            requestedChatroom.setKickTime(kickTime);
                            chatrooms.add(requestedChatroom);
                            break;
                        }
                    } catch (IOException e) {
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //reserved for the admin only, this is one of the requirements: sets the universal deletion time for the chatrooms, if there is no activity
                //in a chatroom, the chatroom gets deleted automatically
            } else if (receivedMessage.equals("/chatroomdeletiontime")) {
                System.out.println("'" + currentUser.getUsername() + "' tried to change the deletion time.");
                try {
                    int universalChatroomDeletionTime = (int) TCPSocketService.receiveObject(client);
                    //in this simple application, we do not have login so anyone who enters the name 'admin' can also control this time
                    if (currentUser.getUsername().toLowerCase().equals("admin")) {
                        chatroomActivityChecker.setUniversalChatroomDeletionTime(universalChatroomDeletionTime);
                        System.out.println("Universal chatroom deletion time is now " + universalChatroomDeletionTime + " minutes.");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("User disconnected.");
                    break;
                }
                //in case the client wants to join a particular chatroom, firstly the name is checked (if it exists),
                //and then the policy, if the chatroom does not exist, then return 0 as the policy, if it exists take the policy
                //and return that to the client and act accordingly, if policy is 2 then ask for password etc.
                //finally, send the multicast address of the chatroom to the client so he can join it and receive group messages
            } else if (receivedMessage.equals("/joinchatroom")) {
                System.out.println("User '" + currentUser.getUsername() + "' requested to join a chatroom.");
                while (true) {
                    try {
                        String policy = "0";
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        System.out.println(requestedChatroomName);
                        Chatroom requestedChatroom = null;
                        //this is not the best solution but in order to avoid concurrent modification exceptions
                        //on the same list we will have to copy it and work on searching on that
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
                            if (chatroom.getName().toLowerCase().equals(requestedChatroomName.toLowerCase())) {
                                policy = chatroom.getPolicy();
                                requestedChatroom = chatroom;
                                break;
                            }
                        }
                        TCPSocketService.sendObject(policy, client);
                        System.out.println(policy);
                        boolean joinedChatroom = false;
                        switch (policy) {
                            case "0":
                                System.out.println("does not exist");
                                continue;
                            case "1":
                                System.out.println("free");
                                joinedChatroom = true;
                                requestedChatroom.getUsers().add(currentUser);
                                break;
                            case "2":
                                System.out.println("password");
                                boolean foundPassword = false;
                                while (true) {
                                    String password = (String) TCPSocketService.receiveObject(client);
                                    System.out.println(password);
                                    if (requestedChatroom.getPassword().equals(password)) {
                                        System.out.println("positive");
                                        requestedChatroom.getUsers().add(currentUser);
                                        foundPassword = true;
                                        TCPSocketService.sendObject(foundPassword, client);
                                        joinedChatroom = true;
                                        break;
                                    }
                                    TCPSocketService.sendObject(foundPassword, client);
                                }
                                break;
                            case "3":
                                System.out.println("notification");
                                //send to the owner a notification that a new user wants to join the group
                                Message notificationToOwner = new Message("[{[PERMISSION_ASKED]}]|><|", currentUser);
                                pendingChatroomMessages.put(requestedChatroom, notificationToOwner);
                                break;
                        }
                        if (joinedChatroom) {
                            TCPSocketService.sendObject(requestedChatroom.getMulticastAddress(), client);
                            Record record = new Record(currentUser, requestedChatroom);
                            userActivity.add(record);
                        }
                        break;
                    } catch (IOException e) {
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                //if the owner wants to kick someone from the chatroom, he firstly enters the name of the chatroom, the server checks if the chatroom
                //exists and if he is the owner, and then asks for the username of the 'to be kicked' user
            } else if (receivedMessage.equals("/kickfromchatroom")) {
                System.out.println("User '" + currentUser.getUsername() + "' requested to kick a user from a chatroom.");
                while (true) {
                    try {
                        String requestedChatroomName = (String) TCPSocketService.receiveObject(client);
                        boolean chatroomExists = false;
                        Chatroom currentChatroom = null;
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
                            //ignoring differences to characters
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
                                for (User user : new ArrayList<>(currentChatroom.getUsers())) {
                                    if (user.getUsername().toLowerCase().equals(userToBeKickedName.toLowerCase()) && !user.equals(currentUser)) {
                                        //we put 'user' because we only need to send that message to that particular user(that he was kicked), essentially
                                        //changing the multicast feature to a unicast for this case
                                        Message notification = new Message("[{[GOT_KICKED]}]|><|" + currentChatroom.getName(), user);
                                        pendingChatroomMessages.put(currentChatroom, notification);
                                        // we do not remove the user yet (similarly to chatroom deletion), we let the users know first and then we remove him
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
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                //in case the client wants to move ownership to somebody else that is in the group, chatroom name is checked and if client is the owner
                //then if the new owner is in the chatroom is checked
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
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
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
                                for (User user : new ArrayList<>(currentChatroom.getUsers())) {
                                    if (user.getUsername().toLowerCase().equals(newOwnerName.toLowerCase())) {
                                        userExists = true;
                                        newOwner = user;
                                        //user exists so let client know
                                        TCPSocketService.sendObject(userExists, client);
                                    }
                                }
                                if (!userExists) {
                                    //user does not exist
                                    TCPSocketService.sendObject(userExists, client);
                                }
                                //we do this break out here, because if we do it inside the for loop, we will be stuck in the while loop for ever
                                else if (userExists) {
                                    //if we swap ownership, then we also need to make the new owner
                                    //invulnerable to AFK kicking and the old owner vulnerable
                                    Record newOwnerRecord = new Record(newOwner, currentChatroom);
                                    Record oldOwnerRecord = new Record(currentUser, currentChatroom);
                                    userActivity.add(oldOwnerRecord);
                                    userActivity.remove(newOwnerRecord);
                                    currentChatroom.setOwner(newOwner);
                                    System.out.println("Owner has changed.");
                                    break;
                                }
                            }

                        }
                        //same for this while loop as well, so we somehow have to break
                        if (chatroomExists && userExists) {
                            break;
                        }

                    } catch (IOException e) {
                        System.out.println("User disconnected.");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                //this is an infinite loop that exists as long as the client is connected to the server
                //when the client first launches the application, he sents a request here, a thread is running
                //for all the requests that the client wants (such as whisper) and an extra thread is running
                //inside this loop.
                //This loop checks if the multimap "pendingchats" contains any messages/characters that concern the client
                //if it contains, then the server takes them one by one and sends them to the client
                //this is a second connection with the client and works under a different port than the normal connection (the chattingPort)
            } else if (receivedMessage.equals(("/chattingPort"))) {
                while (true) {
                    if (pendingChats.containsKey(currentUser)) {
                        Message msg = null;
                        for (Message message : new ArrayList<>(pendingChats.get(currentUser))) {
                            try {
                                TCPSocketService.sendObject(message, client);
                                msg = message;
                                break;
                            } catch (IOException e) {
                                System.out.println("Chatting connection has been closed.");
                                break;
                            }
                        }
                        pendingChats.remove(currentUser, msg);
                    }
                }
            }
            //the client wants to accept permissions that other users sent to him.
            //The chatrooms that have a policy of 3 (permission-required) need permission from the owner
            //therefore, the client (who is also the owner) lets the server know whether he accepts or denies a user one by one
            //if the client accepts a user, then the user is added to the chatroom user list
            //if not, the permission is ignored
            //once again, no need for checking if the chatroom/username are correct or exist
            // as this has been done in other parts of the code (see 'joinchatroom' for example)
            else if (receivedMessage.equals("/permissions")) {
                try {
                    String answer = (String) TCPSocketService.receiveObject(client);
                    if (answer.equals("y")) {
                        String username = (String) TCPSocketService.receiveObject(client);
                        String chatroomName = (String) TCPSocketService.receiveObject(client);
                        User wantedUser = null;
                        Chatroom wantedChatroom = null;
                        for (User user : new ArrayList<>(users)) {
                            if (user.getUsername().toLowerCase().equals(username.toLowerCase())) {
                                wantedUser = user;
                                break;
                            }
                        }
                        for (Chatroom chatroom : new ArrayList<>(chatrooms)) {
                            if (chatroom.getName().toLowerCase().equals(chatroomName.toLowerCase())) {
                                wantedChatroom = chatroom;
                                chatroom.getUsers().add(wantedUser);
                                //we also need to add the record which helps track the user's activity
                                Record record = new Record(wantedUser, wantedChatroom);
                                userActivity.add(record);
                                break;
                            }
                        }
                        pendingChatroomMessages.put(wantedChatroom, new Message("[{[PERMISSION_ACCEPTED]}]|><|", wantedUser));
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("User disconnected.");
                    break;
                }
                //the client wants to exits and lets the server know, so the connection is closed.
            } else if (receivedMessage.equals("/exit")) {
                try {
                    socket.close();
                    System.out.println("Connection has been closed.");
                    break;
                } catch (IOException e) {
                    System.out.println("User disconnected.");
                    break;
                }
            }
        }
        System.out.println("Closing thread...");
    }
}


