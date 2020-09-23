package mindustry.net;

import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.net.Net.*;
import mindustry.net.Packets.*;

import java.io.*;

import static mindustry.Vars.*;

public abstract class NetConnection{
    public final String address;
    public String uuid = "AAAAAAAA", usid = uuid;
    public boolean mobile, modclient;
    public @Nullable Player player;
    public boolean kicked = false;

    /** ID of last received client snapshot. */
    public int lastReceivedClientSnapshot = -1;
    /** Timestamp of last received snapshot. */
    public long lastReceivedClientTime;
    /** Build requests that have been recently rejected. This is cleared every snapshot. */
    public Seq<BuildPlan> rejectedRequests = new Seq<>();

    public boolean hasConnected, hasBegunConnecting, hasDisconnected;
    public float viewWidth, viewHeight, viewX, viewY;

    public NetConnection(String address){
        this.address = address;
    }

    /** Kick with a special, localized reason. Use this if possible. */
    public void kick(KickReason reason){
        if(kicked) return;

        Log.info("Kicking connection @; Reason: @", address, reason.name());

        if((reason == KickReason.kick || reason == KickReason.banned || reason == KickReason.vote)){
            PlayerInfo info = netServer.admins.getInfo(uuid);
            info.timesKicked++;
            info.lastKicked = Math.max(Time.millis() + 30 * 1000, info.lastKicked);
        }

        Call.kick(this, reason);

        Time.runTask(2f, this::close);

        netServer.admins.save();
        kicked = true;
    }

    /** Kick with an arbitrary reason. */
    public void kick(String reason){
        kick(reason, 30 * 1000);
    }

    /** Kick with an arbitrary reason, and a kick duration in milliseconds. */
    public void kick(String reason, int kickDuration){
        if(kicked) return;

        Log.info("Kicking connection @; Reason: @", address, reason.replace("\n", " "));

        PlayerInfo info = netServer.admins.getInfo(uuid);
        info.timesKicked++;
        info.lastKicked = Math.max(Time.millis() + kickDuration, info.lastKicked);

        Call.kick(this, reason);

        Time.runTask(2f, this::close);

        netServer.admins.save();
        kicked = true;
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
