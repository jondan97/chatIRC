package com.company.entity;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class User implements Serializable {

    private String username;
    //role could be either admin or normal user, might never be implemented
    private String role;
    //details include IP address etc.
    private InetAddress details;
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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public InetAddress getDetails() {
        return details;
    }

    public void setDetails(InetAddress details) {
        this.details = details;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    //the authors believe that Username and Details are enough to distinguish between Users
    //username is unique but details are needed to order to create a somewhat "session"
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
