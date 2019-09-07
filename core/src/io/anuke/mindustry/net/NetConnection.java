package io.anuke.mindustry.net;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.Administration.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.net.Packets.*;

import static io.anuke.mindustry.Vars.netServer;

public abstract class NetConnection{
    private static int lastID;

    public final int id;
    public final String address;

    public boolean modclient;
    public boolean mobile;
    public @Nullable Player player;

    /** ID of last recieved client snapshot. */
    public int lastRecievedClientSnapshot = -1;
    /** Timestamp of last recieved snapshot. */
    public long lastRecievedClientTime;

    public boolean hasConnected = false;
    public boolean hasBegunConnecting = false;
    public float viewWidth, viewHeight, viewX, viewY;

    /** Assigns this connection a unique ID. No two connections will ever have the same ID.*/
    public NetConnection(String address){
        this.id = lastID++;
        this.address = address;
    }

    public void kick(KickReason reason){
        Log.info("Kicking connection #{0} / IP: {1}. Reason: {2}", this.id, address, reason.name());

        if(player != null && (reason == KickReason.kick || reason == KickReason.banned || reason == KickReason.vote) && player.uuid != null){
            PlayerInfo info = netServer.admins.getInfo(player.uuid);
            info.timesKicked++;
            info.lastKicked = Math.max(Time.millis(), info.lastKicked);
        }

        Call.onKick(id, reason);

        Time.runTask(2f, this::close);

        netServer.admins.save();
    }

    public boolean isConnected(){
        return true;
    }

    public abstract void send(Object object, SendMode mode);

    public abstract void close();
}
