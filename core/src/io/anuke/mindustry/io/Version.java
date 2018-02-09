package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

public class Version {
    public static final String name;
    public static final String type;
    public static final String code;
    public static final int build;
    public static final String buildName;

    static{
        try {
            FileHandle file = Gdx.files.internal("version.properties");

            ObjectMap<String, String> map = new ObjectMap<>();
            PropertiesUtils.load(map, file.reader());

            name = map.get("name");
            type = map.get("version");
            code = map.get("code");
            build = Strings.canParseInt(map.get("build")) ? Integer.parseInt(map.get("build")) : -1;
            buildName = build == -1 ? map.get("build") : "build " + build;

        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
