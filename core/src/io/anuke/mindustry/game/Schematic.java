package io.anuke.mindustry.game;

import io.anuke.arc.collection.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.world.*;

public class Schematic{
    public final Array<Stile> tiles;
    public int width, height;

    public Schematic(Array<Stile> tiles, int width, int height){
        this.tiles = tiles;
        this.width = width;
        this.height = height;
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
