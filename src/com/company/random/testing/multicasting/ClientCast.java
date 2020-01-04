package com.company.random.testing.multicasting;

import com.company.thread.MulticastReceiver;

public class ClientCast {
    public static void main(String[] args) {
        MulticastReceiver r = new MulticastReceiver();
        r.run();

    }
}
