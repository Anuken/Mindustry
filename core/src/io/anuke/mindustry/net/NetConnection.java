package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Net.SendMode;

public abstract class NetConnection {
    public final int id;
    public final String address;

    /**ID of last snapshot this connection is guaranteed to have recieved.*/
    public int lastSnapshotID;
    /**Byte array of last sent snapshot data that is confirmed to be recieved..*/
    public byte[] lastSnapshot;
    /**Byte array of last sent snapshot.*/
    public byte[] lastSentSnapshot;

    public NetConnection(int id, String address){
        this.id = id;
        this.address = address;
    }

    public abstract void send(Object object, SendMode mode);
    public abstract void close();
}
