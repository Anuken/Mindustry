package io.anuke.kryonet;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;
import io.anuke.ucore.util.ColorCodes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ScheduledExecutorService;

import static io.anuke.mindustry.Vars.headless;

/** Utilities and configs for kryo module. */
public class KryoCore{
    public static boolean fakeLag = false;
    public static final int fakeLagMax = 500;
    public static final int fakeLagMin = 0;
    public static final float fakeLagDrop = 0.1f;
    public static final float fakeLagDuplicate = 0.1f;

    public static boolean lastUDP;

    private static ScheduledExecutorService threadPool;

    public static void init(){
        Log.set(fakeLag ? Log.LEVEL_DEBUG : Log.LEVEL_WARN);

        Log.setLogger(new Logger(){
            public void log(int level, String category, String message, Throwable ex){
                if(fakeLag){
                    if(message.contains("UDP")){
                        lastUDP = true;
                    }else if(message.contains("TCP")){
                        lastUDP = false;
                    }
                    return;
                }

                StringBuilder builder = new StringBuilder(256);

                if(headless)
                    builder.append(ColorCodes.BLUE);

                builder.append("Net Error: ");

                builder.append(message);

                if(ex != null){
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

    private static int calculateLag(){
        return fakeLagMin + (int) (Math.random() * (fakeLagMax - fakeLagMin));
    }
}
