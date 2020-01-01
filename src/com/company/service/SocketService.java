package com.company.service;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class SocketService {

    //mainly used by the server to answer to questions:
    //converts an object to a byte array, allowing a socket to send it to a receiver
    //requires an Object, this could be a String
    //returns a byte array
    public static byte[] convertObjectToByteArray(Object o) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeObject(o);
        oo.close();
        byte[] serializedObject = bStream.toByteArray();
        return serializedObject;
    }

    //similar to the above method
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


    //this allows the server to send a quick message to a receiver, basically used to group
    //common lines found all over the code together
    //requires a message, the message could be something like "true"
    //returns a ByteBuffer that includes the original message
    public static ByteBuffer newQuickConfirmationPacket(String message) {
        byte[] messageByteArray = message.getBytes();
        ByteBuffer buf = ByteBuffer.wrap(messageByteArray);
        return buf;
    }

    //------------------------------------------OLD METHODS------------------------------------------------//


    //used in the old files, no longer needed in the newer version
    public static DatagramPacket quickConfirmationPacket(String message, InetAddress addressTo, int port) {
        byte[] messageByteArray = message.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(messageByteArray, messageByteArray.length,
                addressTo, port);
        return datagramPacket;
    }

    public static Serializable[] convertByteArrayToObject(byte[] array) throws IOException, ClassNotFoundException {
        ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(array));
        Serializable[] indexedObject = new Serializable[2];
        indexedObject[0] = iStream.readInt();
        indexedObject[1] = (Serializable) iStream.readObject();
        iStream.close();
        return indexedObject;
    }
}
