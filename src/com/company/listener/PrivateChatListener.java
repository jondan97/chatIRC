package com.company.listener;

import com.company.entity.Message;
import com.company.entity.User;
import com.company.service.TCPSocketService;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class PrivateChatListener extends Thread {

    private String ipString;
    private int port;
    private BufferedReader userInput;
    private HashMap<User, String> messages;
    private boolean showKeystrokes;

    public PrivateChatListener(String ipString, int port, BufferedReader userInput) {
        this.ipString = ipString;
        this.port = port;
        this.userInput = userInput;
        messages = new HashMap<>();
        showKeystrokes = false;
    }

    public void run() {
        try {
            Socket tcpSocket = new Socket(ipString, port);
            TCPSocketService.sendObject("/chattingPort", tcpSocket);
            boolean showSender = true;
            while (true) {
                if (!showKeystrokes) {
                    Message msg = (Message) TCPSocketService.receiveObject(tcpSocket);
                    if (!messages.containsKey(msg.getSender())) {
                        //System.out.println("sender added");
                        messages.put(msg.getSender(), msg.getCharacter().toString());
                    } else if (messages.containsKey(msg.getSender())) {
                        if (msg.getKeyValue() != 13) {
                            //System.out.println("letter added.");
                            messages.replace(msg.getSender(), messages.get(msg.getSender()) + msg.getCharacter());
                        } else if (msg.getKeyValue() == 13) {
                            userInput.ready();
                            //System.out.println("message sent");
                            System.out.println("(" + msg.getSender().getUsername() + "): " + messages.get(msg.getSender()));
                            messages.remove(msg.getSender());
                        }
                    }
                } else if (showKeystrokes) {
                    Message msg = (Message) TCPSocketService.receiveObject(tcpSocket);
                    if (showSender) {
                        System.out.print("(" + msg.getSender().getUsername() + "): ");
                        showSender = false;
                    }
                    if (msg.getKeyValue() == 13) {
                        System.out.println();
                        showSender = true;
                    } else {
                        System.out.print(msg.getCharacter());
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            try {
                Socket tcpSocket = new Socket(ipString, port);
                System.out.println("The server has been restarted.");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isShowKeystrokes() {
        return showKeystrokes;
    }

    public void setShowKeystrokes(boolean showKeystrokes) {
        this.showKeystrokes = showKeystrokes;
    }
}