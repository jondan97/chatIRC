package com.company;

import java.net.Socket;

public class TCPThread extends Thread {
    public TCPThread(Socket socket) {
    }

    public void run() {
        System.out.println("tcp thread started");
        //to do: move some UDP functionalities here, implement new requirements

    }
}
