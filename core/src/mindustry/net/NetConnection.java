package mindustry.net;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.net.Packets.*;

import java.io.*;

import static mindustry.Vars.*;

public abstract class NetConnection{
    public final String address;
    public String uuid = "AAAAAAAA", usid = uuid;
    public boolean mobile, modclient;
    public @Nullable Player player;
    public boolean kicked = false;
    public long syncTime;

    /** When this connection was established. */
    public long connectTime = Time.millis();
    /** ID of last received client snapshot. */
    public int lastReceivedClientSnapshot = -1;
    /** Count of snapshots sent from server. */
    public int snapshotsSent;
    /** Timestamp of last received snapshot. */
    public long lastReceivedClientTime;
    /** Build requests that have been recently rejected. This is cleared every snapshot. */
    public Seq<BuildPlan> rejectedRequests = new Seq<>();
    /** Handles chat spam rate limits. */
    public Ratekeeper chatRate = new Ratekeeper();
    /** Handles packet spam rate limits. */
    public Ratekeeper packetRate = new Ratekeeper();

    public boolean hasConnected, hasBegunConnecting, hasDisconnected;
    public float viewWidth, viewHeight, viewX, viewY;

    public NetConnection(String address){
        this.address = address;
    }

    /** Kick with a special, localized reason. Use this if possible. */
    public void kick(KickReason reason){
        kick(reason, (reason == KickReason.kick || reason == KickReason.banned || reason == KickReason.vote) ? 30 * 1000 : 0);
    }

    /** Kick with a special, localized reason. Use this if possible. */
    public void kick(KickReason reason, long kickDuration){
        kick(null, reason, kickDuration);
    }

    /** Kick with an arbitrary reason. */
    public void kick(String reason){
        kick(reason, null, 30 * 1000);
    }

    /** Kick with an arbitrary reason. */
    public void kick(String reason, long duration){
        kick(reason, null, duration);
    }

    /** Kick with an arbitrary reason, and a kick duration in milliseconds. */
    private void kick(String reason, @Nullable KickReason kickType, long kickDuration){
        if(kicked) return;

        Log.info("Kicking connection @ / @; Reason: @", address, uuid, reason == null ? kickType.name() : reason.replace("\n", " "));

        if(kickDuration > 0){
            netServer.admins.handleKicked(uuid, address, kickDuration);
        }

        if(reason == null){
            Call.kick(this, kickType);
        }else{
            Call.kick(this, reason);
        }

        if(uuid.startsWith("steam:")){
            //run with a 2-frame delay so there is time to send the kick packet, steam handles this weirdly
            Core.app.post(() -> Core.app.post(this::close));
        }else{
            close();
        }

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
            begin.type = Net.getPacketId(stream);
            send(begin, true);
            cid = begin.id;

            while(stream.stream.available() > 0){
                byte[] bytes = new byte[Math.min(maxTcpSize, stream.stream.available())];
                stream.stream.read(bytes);

                StreamChunk chunk = new StreamChunk();
                chunk.id = cid;
                chunk.data = bytes;
                send(chunk, true);
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public abstract void send(Object object, boolean reliable);

    public abstract void close();
}
