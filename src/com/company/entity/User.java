package com.company.entity;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class User implements Serializable {

    private String alias;
    private String role;
    private InetAddress details;

    public User(String alias, InetAddress details) {
        this.alias = alias;
        this.details = details;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return getAlias().equals(user.getAlias()) &&
                getDetails().equals(user.getDetails());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAlias(), getDetails());
    }
}
