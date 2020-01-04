package com.company.entity;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;

public class Chatroom implements Serializable {
    User owner;
    //saved as a number but in case we want to swap it in the future with a title such as "Free", we leave it
    //as it is
    String policy;
    String name;
    String password;
    //list of users that belong in this chatroom
    ArrayList<User> users;
    //set by the owner
    int kickTime;
    //set by the server admin (who is going to be distinguished by a unique name)
    int deletionTime;
    //        224.0.1.0 	238.255.255.255 	Globally scoped (Internet-wide) multicast address
    //        239.0.0.0 	239.255.255.255 	Administratively scoped (local) multicast addresses
    InetAddress multicastAddress;

    //default constructor, only need to declare the array list
    public Chatroom() {
        users = new ArrayList<>();
    }

    //the authors expect that the reader is already familiar with getters/setters

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public int getKickTime() {
        return kickTime;
    }

    public void setKickTime(int kickTime) {
        this.kickTime = kickTime;
    }

    public int getDeletionTime() {
        return deletionTime;
    }

    public void setDeletionTime(int deletionTime) {
        this.deletionTime = deletionTime;
    }

    public InetAddress getMulticastAddress() {
        return multicastAddress;
    }

    public void setMulticastAddress(InetAddress multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    //this method is also shown to the user
    //(the authors are aware that toString is mainly for debugging)
    //the policy which is saved in numbers (1-3) is converted to
    //user-friendly text
    @Override
    public String toString() {
        String policyReadable = null;
        if (this.policy.equals("1")) {
            policyReadable = "Free";
        } else if (this.policy.equals("2")) {
            policyReadable = "Password-Protected";
        } else if (this.policy.equals("3")) {
            policyReadable = "Permission-Required";
        }
        return "Chatroom(" +
                "" + name + "): " +
                "Owner=" + owner.getUsername() +
                ", policy='" + policyReadable + '\'';
    }

    //owner and name is enough for a chatroom to be unique
    //name is lower case in case user inputs the same name differently
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chatroom)) return false;
        Chatroom chatroom = (Chatroom) o;
        return getOwner().equals(chatroom.getOwner()) &&
                getName().toLowerCase().equals(chatroom.getName().toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwner(), getName());
    }
}
