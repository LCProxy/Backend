package me.lcproxy.jb.server.packets;

import me.lcproxy.jb.WebServer;
import me.lcproxy.jb.player.Player;
import me.lcproxy.jb.server.ByteBufWrapper;
import me.lcproxy.jb.server.ServerHandler;
import me.lcproxy.jb.server.WSPacket;
import me.lcproxy.jb.util.GenFromIndexFile;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class WSPacketCosmeticGive extends WSPacket {

    UUID target;
    int cosmeticId;
    int color = -1;
    boolean update;
    boolean external;
    ArrayList<Integer> externalCosmetics;
    public WSPacketCosmeticGive() {
        this.cosmeticId = -1;
        this.update = false;
        this.external = false;
    }
    public WSPacketCosmeticGive(UUID uuid) {
        this.target = uuid;
        this.update = false;
        this.external = false;
    }
    public WSPacketCosmeticGive(UUID uuid, boolean update) {
        this.target = uuid;
        this.update = update;
        this.external = false;
    }
    public WSPacketCosmeticGive(UUID uuid, boolean update, boolean external, ArrayList<Integer> externalCosmetics) {
        this.target = uuid;
        this.update = update;
        this.external = external;
        this.externalCosmetics = externalCosmetics;
    }
    public WSPacketCosmeticGive(UUID uuid, int Color) {
        this.target = uuid;
        this.update = true;
        this.color = Color;
        this.external = false;
    }

    @Override
    public void write(WebSocket conn, ByteBufWrapper out) throws IOException {
        if (target == null) {
            target = conn.getAttachment();
        }
        out.writeLong(target.getMostSignificantBits());
        out.writeLong(target.getLeastSignificantBits());
        Player player = WebServer.getInstance().getPlayerManager().getPlayerById(target);
        if (!update) {
            out.writeVarInt(GenFromIndexFile.getCosmetics().values().size());
            ArrayList<Integer> dupes = new ArrayList<>();
            int i = 0;
            for (String[] values : GenFromIndexFile.getCosmetics().values()) {
                int id = Integer.parseInt(values[0]);
                String name = values[3];

                boolean state = Boolean.parseBoolean(values[5]);
                if (!dupes.contains(id)) {
                    dupes.add(id);
                } else {
                    //System.out.println("Dupe id: " + id + " Name: " + name);
                }
                out.writeVarInt(id);
                if(external) {
                    out.writeBoolean(externalCosmetics.contains(id));
                } else {
                    out.writeBoolean(player.getEnabledCosmetics().contains(id));
                }
                i++;
            }
            //System.out.println("Added cosmetics to user " + player.getUsername());
            out.writeInt(WebServer.getInstance().getPlayerManager().getPlayerById(target).getRank().getColor());
            out.writeBoolean(true);

        }

        else {
            if(color == -1) {
                out.writeVarInt(external ? externalCosmetics.size() : player.getEnabledCosmetics().size());
                for (int cosmId : external ? externalCosmetics : player.getEnabledCosmetics()) {
                    String[] info = GenFromIndexFile.getCosmetics().get(cosmId);
                    int id = Integer.parseInt(info[0]);
                    String name = info[3];
                    out.writeVarInt(id);
                    out.writeBoolean(true);
                }
                out.writeInt(WebServer.getInstance().getPlayerManager().getPlayerById(target).getRank().getColor());
                out.writeBoolean(true);
            } else {
                out.writeVarInt(external ? externalCosmetics.size() : player.getEnabledCosmetics().size());
                for (int cosmId : external ? externalCosmetics : player.getEnabledCosmetics()) {
                    String[] info = GenFromIndexFile.getCosmetics().get(cosmId);
                    int id = Integer.parseInt(info[0]);
                    String name = info[3];
                    out.writeVarInt(id);
                    out.writeBoolean(true);
                }
                out.writeInt(color);
                out.writeBoolean(true);
            }
        }
    }

    @Override
    public void read(WebSocket conn, ByteBufWrapper in) throws IOException {

    }

    @Override
    public void process(WebSocket conn, ServerHandler handler) throws IOException {

    }
}
