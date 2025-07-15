package mindustry.world.blocks;

import arc.*;
import arc.graphics.g2d.*;

public class TileBitmask{
    /** Autotile bitmasks for 8-directional sprites (see <a href="https://github.com/GglLfr/tile-gen">tile-gen</a>)*/
    public static final int[] values = {
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    3,  4,  3,  4, 15, 40, 15, 20,  3,  4,  3,  4, 15, 40, 15, 20,
    5, 28,  5, 28, 29, 10, 29, 23,  5, 28,  5, 28, 31, 11, 31, 32,
    3,  4,  3,  4, 15, 40, 15, 20,  3,  4,  3,  4, 15, 40, 15, 20,
    2, 30,  2, 30,  9, 46,  9, 22,  2, 30,  2, 30, 14, 44, 14,  6,
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    39, 36, 39, 36, 27, 16, 27, 24, 39, 36, 39, 36, 27, 16, 27, 24,
    38, 37, 38, 37, 17, 41, 17, 43, 38, 37, 38, 37, 26, 21, 26, 25,
    3,  0,  3,  0, 15, 42, 15, 12,  3,  0,  3,  0, 15, 42, 15, 12,
    5,  8,  5,  8, 29, 35, 29, 33,  5,  8,  5,  8, 31, 34, 31,  7,
    3,  0,  3,  0, 15, 42, 15, 12,  3,  0,  3,  0, 15, 42, 15, 12,
    2,  1,  2,  1,  9, 45,  9, 19,  2,  1,  2,  1, 14, 18, 14, 13,
    };

    public static TextureRegion[] load(String name){
        var regions = new TextureRegion[47];
        for(int i = 0; i < 47; i++){
            regions[i] = Core.atlas.find(name + "-" + i);
        }
        return regions;
    }

    public static TextureRegion[][] loadVariants(String name, int variants){
        var regions = new TextureRegion[variants][47];
        for(int v = 0; v < variants; v++){
            for(int i = 0; i < 47; i++){
                regions[v][i] = Core.atlas.find(name + "-" + (v+1) + "-" + i);
            }
        }

        return regions;
    }
}
