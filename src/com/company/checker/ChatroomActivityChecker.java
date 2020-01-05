package com.company.checker;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.google.common.collect.Multimap;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//thread that mainly handles chatrooms that are inactive, all inactive chatrooms are deleted
//concerns chatroom activity, checks for expired chatrooms based on the universalChatroomDeletionTime attribute, if a chatroom exceeds that number
// then it is deleted
public class ChatroomActivityChecker extends ActivityChecker {

    //a sort of 'custom latch' made for communication between chatroomActivityChecker thread and MulticastPublisher thread
    //the Checkers send a multimap key/value pair to the Publisher and then 'locks', waiting for the Publisher to finish with the pair.
    //When the Publisher finishes with the pair, it sets the latch to 0 and the Checker 'unlocks' and sets the next pair
    AtomicInteger latch;

    //this attributes control the time that a chatroom is allowed to stay on without anyone typing in it
    //if a chatroom exceeds this number (represented in minutes) it gets deleted
    private int universalChatroomDeletionTime;

    //constructor
    public ChatroomActivityChecker(List<Chatroom> chatrooms, Multimap<Chatroom, Message> pendingChatroomMessages, AtomicInteger latch) {
        super(chatrooms, pendingChatroomMessages);
        //default chatroom deletion threshold is 1 minute
        this.universalChatroomDeletionTime = 1;
        this.latch = latch;
    }

    @Override
    public void run() {
        while (true) {
            //for some reason, if you remove this, the thread sleeps(?) or something like that and it never checks if the multimap is empty, this is what I call  M A G I C
            this.isAlive();
            if (!chatrooms.isEmpty()) {
                Iterator<Chatroom> iter = chatrooms.iterator();
                while (iter.hasNext()) {
                    Chatroom toBeDeletedChatroom = iter.next();
                    if (calculateMinutesFromNow(toBeDeletedChatroom.getLastActive()) >= universalChatroomDeletionTime) {
                        //notifying all users belonging to this group that it does not exist anymore, so they can remove the multicast ip from their sockets
                        //lets say we insert a unique sequence
                        //in this case, no sender is needed as this is sent by the server itself
                        Message notification = new Message("[{[FOR_IDLE]}]|><|" + toBeDeletedChatroom.getName(), null);
                        pendingChatroomMessages.put(toBeDeletedChatroom, notification);
                        latch.set(1);
                        while (latch.get() == 1) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    break;
                }
            }
        }
    }

    //used by the TCP thread to set the time, set by the admin
    public void setUniversalChatroomDeletionTime(int universalChatroomDeletionTime) {
        this.universalChatroomDeletionTime = universalChatroomDeletionTime;
    }
}
