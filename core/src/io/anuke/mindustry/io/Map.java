package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Texture;
import io.anuke.ucore.function.Supplier;

import java.io.InputStream;

public class Map {
    /**Internal map name. This is the filename, without any extensions.*/
    public final String name;
    /**Whether this is a custom map.*/
    public final boolean custom;
    /**Metadata. Author description, display name, etc.*/
    public final MapMeta meta;
    /**Supplies a new input stream with the data of this map.*/
    public final Supplier<InputStream> stream;
    /**Preview texture.*/
    public Texture texture;

    public Map(String name, MapMeta meta, boolean custom, Supplier<InputStream> streamSupplier){
        this.name = name;
        this.custom = custom;
        this.meta = meta;
        this.stream = streamSupplier;
    }

    public String getDisplayName(){
        return meta.tags.get("name", name);
    }
}
