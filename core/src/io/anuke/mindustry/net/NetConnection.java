package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Net.SendMode;

public abstract class NetConnection {
    public final int id;
    public final String address;

    /**ID of last snapshot this connection is guaranteed to have recieved.*/
    public int lastSnapshotID = -1;
    /**Byte array of last sent snapshot data that is confirmed to be recieved.*/
    public byte[] lastSnapshot;

    /**ID of last sent snapshot.*/
    public int lastSentSnapshotID = -1;
    /**Byte array of last sent snapshot.*/
    public byte[] lastSentSnapshot;

    /**ID of last recieved client snapshot.*/
    public int lastRecievedSnapshot = -1;
    /**Timestamp of last recieved snapshot.*/
    public long lastRecievedTime;

    public NetConnection(int id, String address){
        this.id = id;
        this.address = address;
    }

    public abstract void send(Object object, SendMode mode);
    public abstract void close();
}
