package com.company.random.testing.multicasting;

import com.company.thread.MulticastPublisher;

import java.io.IOException;

public class ServerCast {
    public static void main(String[] args) throws IOException {
        MulticastPublisher m = new MulticastPublisher();
        //m.multicast("xixi");
        System.out.println("packet sent");

    }
}
