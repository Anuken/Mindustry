package arc.util;

import arc.struct.*;

public class Bench{
    private static long totalStart;
    private static String lastName;
    private static ObjectMap<String, Long> times = new ObjectMap<>();
    private static long last;

    public static void begin(String name){
        if(lastName != null){
            endi();
        }else{
            totalStart = Time.millis();
        }
        last = Time.millis();
        lastName = name;
    }

    public static void end(){
        endi();
        long total = Time.timeSinceMillis(totalStart);

        times.each((name, time) -> {
            Log.info("[PERF] @: @ms (@%)", name, time, (int)((float)time / total * 100));
        });
        Log.info("[PERF] TOTAL: @ms", total);
    }

    private static void endi(){
        times.put(lastName, Time.timeSinceMillis(last));
        lastName = null;
    }
}
