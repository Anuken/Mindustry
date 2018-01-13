package io.anuke.kryonet;

import com.esotericsoftware.kryo.Kryo;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Address;
import io.anuke.mindustry.net.Net;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class KryoRegistrator {

    public static void register(Kryo kryo){
        //TODO register stuff?
    }

    public static ByteBuffer writeServerData(){
        ByteBuffer buffer = ByteBuffer.allocate(1 + Vars.player.name.length() + 4);
        buffer.put((byte)Vars.player.name.length());
        buffer.put(Vars.player.name.getBytes());
        buffer.putInt(Net.getConnections().size + 1);
        return buffer;
    }

    public static Address readServerData(InetAddress ia, ByteBuffer buffer){
        //old version address.
        if(buffer.capacity() == 4) return null;

        byte length = buffer.get();
        byte[] sname = new byte[length];
        buffer.get(sname);
        int players = buffer.getInt();

        return new Address(new String(sname), ia.getHostAddress(), players);
    }
}
