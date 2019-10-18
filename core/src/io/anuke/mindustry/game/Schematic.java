package io.anuke.mindustry.game;

import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.world.*;

public class Schematic{
    public final Array<Stile> tiles;
    public StringMap tags;
    public int width, height;
    public boolean workshop;
    public @Nullable FileHandle file;

    public Schematic(Array<Stile> tiles, StringMap tags, int width, int height){
        this.tiles = tiles;
        this.tags = tags;
        this.width = width;
        this.height = height;
    }

    public String name(){
        return tags.get("name", "unknown");
    }

    public static class Stile{
        public @NonNull Block block;
        public short x, y;
        public int config;
        public byte rotation;

        public Stile(Block block, int x, int y, int config, byte rotation){
            this.block = block;
            this.x = (short)x;
            this.y = (short)y;
            this.config = config;
            this.rotation = rotation;
        }
    }
}
