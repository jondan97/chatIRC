package com.company.random.testing.privateChat;

import com.company.entity.Chatroom;
import com.company.entity.User;
import com.company.service.TCPSocketService;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class TCPClient {
    public static void main(String[] argv) throws Exception {
        String input;
        Socket server = new Socket("192.168.1.13", 1);
        Chatroom chatroom = new Chatroom();
        chatroom.setOwner(new User("skap"));
        chatroom.setName("XAAXXAXAXAXAAXXAXAAXAX");
        chatroom.setPolicy("3");
        chatroom.setKickTime(4);
        //chatroom.setDeletionTime(56);
        chatroom.setPassword("xdDD");
        TCPSocketService.sendObject(chatroom, server);
        Chatroom returnMessage = (Chatroom) TCPSocketService.receiveObject(server);


        System.out.println("return Message is= " + returnMessage);
        Scanner scanner = new Scanner(System.in);

        System.out.println("\r\nConnected to Server: " + server.getInetAddress());
        while (true) {
            input = scanner.nextLine();
            PrintWriter out = new PrintWriter(server.getOutputStream(), true);
            out.println(input);
            out.flush();
        }
    }
}
