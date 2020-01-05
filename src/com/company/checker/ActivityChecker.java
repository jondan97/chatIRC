package com.company.checker;

import com.company.entity.Chatroom;
import com.company.entity.Message;
import com.google.common.collect.Multimap;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */

//parent class for the two children: chatroom and user activity checker
//this mainly works as a "grouped methods/attributes" class that both children checkers share
public class ActivityChecker extends Thread {

    //all chatrooms of the application
    protected List<Chatroom> chatrooms;
    //all pending chatroom messages of the application
    protected Multimap<Chatroom, Message> pendingChatroomMessages;

    //constructor that only the chatroomActivityChecker uses
    public ActivityChecker(List<Chatroom> chatrooms, Multimap<Chatroom, Message> pendingChatroomMessages) {
        this.chatrooms = chatrooms;
        this.pendingChatroomMessages = pendingChatroomMessages;
    }

    //this allows the thread to calculate how long ago was a LocalDateTime record from the present in minutes
    //takes local date time
    //returns difference in minutes (long type)
    public long calculateMinutesFromNow(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        return ChronoUnit.MINUTES.between(time, now);
    }
}
