package io.anuke.mindustry.maps;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Texture;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.MapIO;

public class Map{
    /** Whether this is a custom map.*/
    public final boolean custom;
    /** Metadata. Author description, display name, etc.*/
    public final ObjectMap<String, String> tags;
    /** Base file of this map. File can be named anything at all.*/
    public final FileHandle file;
    /** Format version.*/
    public final int version;
    /** Map width/height, shorts.*/
    public int width, height;
    /** Preview texture.*/
    public Texture texture;
    /** Build that this map was created in. -1 = unknown or custom build.*/
    public int build;

    public Map(FileHandle file, int width, int height, ObjectMap<String, String> tags, boolean custom, int version, int build){
        this.custom = custom;
        this.tags = tags;
        this.file = file;
        this.width = width;
        this.height = height;
        this.version = version;
        this.build = build;
    }

    public Map(FileHandle file, int width, int height, ObjectMap<String, String> tags, boolean custom, int version){
        this(file, width, height, tags, custom, version, -1);
    }

    public Map(FileHandle file, int width, int height, ObjectMap<String, String> tags, boolean custom){
        this(file, width, height, tags, custom, MapIO.version);
    }

    public int getHightScore(){
        return Core.settings.getInt("hiscore" + file.nameWithoutExtension(), 0);
    }

    public void setHighScore(int score){
        Core.settings.put("hiscore" + file.nameWithoutExtension(), score);
        Vars.data.modified();
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

    public boolean hasTag(String name){
        return tags.containsKey(name);
    }

    @Override
    public String toString(){
        return "Map{" +
                "file='" + file + '\'' +
                ", custom=" + custom +
                ", tags=" + tags +
                '}';
    }
}
