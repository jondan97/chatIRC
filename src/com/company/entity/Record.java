package com.company.entity;

import java.time.LocalDateTime;
import java.util.Objects;
/*
 * AUTHORS
 * IOANNIS DANIIL
 * MICHAEL-ANGELO DAMALAS
 * ALEX TATTOS
 * CHRIS DILERIS
 * */

public class Record {

    //the user who is associated with this record
    private User user;
    //the time this record was first created, in future references this time might also get updated to the
    //time of that particular moment (that it is needed)
    private LocalDateTime time;
    //the chatroom that is associated with this record
    private Chatroom chatroom;

    //constructor
    public Record(User user, Chatroom chatroom) {
        this.user = user;
        this.chatroom = chatroom;
        time = LocalDateTime.now();
    }

    public Chatroom getChatroom() {
        return chatroom;
    }

    public void setChatroom(Chatroom chatroom) {
        this.chatroom = chatroom;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    //the association Chatroom-Owner is unique enough for this 'equals' to have value
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Record)) return false;
        Record record = (Record) o;
        return getUser().equals(record.getUser()) &&
                getChatroom().equals(record.getChatroom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUser(), getChatroom());
    }
}



