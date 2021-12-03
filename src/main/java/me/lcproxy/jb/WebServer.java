package me.lcproxy.jb;

import io.netty.buffer.Unpooled;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.Getter;
import lombok.SneakyThrows;
import me.lcproxy.jb.mongo.MongoManager;
import me.lcproxy.jb.player.Player;
import me.lcproxy.jb.player.PlayerManager;
import me.lcproxy.jb.server.ByteBufWrapper;
import me.lcproxy.jb.server.ServerHandler;
import me.lcproxy.jb.server.packets.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class WebServer extends WebSocketServer {

    @Getter
    private static WebServer instance;

    @Getter
    private final ServerHandler serverHandler;

    @Getter
    private final PlayerManager playerManager;

    @Getter
    private final MongoManager mongoManager;

    @Getter
    private final int serverId;

    @SneakyThrows
    public WebServer() {
        super(new InetSocketAddress("0.0.0.0", 19486));
        instance = this;
        this.serverHandler = new ServerHandler();
        this.playerManager = new PlayerManager();
        this.mongoManager = new MongoManager();
        this.serverId = ThreadLocalRandom.current().nextInt(1, 999999);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        new Thread(() -> {
            try {
                System.out.println("Connection from " + clientHandshake.getFieldValue("username"));

                serverHandler.sendPacket(webSocket, new WSSendChatMessage("§b[LCProxy] §aHang on, we're loading your data."));

                UUID funnyUUID = UUID.fromString(clientHandshake.getFieldValue("playerid"));

                if (PlayerManager.getPlayerMap().containsKey(funnyUUID)) {
                    playerManager.removePlayer(funnyUUID, false);
                }

                webSocket.setAttachment(funnyUUID);

                Player player = null;

                try {
                    player = playerManager.getOrCreatePlayer(webSocket, clientHandshake.getFieldValue("username"));
                    player.sendAllPackets();
                } catch (Exception e) {
                    // ignore
                }

                serverHandler.sendPacket(webSocket, new WSSendChatMessage("§bThanks for using LCProxy!\n§bYour cosmetics have been §aactivated§b."));

                System.out.println("Sent packets to " + player.getUsername());

                for (Player online : PlayerManager.getPlayerMap().values()) {
                    serverHandler.sendPacket(online.getConn(), new WSPacketCosmeticGive(player.getPlayerId(), player.getRankorDefault().getColor()));
                }

                WebServer.getInstance().getServerHandler().sendPacket(webSocket, new WSPacketCosmeticGive());
                WebServer.getInstance().getServerHandler().sendPacket(webSocket, new WSPacketCosmeticGive());
                WebServer.getInstance().getServerHandler().sendPacket(webSocket, new WSPacketCosmeticGive());
                WebServer.getInstance().getServerHandler().sendPacket(webSocket, new WSPacketCosmeticGive());

            } catch (Exception e) {
                System.out.println("Error on open socket. Username: " + clientHandshake.getFieldValue("username"));
                e.printStackTrace();
            }
        }).start();
    }

    public void updateTags() {
        /*for (Player user : PlayerManager.getPlayerMap().values()) {
            for (Player online : PlayerManager.getPlayerMap().values()) {
                WebServer.getInstance().getServerHandler().sendPacket(online.getConn(), new WSPacketCosmeticGive(user.getPlayerId(), user.getRankorDefault().getColor()));
            }
        }*/
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        new Thread(() -> {
            if (webSocket.getAttachment() != null) {
                Player player = PlayerManager.getPlayerMap().get(webSocket.getAttachment());
                if (player != null) {
                    player.save(false);
                    this.playerManager.removePlayer(webSocket.getAttachment(), true);
                } else {
                    System.out.println("Player is null.");
                }
            }
        }).start();
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {

    }

    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        this.serverHandler.handlePacket(webSocket, new ByteBufWrapper(Unpooled.wrappedBuffer(message.array())));
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

    }
}