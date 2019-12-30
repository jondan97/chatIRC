package com.company.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Chatroom implements Serializable {
    User owner;
    String policy;
    String name;
    String password;
    ArrayList<User> users;
    int kickTime;
    int deletionTime;

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
                "Owner=" + owner.getAlias() +
                ", policy='" + policyReadable + '\'';
    }

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
