package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;

public class MapMeta{
    public final int version;
    public final ObjectMap<String, String> tags;
    public final int width, height;
    public final IntIntMap blockMap;

    public MapMeta(int version, ObjectMap<String, String> tags, int width, int height, IntIntMap blockMap){
        this.version = version;
        this.tags = tags;
        this.width = width;
        this.height = height;
        this.blockMap = blockMap;
    }

    public String author(){
        return tags.get("author", "unknown");
    }

    public String description(){
        return tags.get("description", "unknown");
    }

    public String name(){
        return tags.get("name", "unknown");
    }

    public boolean hasOreGen(){
        return !tags.get("oregen", "0").equals("0");
    }
}
