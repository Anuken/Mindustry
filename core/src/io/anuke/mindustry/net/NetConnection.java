package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Net.SendMode;

public abstract class NetConnection{
    public final int id;
    public final String address;

    public boolean modclient;
    public boolean mobile;

    public int lastSentSnapshotID = -1;

    /**ID of last recieved client snapshot.*/
    public int lastRecievedClientSnapshot = -1;
    /**Timestamp of last recieved snapshot.*/
    public long lastRecievedClientTime;

    public boolean hasConnected = false;
    public boolean hasBegunConnecting = false;
    public float viewWidth, viewHeight, viewX, viewY;

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
