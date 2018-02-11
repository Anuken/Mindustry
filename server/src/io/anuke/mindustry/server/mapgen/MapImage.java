package io.anuke.mindustry.server.mapgen;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;
import io.anuke.ucore.util.Mathf;

public class MapImage {
    public final Pixmap pixmap;
    public final int width, height;

    public MapImage(Pixmap pixmap){
        this.pixmap = pixmap;
        this.width = pixmap.getWidth();
        this.height = pixmap.getHeight();
    }

    public MapImage(int width, int height){
        this(new Pixmap(width, height, Format.RGBA8888));
    }

    public Block wall(int x, int y){
        BlockPair pair = ColorMapper.get(pixmap.getPixel(x, y));
        return pair.wall;
    }

    public Block floor(int x, int y){
        BlockPair pair = ColorMapper.get(pixmap.getPixel(x, y));
        return pair.floor;
    }

    public void set(int x, int y, Block block){
        pixmap.drawPixel(x, y, ColorMapper.getColor(block));
    }

    public Block get(int x, int y){
        BlockPair pair = ColorMapper.get(pixmap.getPixel(x, y));
        return pair.dominant();
    }

    public boolean solid(int x, int y){
        BlockPair pair = ColorMapper.get(pixmap.getPixel(x, y));
        return pair.dominant().solid;
    }

    public boolean has(int x, int y){
        return Mathf.inBounds(x, y, width, height);
    }

    public int pack(int x, int y){
        return x + y*width;
    }
}
