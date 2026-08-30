package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class StaticWall extends Prop{
    public @Load("@-large") TextureRegion large;
    public TextureRegion[][] split;
    /** If true, this wall uses autotiling; variants are not supported. See https://github.com/GglLfr/tile-gen*/
    public boolean autotile;
    /** If >1, the middle region of the autotile has random variants. */
    public int autotileMidVariants = 1;

    protected TextureRegion[] autotileRegions, autotileMidRegions;

    public StaticWall(String name){
        super(name);
        breakable = alwaysReplace = unitMoveBreakable = false;
        solid = true;
        variants = 2;
        cacheLayer = CacheLayer.walls;
        allowRectanglePlacement = true;
        placeEffect = Fx.rotateBlock;
        instantBuild = true;
        ignoreBuildDarkness = true;
        placeableLiquid = true;
    }

    @Override
    public void drawBase(Tile tile){
        if(autotile){
            int bits = 0;

            for(int i = 0; i < 8; i++){
                Tile other = tile.nearby(Geometry.d8[i]);
                if(checkAutotileSame(tile, other)){
                    bits |= (1 << i);
                }
            }

            int bit = TileBitmask.values[bits];

            TextureRegion region = bit == 13 && autotileMidVariants > 1 ? autotileMidRegions[variant(tile.x, tile.y, autotileMidRegions.length)] : autotileRegions[bit];

            Draw.rect(region, tile.worldx(), tile.worldy());
        }else{
            int rx = tile.x / 2 * 2;
            int ry = tile.y / 2 * 2;

            if(Core.atlas.isFound(large) && eq(rx, ry) && Mathf.randomSeed(Point2.pack(rx, ry)) < 0.5 && split.length >= 2 && split[0].length >= 2){
                Draw.rect(split[tile.x % 2][1 - tile.y % 2], tile.worldx(), tile.worldy());
            }else if(variants > 0){
                Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());
            }else{
                Draw.rect(region, tile.worldx(), tile.worldy());
            }
        }

        //draw ore on top
        if(tile.overlay().wallOre){
            tile.overlay().drawBase(tile);
        }
    }

    public int variant(int x, int y, int max){
        return Mathf.randomSeed(Point2.pack(x, y), 0, Math.max(0, max - 1));
    }

    public boolean checkAutotileSame(Tile tile, @Nullable Tile other){
        return other != null && other.block() == this;
    }

    @Override
    public void load(){
        super.load();
        int size = large.width / 2;
        split = large.split(size, size);
        if(split != null){
            for(var arr : split){
                for(var reg : arr){
                    reg.scale = region.scale;
                }
            }
        }

        if(autotile){
            autotileRegions = TileBitmask.load(name);

            if(autotileMidVariants > 1){
                autotileMidRegions = new TextureRegion[autotileMidVariants];
                for(int i = 0; i < autotileMidVariants; i++){
                    autotileMidRegions[i] = Core.atlas.find(i == 0 ? name + "-13" : name + "-mid-" + (i + 1));
                }
            }
        }
    }

    @Override
    public boolean canReplace(Block other){
        return other instanceof StaticWall || super.canReplace(other);
    }

    boolean eq(int rx, int ry){
        return rx < world.width() - 1 && ry < world.height() - 1
            && world.tile(rx + 1, ry).block() == this
            && world.tile(rx, ry + 1).block() == this
            && world.tile(rx, ry).block() == this
            && world.tile(rx + 1, ry + 1).block() == this;
    }
}
