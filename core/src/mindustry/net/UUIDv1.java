package mindustry.net;

import arc.math.*;
import arc.util.*;

import java.net.*;
import java.util.*;

/** Generates RFC 4122 version 1 (time-based) UUIDs, using the machine's MAC address as the node ID when possible. */
public final class UUIDv1{
    private static final long epochOffset = 0x01B21DD213814000L;
    private static final Rand rand = new Rand();

    private static long lastTime = -1;
    private static int clockSeq = -1;

    private UUIDv1(){}

    /** @return a new version 1 UUID as 16 raw bytes, in RFC 4122 field order. */
    public static synchronized byte[] generate(){
        long time = Time.millis() * 10000L + epochOffset;
        if(clockSeq == -1) clockSeq = rand.nextInt() & 0x3fff;
        if(time <= lastTime) clockSeq = (clockSeq + 1) & 0x3fff;
        lastTime = time;

        long timeLow = time & 0xffffffffL;
        long timeMid = (time >> 32) & 0xffffL;
        long timeHi = ((time >> 48) & 0x0fffL) | 0x1000L;
        int clockSeqHi = (clockSeq >> 8) | 0x80;
        int clockSeqLow = clockSeq & 0xff;
        long node = node();

        byte[] out = new byte[16];
        out[0] = (byte)(timeLow >> 24);
        out[1] = (byte)(timeLow >> 16);
        out[2] = (byte)(timeLow >> 8);
        out[3] = (byte)timeLow;
        out[4] = (byte)(timeMid >> 8);
        out[5] = (byte)timeMid;
        out[6] = (byte)(timeHi >> 8);
        out[7] = (byte)timeHi;
        out[8] = (byte)clockSeqHi;
        out[9] = (byte)clockSeqLow;
        out[10] = (byte)(node >> 40);
        out[11] = (byte)(node >> 32);
        out[12] = (byte)(node >> 24);
        out[13] = (byte)(node >> 16);
        out[14] = (byte)(node >> 8);
        out[15] = (byte)node;
        return out;
    }

    /** @return the 48-bit node ID, preferring a real MAC address; falls back to a random multicast-flagged one per RFC 4122. */
    private static long node(){
        try{
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while(ifaces != null && ifaces.hasMoreElements()){
                NetworkInterface iface = ifaces.nextElement();
                byte[] mac = iface.getHardwareAddress();
                if(mac != null && mac.length == 6 && !iface.isLoopback()){
                    long node = 0;
                    for(byte b : mac) node = (node << 8) | (b & 0xffL);
                    return node;
                }
            }
        }catch(Exception ignored){
        }

        long node = rand.nextLong() & 0xffffffffffffL;
        return node | 0x010000000000L;
    }
}
