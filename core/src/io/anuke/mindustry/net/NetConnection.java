package io.anuke.mindustry.net;

import io.anuke.mindustry.net.Net.SendMode;

public abstract class NetConnection {
    public final int id;
    public final String address;

    public NetConnection(int id, String address){
        this.id = id;
        this.address = address;
    }

    public abstract void send(Object object, SendMode mode);
    public abstract void close();
}
