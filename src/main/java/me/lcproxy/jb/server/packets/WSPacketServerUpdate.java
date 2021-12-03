package me.lcproxy.jb.server.packets;

import me.lcproxy.jb.WebServer;
import me.lcproxy.jb.player.Player;
import me.lcproxy.jb.player.PlayerManager;
import me.lcproxy.jb.server.ByteBufWrapper;
import me.lcproxy.jb.server.ServerHandler;
import me.lcproxy.jb.server.WSPacket;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WSPacketServerUpdate extends WSPacket {
    private String playerId;
    private String serverAddress;

    public WSPacketServerUpdate() {}

    public WSPacketServerUpdate(String playerId, String serverAddress) {
        this.playerId = playerId;
        this.serverAddress = serverAddress;
    }

    @Override
    public void write(WebSocket conn, ByteBufWrapper out) throws IOException {
        out.writeString(this.playerId);
        out.writeString(this.serverAddress);
    }

    @Override
    public void read(WebSocket conn, ByteBufWrapper in) throws IOException {
        this.playerId = in.readString(52);
        this.serverAddress = in.readString(100);
    }

    @Override
    public void process(WebSocket conn, ServerHandler handler) throws IOException {
        Player player = PlayerManager.getPlayerMap().get(conn.getAttachment());
        handler.sendPacket(conn, new WSPacketCosmeticGive());
        if (this.serverAddress.equalsIgnoreCase(player.getServer())) return;

        if (!this.serverAddress.equalsIgnoreCase("")) {
            player.setServer(this.serverAddress);

            WebServer.getInstance().getPlayerManager().getPlayerMap().forEach((uuid, player1) -> {
                try {
                    if(player1.getServer() != null && player1.getServer().toLowerCase().contains(new URI(player.getServer()).getHost().toLowerCase())) {
                        handler.sendPacket(conn, new WSPacketCosmeticGive(player.getPlayerId(), true));
                    }
                } catch (URISyntaxException e) {
                    // fold
                }
            });

            handler.sendPacket(conn, new WSSendChatMessage("§bThanks for using LCProxy!\n§bYour cosmetics have been §aactivated§b."));
        } else {
            player.setServer("");
        }
    }
}