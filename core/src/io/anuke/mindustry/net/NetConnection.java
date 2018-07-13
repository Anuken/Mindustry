package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Net.SendMode;

public abstract class NetConnection{
    public final int id;
    public final String address;

    /**
     * The current base snapshot that the client is absolutely confirmed to have recieved.
     * All sent snapshots should be taking the diff from this base snapshot, if it isn't null.
     */
    public byte[] currentBaseSnapshot;
    /**
     * ID of the current base snapshot.
     */
    public int currentBaseID = -1;

    public int lastSentBase = -1;
    public byte[] lastSentSnapshot;
    public byte[] lastSentRawSnapshot;
    public int lastSentSnapshotID = -1;

    /**
     * ID of last recieved client snapshot.
     */
    public int lastRecievedClientSnapshot = -1;
    /**
     * Timestamp of last recieved snapshot.
     */
    public long lastRecievedClientTime;

    public boolean hasConnected = false;

    public NetConnection(int id, String address){
        this.id = id;
        this.address = address;
    }

    public boolean isConnected(){
        return true;
    }

    public abstract void send(Object object, SendMode mode);

    public abstract void close();
}
