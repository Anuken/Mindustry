package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Net.SendMode;

public abstract class NetConnection{
    private static int lastID;

    public final int id;
    public final String address;

    public boolean modclient;
    public boolean mobile;

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

    public boolean isConnected(){
        return true;
    }

    public abstract void send(Object object, SendMode mode);

    public abstract void close();
}
