package com.company.entity;

import java.io.Serializable;

public class Message implements Serializable {

    private String character;
    private User sender;
    private int keyValue;

    public Message(String character, User sender, int keyValue) {
        this.character = character;
        this.sender = sender;
        this.keyValue = keyValue;
    }

    public Message(String character, User sender) {
        this.character = character;
        this.sender = sender;
    }

    public int getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(int keyValue) {
        this.keyValue = keyValue;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }
}
