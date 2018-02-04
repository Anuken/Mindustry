package io.anuke.kryonet;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Host;
import io.anuke.ucore.util.ColorCodes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.playerGroup;

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
        String host = headless ? "Server" : Vars.player.name;

        ByteBuffer buffer = ByteBuffer.allocate(1 + host.getBytes().length + 4);
        buffer.put((byte)host.getBytes().length);
        buffer.put(host.getBytes());
        buffer.putInt(playerGroup.size());
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
