package com.company.random.testing.udptcpInCaseTheseAreNeeded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        System.out.println("end");

        // TCP
        new Thread(new Runnable() {
            @Override
            public void run() {
                ExecutorService executor = null;
                try (ServerSocket server = new ServerSocket(1234)) {
                    executor = Executors.newFixedThreadPool(5);
                    System.out.println("Listening on TCP port 1234, Say hi!");
                    while (true) {
                        final Socket socket = server.accept();
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                String inputLine = "";
                                System.err.println(
                                        socket.toString() + " ~> connected");
                                try (PrintWriter out = new PrintWriter(
                                        socket.getOutputStream(), true);
                                     BufferedReader in = new BufferedReader(
                                             new InputStreamReader(socket
                                                     .getInputStream()))) {
                                    while (!inputLine.equals("!quit")
                                            && (inputLine = in
                                            .readLine()) != null) {
                                        System.out.println(socket.toString()
                                                + ": " + inputLine);
                                        // Echo server...
                                        out.println(inputLine);
                                    }
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                } finally {
                                    try {
                                        System.err.println(socket.toString()
                                                + " ~> closing");
                                        socket.close();
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                } catch (IOException ioe) {
                    System.err.println("Cannot open the port on TCP");
                    ioe.printStackTrace();
                } finally {
                    System.out.println("Closing TCP server");
                    if (executor != null) {
                        executor.shutdown();
                    }
                }
            }
        }).start();
        System.out.println("end");

        // UDP
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (DatagramSocket socket = new DatagramSocket(1234)) {
                    byte[] buf = new byte[socket.getReceiveBufferSize()];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);

                    System.out.println("Listening on UDP port 1234, Say hi!");
                    while (true) {
                        socket.receive(packet);
                        System.out.println(packet.getSocketAddress().toString()
                                + ": " + new String(buf, "UTF-8"));
                        // Echo server
                        socket.send(packet);
                    }
                } catch (IOException ioe) {
                    System.err.println("Cannot open the port on UDP");
                    ioe.printStackTrace();
                } finally {
                    System.out.println("Closing UDP server");
                }
            }
        }).start();
        System.out.println("end");
    }
}
