package com.company.entity;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class User implements Serializable {

    private String username;
    //details include IP address etc.
    private InetAddress details;
    //this allows the server to know which port the UDP socket (on the client-side) listens to, this information is
    //intially transferred during the /isUserRegistered command
    private int multicastPort;

    //constructor that requires username and details
    //this is the only one that should be used all over the application (except for 1-2 cases)
    public User(String username, InetAddress details) {
        this.username = username;
        this.details = details;
    }

    //mainly created for test purposes (when the authors needed a fast user)
    public User(String username) {
        this.username = username;
    }

    //the authors expect that the reader is already familiar with getters/setters

    public String getUsername() {
        return username;
    }


    public InetAddress getDetails() {
        return details;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    //the authors believe that Username and Details are enough to distinguish between Users
    //username is unique but details are needed in order to create a somewhat "session" when connected to the server or
    //each time a user requests something
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return getUsername().equals(user.getUsername()) &&
                getDetails().equals(user.getDetails());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getDetails());
    }
}
