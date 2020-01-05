package com.company.entity;

import java.io.Serializable;

public class Message implements Serializable {

    //message that is contained along with this class
    //in private chat this is not a full sentence but a single character
    private String message;
    //who send the message
    private User sender;
    //in the case of private chatting, the server needs to know the key value of the character, for example '13' means
    //'SPACE' on the keyboard
    private int keyValue;

    //constructor used for private chatting
    public Message(String message, User sender, int keyValue) {
        this.message = message;
        this.sender = sender;
        this.keyValue = keyValue;
    }

    //constructor used for multicasting
    public Message(String message, User sender) {
        this.message = message;
        this.sender = sender;
    }

    public int getKeyValue() {
        return keyValue;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getSender() {
        return sender;
    }
}
