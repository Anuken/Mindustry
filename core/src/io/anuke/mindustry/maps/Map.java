package io.anuke.mindustry.maps;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.Texture;

import java.io.InputStream;

public class Map{
    /** Internal map name. This is the filename, without any extensions.*/
    public final String name;
    /** Whether this is a custom map.*/
    public final boolean custom;
    /** Metadata. Author description, display name, etc.*/
    public final ObjectMap<String, String> tags;
    /** Supplies a new input stream with the data of this map.*/
    public final Supplier<InputStream> stream;
    /** Preview texture.*/
    public Texture texture;

    public Map(String name, ObjectMap<String, String> tags, boolean custom, Supplier<InputStream> streamSupplier){
        this.name = name;
        this.custom = custom;
        this.tags = tags;
        this.stream = streamSupplier;
    }

    public Map(String name){
        this(name, new ObjectMap<>(), true, () -> null);
    }

    public String getDisplayName(){
        return tags.get("name", name);
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

    public boolean hasOreGen(){
        return !tags.get("oregen", "0").equals("0");
    }

    @Override
    public String toString(){
        return "Map{" +
                "name='" + name + '\'' +
                ", custom=" + custom +
                ", tags=" + tags +
                '}';
    }
}
