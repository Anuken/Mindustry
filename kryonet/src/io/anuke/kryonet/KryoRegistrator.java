package io.anuke.kryonet;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;
import io.anuke.ucore.util.ColorCodes;

import java.io.PrintWriter;
import java.io.StringWriter;

import static io.anuke.mindustry.Vars.headless;

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
}
