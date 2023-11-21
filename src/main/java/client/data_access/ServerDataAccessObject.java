package client.data_access;

import common.packet.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerDataAccessObject {

    private final LinkedBlockingQueue<Packet> receivedPacket = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<PacketServerLoginResponse> loginResponses = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<PacketServerSignupResponse> signupResponses = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<PacketServerMessage> serverMessages = new LinkedBlockingQueue<>();

    private final LinkedBlockingQueue<PacketServerGetFriendListResponse> getFriendListResponses = new LinkedBlockingQueue<>();

    private final LinkedBlockingQueue<PacketServerTextMessageResponse> sendMessageResponses = new LinkedBlockingQueue<>();

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ServerDataAccessObject(String serverAddress, int serverPort) {
        try {
            Socket clientSocket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            receivePacket();
            packetClassification();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receivePacket() {
        Thread receiveThread = new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Packet) {
                        receivedPacket.add((Packet) obj);
                    } else {
                        throw new IOException("Received object is not a packet");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        receiveThread.start();
    }

    private void packetClassification() {
        Thread receiveThread = new Thread(() -> {
            while (true) {
                try {
                    Packet packet = receivedPacket.take();
                    if (packet instanceof PacketDebug) {
                        System.out.println("Received from server (debug message): " + packet);
                    } else if (packet instanceof PacketServerMessage) {
                        serverMessages.add((PacketServerMessage) packet);
                    } else if (packet instanceof PacketServerLoginResponse) {
                        loginResponses.add((PacketServerLoginResponse) packet);
                    } else if (packet instanceof PacketServerTextMessageResponse) {
                        sendMessageResponses.add((PacketServerTextMessageResponse) packet);
                    } else if (packet instanceof PacketServerSignupResponse) {
                        signupResponses.add((PacketServerSignupResponse) packet);
                    } else if (packet instanceof PacketServerGetFriendListResponse) {
                        getFriendListResponses.add((PacketServerGetFriendListResponse) packet);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        receiveThread.start();
    }

    public void sendPacket(Packet msg) {
        System.out.println("Message to send to server: " + msg.toString());
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PacketServerLoginResponse getLoginResponse() {
        try {
            return loginResponses.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public PacketServerGetFriendListResponse getFriendListResponse() {
        try {
            return getFriendListResponses.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public PacketServerTextMessageResponse getSendMessageResponse() {
        try {
            return sendMessageResponses.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public PacketServerMessage getReceiveMessage() {
        try {
            return serverMessages.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public PacketServerSignupResponse getSignupResponse() {
        try {
            return signupResponses.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
