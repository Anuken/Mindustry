package io.anuke.mindustry.maps;

import io.anuke.arc.Core;
import io.anuke.arc.collection.IntIntMap;
import io.anuke.arc.collection.ObjectMap;

//todo: specify preferred game rules here; can be overriden
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
        return tag("author");
    }

    public String description(){
        return tag("description");
    }

    public String name(){
        return tag("name");
    }

    public String tag(String name){
        return tags.containsKey(name) && !tags.get(name).trim().isEmpty() ? tags.get(name): Core.bundle.get("unknown");
    }

    @Override
    public String toString(){
        return "MapMeta{" +
                "tags=" + tags +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
