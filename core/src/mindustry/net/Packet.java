package mindustry.net;

import arc.util.io.*;

public abstract class Packet{
    //these are constants because I don't want to bother making an enum to mirror the annotation enum

    /** Does not get handled unless client is connected. */
    public static final int priorityLow = 0;
    /** Gets put in a queue and processed if not connected. */
    public static final int priorityNormal = 1;
    /** Gets handled immediately, regardless of connection status. */
    public static final int priorityHigh = 2;

    public void read(Reads read){}
    public void write(Writes write){}

    public int getPriority(){
        return priorityNormal;
    }

    public void handleClient(){}
    public void handleServer(NetConnection con){}
}
