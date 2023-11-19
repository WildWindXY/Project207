package server.use_case.login;

import common.packet.PacketClientLogin;
import common.packet.PacketServerLoginResponse;
import server.data_access.network.ConnectionInfo;
import server.entity.PacketIn;
import server.entity.ServerUser;
import server.use_case.ServerThreadPool;
import utils.Tuple;

public class ServerLoginInteractor implements ServerLoginInputBoundary {
    private final ServerLoginOutputBoundary serverLoginPresenter;
    private final ServerLoginDataAccessInterface serverLoginDataAccessInterface;

    public ServerLoginInteractor(ServerLoginDataAccessInterface serverLoginDataAccessInterface, ServerLoginOutputBoundary serverLoginPresenter) {
        this.serverLoginPresenter = serverLoginPresenter;
        this.serverLoginDataAccessInterface = serverLoginDataAccessInterface;
        ServerThreadPool.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    handlePacket(serverLoginDataAccessInterface.getPacketClientLogin());
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                serverLoginPresenter.addMessage("ServerLoginInteractor ended");
            }
        }, "ServerLoginInteractor");
    }

    private void handlePacket(PacketIn<PacketClientLogin> packetIn) {
        String username = packetIn.getPacket().getUsername();
        String password = packetIn.getPacket().getHashedPassword();
        ConnectionInfo info = packetIn.getConnectionInfo();
        if (info.getStatus() == ConnectionInfo.Status.LOGGED) {
            serverLoginPresenter.addMessage("Login Failed: client " + info.getUser().getUsername() + "already logged in");
            serverLoginDataAccessInterface.sendTo(new PacketServerLoginResponse(-1, PacketServerLoginResponse.Status.ALREADY_LOGGED_IN), info);
        } else if (username == null || password == null) {
            serverLoginPresenter.addMessage("Login Failed: packet contains null attribute.");
            serverLoginDataAccessInterface.sendTo(new PacketServerLoginResponse(-1, PacketServerLoginResponse.Status.NULL_ATTRIBUTE), info);
        } else {
            Tuple<Integer, ServerUser> tuple = serverLoginDataAccessInterface.checkPassword(username, password);
            if (tuple.first() == -1) {
                serverLoginPresenter.addMessage("Login Failed: incorrect password " + password);
                serverLoginDataAccessInterface.sendTo(new PacketServerLoginResponse(-1, PacketServerLoginResponse.Status.WRONG_PASSWORD), info);
            } else if (tuple.first() == -2) {
                serverLoginPresenter.addMessage("Login Failed: username " + username + " not found.");
                serverLoginDataAccessInterface.sendTo(new PacketServerLoginResponse(-1, PacketServerLoginResponse.Status.NO_SUCH_USERNAME), info);
            } else {
                serverLoginPresenter.addMessage("Signup Success: [username: " + username + ", password: " + password + ", id: " + tuple.second().getUserId());
                info.login(tuple.second());
                serverLoginDataAccessInterface.sendTo(new PacketServerLoginResponse(tuple.second().getUserId(), PacketServerLoginResponse.Status.SUCCESS), info);
            }
        }
    }
}
