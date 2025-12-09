package mindustry.graphics;

import arc.files.*;
import arc.util.*;

import java.util.*;

public class IntelGpuCheck{
    private static boolean wasIntel, checkedLastLaunch;

    /** initialize intel version check for the next application launch */
    public static void init(String vendor){
        if(!OS.isWindows) return;

        boolean isIntel = vendor.toLowerCase(Locale.ROOT).contains("intel");
        try{
            Fi file = new Fi(OS.getAppDataDirectoryString("Mindustry")).child("was_intel_gpu");
            if(isIntel){
                file.writeString("1");
            }else if(file.exists()){
                file.delete();
            }
        }catch(Throwable e){
            Log.err(e);
        }
    }

    /** @return whether the last launch used an intel GPU on Windows */
    public static boolean wasIntel(){
        if(!OS.isWindows) return false;
        if(checkedLastLaunch) return wasIntel;
        checkedLastLaunch = true;

        try{
            Fi file = new Fi(OS.getAppDataDirectoryString("Mindustry")).child("was_intel_gpu");
            if(file.exists() && file.readString().equals("1")){
                return wasIntel = true;
            }
        }catch(Throwable e){
            Log.err("Failed to check whether the last launch used an intel GPU.", e);
        }
        return wasIntel = false;
    }
}
