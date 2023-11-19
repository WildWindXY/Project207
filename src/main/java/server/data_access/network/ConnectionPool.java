package server.data_access.network;

import common.packet.Packet;
import utils.TextUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

class ConnectionPool {
    private final LinkedList<Connection> connections = new LinkedList<>();
    private final ServerSocket serverSocket;
    private final NetworkManager networkManager;
    private final Timer timer;


    ConnectionPool(NetworkManager networkManager, int port) throws IOException {
        this.networkManager = networkManager;
        serverSocket = new ServerSocket(port);
        new Thread(this::handleConnections).start();
        timer = new Timer("Timer clear dead connections");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!serverSocket.isClosed()) {
                    connections.removeIf(connection -> connection.dead);
                } else {
                    timer.cancel();
                }
            }
        }, 0, 1000);// execute every second
    }

    private void handleConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                networkManager.addMessageToTerminal("Some client connected");
                Connection connection = new Connection(socket);
                connections.add(connection);
            } catch (Exception ignored) {//TODO: Log it later

            }
        }
    }

    /**
     * Closes the resources associated with the server.
     * <p>
     * This method is used to close the server's resources, including the server socket
     * and all active connections. It is typically called to gracefully terminate the
     * server. Any IOExceptions that occur during the closing process are caught and
     * ignored for later handling.
     */
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            networkManager.addMessageToTerminal(TextUtils.error(e.getMessage()));
        }
        timer.cancel();
        for (Connection connection : connections) {
            connection.destroy();
        }
    }

    /**
     * Sends a packet to all connected clients.
     * <p>
     * This method sends the specified packet to all connected clients by adding it to
     * each client's send queue. Additionally, it logs the action in the network manager's
     * terminal output.
     *
     * @param packet The packet to be sent to all connected clients.
     */
    public void sendAll(Packet packet) {
        for (Connection connection : connections) {
            connection.toSend.add(packet);
            networkManager.addMessageToTerminal("Send packet to client: " + packet);
        }
    }

    //TODO: public void sendTo(Packet packet, User user){}

    private class Connection {
        private boolean dead = false;
        private final Socket socket;
        private final ExecutorService executorService;
        private final ObjectInputStream in;
        private final ObjectOutputStream out;
        private final LinkedBlockingQueue<Packet> toSend = new LinkedBlockingQueue<>();

        Connection(Socket socket) throws IOException {
            executorService = Executors.newFixedThreadPool(2, r -> new Thread(r, "TCP connection thread: " + socket));
            this.socket = socket;
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            executorService.submit(() -> {
                try {
                    while (!dead) {
                        Object object = in.readObject();
                        if (object instanceof Packet) {
                            packetHandler((Packet) object);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    networkManager.addMessageToTerminal(TextUtils.error(e.getMessage()));
                    destroy();
                }
            });
            executorService.submit(() -> {
                try {
                    while (!dead) {
                        Object obj = toSend.take();
                        out.writeObject(obj);
                        out.flush();
                    }
                } catch (IOException | InterruptedException e) {
                    networkManager.addMessageToTerminal(TextUtils.error(e.getMessage()));
                    destroy();
                }
            });
        }

        private void packetHandler(Packet packet) {
            networkManager.packetHandler(packet);
        }

        private void destroy() {
            dead = true;
            executorService.shutdownNow();
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    networkManager.addMessageToTerminal(TextUtils.error(e.getMessage()));
                }
            }
            try {
                in.close();
            } catch (IOException e) {
                networkManager.addMessageToTerminal(TextUtils.error(e.getMessage()));
            }
            try {
                out.close();
            } catch (IOException e) {
                networkManager.addMessageToTerminal(TextUtils.error(e.getMessage()));
            }
        }
    }
}

