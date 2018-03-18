package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Texture;

public class Map {
    /**Internal map name. This is the filename, without any extensions.*/
    public final String name;
    /**Whether this is a custom map.*/
    public final boolean custom;
    /**Metadata. Author description, display name, etc.*/
    public final MapMeta meta;
    /**Preview texture.*/
    public Texture texture;

    public Map(String name, MapMeta meta, boolean custom){
        this.name = name;
        this.custom = custom;
        this.meta = meta;
    }
}
