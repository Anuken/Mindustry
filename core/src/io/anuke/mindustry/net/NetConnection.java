package io.anuke.mindustry.net;

import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.Administration.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;

import java.io.*;

import static io.anuke.mindustry.Vars.netServer;

public abstract class NetConnection{
    public final String address;
    public boolean mobile, modclient;
    public @Nullable
    Player player;

    /** ID of last recieved client snapshot. */
    public int lastRecievedClientSnapshot = -1;
    /** Timestamp of last recieved snapshot. */
    public long lastRecievedClientTime;

    public boolean hasConnected, hasBegunConnecting, hasDisconnected;
    public float viewWidth, viewHeight, viewX, viewY;

    public NetConnection(String address){
        this.address = address;
    }

    /** Kick with a special, localized reason. Use this if possible. */
    public void kick(KickReason reason){
        Log.info("Kicking connection {0}; Reason: {1}", address, reason.name());

        if(player != null && (reason == KickReason.kick || reason == KickReason.banned || reason == KickReason.vote) && player.uuid != null){
            PlayerInfo info = netServer.admins.getInfo(player.uuid);
            info.timesKicked++;
            info.lastKicked = Math.max(Time.millis(), info.lastKicked);
        }

        Call.onKick(this, reason);

        Time.runTask(2f, this::close);

        netServer.admins.save();
    }

    /** Kick with an arbitrary reason. */
    public void kick(String reason){
        Log.info("Kicking connection {0}; Reason: {1}", address, reason.replace("\n", " "));

        if(player != null  && player.uuid != null){
            PlayerInfo info = netServer.admins.getInfo(player.uuid);
            info.timesKicked++;
            info.lastKicked = Math.max(Time.millis(), info.lastKicked);
        }

        Call.onKick(this, reason);

        Time.runTask(2f, this::close);

        netServer.admins.save();
    }

    public boolean isConnected(){
        return true;
    }

    public void sendStream(Streamable stream){
        try{
            int cid;
            StreamBegin begin = new StreamBegin();
            begin.total = stream.stream.available();
            begin.type = Registrator.getID(stream.getClass());
            send(begin, SendMode.tcp);
            cid = begin.id;

            while(stream.stream.available() > 0){
                byte[] bytes = new byte[Math.min(512, stream.stream.available())];
                stream.stream.read(bytes);

                StreamChunk chunk = new StreamChunk();
                chunk.id = cid;
                chunk.data = bytes;
                send(chunk, SendMode.tcp);
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public abstract void send(Object object, SendMode mode);

    public abstract void close();
}
