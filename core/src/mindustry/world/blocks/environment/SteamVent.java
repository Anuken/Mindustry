package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

//can't use an overlay for this because it spans multiple tiles
public class SteamVent extends Floor{
    public static final Point2[] defaultOffsets = {
        new Point2(0, 0),
        new Point2(1, 0),
        new Point2(1, 1),
        new Point2(0, 1),
        new Point2(-1, 1),
        new Point2(-1, 0),
        new Point2(-1, -1),
        new Point2(0, -1),
        new Point2(1, -1),
    };

    public Block parent = Blocks.air;
    public Effect effect = Fx.ventSteam;
    public Color effectColor = Pal.vent;
    public float effectSpacing = 15f;
    public @Nullable Point2[] offsets = null;

    static{
        for(var p : defaultOffsets){
            p.sub(1, 1);
        }
    }

    public SteamVent(String name){
        super(name);
        variants = 2;
    }

    @Override
    public void init(){
        super.init();

        if(offsets == null){
            offsets = defaultOffsets;
        }else{
            //Correct offsets. Top-right corner should be (0, 0).
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for(Point2 p : offsets){
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            for(Point2 p : offsets){
                p.sub(maxX, maxY);
            }

            //Check if (0, 0) exists after offset correction.
            boolean hasOrigin = false;
            for(Point2 p : offsets){
                if(p.x == 0 && p.y == 0){
                    hasOrigin = true;
                    break;
                }
            }
            if(!hasOrigin) throw new IllegalArgumentException("SteamVent lacks a top-right corner.");
        }
    }

    @Override
    public void drawBase(Tile tile){
        parent.drawBase(tile);

        if(checkAdjacent(tile)){
            Mathf.rand.setSeed(tile.pos());
            Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx() - tilesize, tile.worldy() - tilesize);
        }
    }

    @Override
    public boolean updateRender(Tile tile){
        return checkAdjacent(tile);
    }

    @Override
    public void renderUpdate(UpdateRenderState state){
        if(state.tile.nearby(-1, -1) != null && state.tile.nearby(-1, -1).block() == Blocks.air && (state.data += Time.delta) >= effectSpacing){
            effect.at(state.tile.x * tilesize - tilesize, state.tile.y * tilesize - tilesize, effectColor);
            state.data = 0f;
        }
    }

    public boolean checkAdjacent(Tile tile){
        for(var point : offsets){
            Tile other = Vars.world.tile(tile.x + point.x, tile.y + point.y);
            if(other == null || other.floor() != this){
                return false;
            }
        }
        return true;
    }
}
