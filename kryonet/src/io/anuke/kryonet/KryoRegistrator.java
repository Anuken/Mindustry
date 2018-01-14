package io.anuke.kryonet;

import com.esotericsoftware.kryo.Kryo;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class KryoRegistrator {
    public static boolean fakeLag = true;
    public static final int fakeLagAmount = 500;

    public static void register(Kryo kryo){
        //TODO register stuff?
        //Log.set(Log.LEVEL_DEBUG);
    }

    public static ByteBuffer writeServerData(){
        ByteBuffer buffer = ByteBuffer.allocate(1 + Vars.player.name.length() + 4);
        buffer.put((byte)Vars.player.name.length());
        buffer.put(Vars.player.name.getBytes());
        buffer.putInt(Net.getConnections().size + 1);
        return buffer;
    }

    public static Host readServerData(InetAddress ia, ByteBuffer buffer){
        //old version address.
        if(buffer.capacity() == 4) return null;

        byte length = buffer.get();
        byte[] sname = new byte[length];
        buffer.get(sname);
        int players = buffer.getInt();

        return new Host(new String(sname), ia.getHostAddress(), players);
    }
}
