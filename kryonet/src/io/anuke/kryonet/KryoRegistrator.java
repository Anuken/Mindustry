package io.anuke.kryonet;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Host;
import io.anuke.ucore.util.ColorCodes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class KryoRegistrator {
    public static boolean fakeLag = false;
    public static final int fakeLagMax = 1000;
    public static final int fakeLagMin = 0;

    static{
        Log.set(Log.LEVEL_WARN);

        Log.setLogger(new Logger(){
            public void log (int level, String category, String message, Throwable ex) {
                StringBuilder builder = new StringBuilder(256);

                if(headless)
                    builder.append(ColorCodes.BLUE);

                builder.append("Net Error: ");

                builder.append(message);

                if (ex != null) {
                    StringWriter writer = new StringWriter(256);
                    ex.printStackTrace(new PrintWriter(writer));
                    builder.append('\n');
                    builder.append(writer.toString().trim());
                }

                if(headless)
                    builder.append(ColorCodes.RESET);

                io.anuke.ucore.util.Log.info("&b" + builder.toString());
            }
        });
    }

    public static ByteBuffer writeServerData(){
        int maxlen = 32;

        String host = (headless ? "Server" : player.name);
        String map = world.getMap().name;

        host = host.substring(0, Math.min(host.length(), maxlen));
        map = map.substring(0, Math.min(map.length(), maxlen));

        ByteBuffer buffer = ByteBuffer.allocate(128);

        buffer.put((byte)host.getBytes().length);
        buffer.put(host.getBytes());

        buffer.put((byte)map.getBytes().length);
        buffer.put(map.getBytes());

        buffer.putInt(playerGroup.size());
        buffer.putInt(state.wave);
        buffer.putInt(Version.build);
        return buffer;
    }

    public static Host readServerData(InetAddress ia, ByteBuffer buffer){
        if(buffer.capacity() < 128) return null; //old version address.

        byte hlength = buffer.get();
        byte[] hb = new byte[hlength];
        buffer.get(hb);

        byte mlength = buffer.get();
        byte[] mb = new byte[mlength];
        buffer.get(mb);

        String host = new String(hb);
        String map = new String(mb);

        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();

        return new Host(host, ia.getHostAddress(), map, wave, players, version);
    }
}
