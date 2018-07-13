package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

public class Version{
    public static String name;
    public static String type;
    public static String code;
    public static int build = 0;
    public static String buildName;

    public static void init(){
        try{
            FileHandle file = Gdx.files.internal("version.properties");

            ObjectMap<String, String> map = new ObjectMap<>();
            PropertiesUtils.load(map, file.reader());

            name = map.get("name");
            type = map.get("version");
            code = map.get("code");
            build = Strings.canParseInt(map.get("build")) ? Integer.parseInt(map.get("build")) : -1;
            buildName = build == -1 ? map.get("build") : "build " + build;

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
