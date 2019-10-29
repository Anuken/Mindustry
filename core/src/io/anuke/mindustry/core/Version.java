package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.Files.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;

import java.io.*;

public class Version{
    /** Build type. 'official' for official releases; 'custom' or 'bleeding edge' are also used. */
    public static String type;
    /** Build modifier, e.g. 'alpha' or 'release' */
    public static String modifier;
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

        try{
            FileHandle file = OS.isAndroid || OS.isIos ? Core.files.internal("version.properties") : new FileHandle("version.properties", FileType.Internal);

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
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
