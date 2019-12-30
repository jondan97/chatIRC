package com.company.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class SocketService {

    //mainly used by the server to answer to questions
    public static byte[] convertObjectToByteArray(Object o) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeObject(o);
        oo.close();
        byte[] serializedObject = bStream.toByteArray();
        return serializedObject;
    }

    //this allows the "reader" to distinguish between objects, say for example you want to distinguish in a packet
    //between deleting and creating a chatroom, how do you know the purpose of the packet?
    // you add a number in front of the packet
    //so if the reader reads "1', it's 'create' and if the reader reads "2" it's 'delete'
    public static byte[] convertDistinguishableObjectToByteArray(int number, Object o) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeInt(number);
        oo.writeObject(o);
        oo.close();
        byte[] serializedDistinguishableObject = bStream.toByteArray();
        return serializedDistinguishableObject;
    }

    //    public static Object convertByteArraytoObject(){
//
//    }
    public static DatagramPacket quickConfirmationPacket(String message, InetAddress addressTo, int port) {
        byte[] messageByteArray = message.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(messageByteArray, messageByteArray.length,
                addressTo, port);
        return datagramPacket;
    }
}
