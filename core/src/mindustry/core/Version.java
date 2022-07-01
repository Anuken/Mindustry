package mindustry.core;

import arc.*;
import arc.Files.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

public class Version{
    /** Build type. 'official' for official releases; 'custom' or 'bleeding edge' are also used. */
    public static String type = "unknown";
    /** Build modifier, e.g. 'alpha' or 'release' */
    public static String modifier = "unknown";
    /** Number specifying the major version, e.g. '4' */
    public static int number;
    /** Build number, e.g. '43'. set to '-1' for custom builds. */
    public static int build = 0;
    /** Revision number. Used for hotfixes. Does not affect server compatibility. */
    public static int revision = 0;
    /** Whether version loading is enabled. */
    public static boolean enabled = true;

    public static void init(){
        if(!enabled) return;

        Fi file = OS.isAndroid || OS.isIos ? Core.files.internal("version.properties") : new Fi("version.properties", FileType.internal);

        ObjectMap<String, String> map = new ObjectMap<>();
        PropertiesUtils.load(map, file.reader());

        type = map.get("type");
        number = Integer.parseInt(map.get("number", "4"));
        modifier = map.get("modifier");
        if(map.get("build").contains(".")){
            String[] split = map.get("build").split("\\.");
            try{
                build = Integer.parseInt(split[0]);
                revision = Integer.parseInt(split[1]);
            }catch(Throwable e){
                e.printStackTrace();
                build = -1;
            }
        }else{
            build = Strings.canParseInt(map.get("build")) ? Integer.parseInt(map.get("build")) : -1;
        }
    }

    /** @return whether the current game version is greater than the specified version string, e.g. "120.1"*/
    public static boolean isAtLeast(String str){
        return isAtLeast(build, revision, str);
    }

    /** @return whether the version numbers are greater than the specified version string, e.g. "120.1"*/
    public static boolean isAtLeast(int build, int revision, String str){
        if(build <= 0 || str == null || str.isEmpty()) return true;

        int dot = str.indexOf('.');
        if(dot != -1){
            int major = Strings.parseInt(str.substring(0, dot), 0), minor = Strings.parseInt(str.substring(dot + 1), 0);
            return build > major || (build == major && revision >= minor);
        }else{
            return build >= Strings.parseInt(str, 0);
        }
    }

    public static String buildString(){
        return build < 0 ? "custom" : build + (revision == 0 ? "" : "." + revision);
    }

    /** get menu version without colors */
    public static String combined(){
        if(build == -1){
            return "custom build";
        }
        return (type.equals("official") ? modifier : type) + " build " + build + (revision == 0 ? "" : "." + revision);
    }
}
