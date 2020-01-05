package com.company.checker;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.company.entity.Record;
import com.company.entity.User;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */

//thread that checks for user activity
//checks all records and if a record is expired, meaning that a user hasn't typed in a chatroom for too long, he gets kicked
//the threshold is set by the owner
public class UserActivityChecker extends ActivityChecker {

    //list of all the records that contain Chatroom-User association
    private List<Record> userActivity;

    //constructor
    public UserActivityChecker(List<Record> userActivity, List<Chatroom> chatrooms, Multimap<Chatroom, Message> pendingChatroomMessages) {
        super(chatrooms, pendingChatroomMessages);
        this.userActivity = userActivity;
    }

    @Override
    public void run() {
        boolean expiredRecordFound = false;
        while (true) {
            //for some reason, if you remove this, the thread sleeps(?) or something like that and it never checks if the multimap is empty, this is what I call  M A G I C
            this.isAlive();
            if (!userActivity.isEmpty()) {
                //resetting
                Record wantedRecord = null;
                for (Record record : new ArrayList<>(userActivity)) {
                    //calculate time, if bigger than chatroom 'default' time, then kick user
                    long minuteDifferenceFromNow = calculateMinutesFromNow(record.getTime());
                    if (minuteDifferenceFromNow >= (long) record.getChatroom().getKickTime()) {
                        Chatroom wantedChatroom = record.getChatroom();
                        for (Chatroom room : new ArrayList<>(chatrooms)) {
                            if (room.equals(wantedChatroom)) {
                                for (User wantedUser : new ArrayList<>(wantedChatroom.getUsers())) {
                                    if (wantedUser.equals(record.getUser())) {
                                        wantedRecord = record;
                                        expiredRecordFound = true;
                                        Message notification = new Message("[{[GOT_KICKED]}]|><|", record.getUser());
                                        //send a kicking request to the multicast publisher thread
                                        pendingChatroomMessages.put(wantedChatroom, notification);
                                        System.out.println("'" + record.getUser().getUsername() + "' was kicked from [" + record.getChatroom().getName() + "] because he was idle for too long.");
                                        break;
                                    }
                                }
                            }
                        }
                        if (expiredRecordFound) {
                            expiredRecordFound = false;
                            break;
                        }
                    }
                }
                if (wantedRecord != null) {
                    userActivity.remove(wantedRecord);
                }
            }
        }
    }
}
