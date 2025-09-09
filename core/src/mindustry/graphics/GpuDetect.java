package mindustry.graphics;

import arc.struct.*;
import arc.util.*;

import java.util.*;

/** GPU detection for Windows only. All fields will be false or empty on other platforms, even if #init() is called. */
public class GpuDetect{
    public static String rawGpuString = "";
    public static Seq<String> gpus = new Seq<>();
    public static boolean isIntel, isNvidia, isAMD;

    public static void init(){
        if(OS.isWindows){
            try{
                rawGpuString = OS.exec("wmic", "path", "win32_VideoController", "get", "name");
                gpus = Seq.with(rawGpuString.split("\n")).map(s -> s.trim()).removeAll(s -> s.isEmpty() || s.equalsIgnoreCase("name"));

                isIntel = rawGpuString.toLowerCase(Locale.ROOT).contains("intel");
                isNvidia = rawGpuString.toLowerCase(Locale.ROOT).contains("nvidia");
                isAMD = rawGpuString.toLowerCase(Locale.ROOT).contains("amd") || rawGpuString.toLowerCase(Locale.ROOT).contains("radeon");
            }catch(Exception e){
                Log.err(e);
            }
        }
    }
}
