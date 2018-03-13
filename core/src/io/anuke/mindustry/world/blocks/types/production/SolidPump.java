package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

/**Pump that makes liquid from solids and takes in power. Only works on solid floor blocks.*/
public class SolidPump extends Pump {
    protected Liquid result = Liquid.water;
    protected float powerUse = 0.1f;

    protected final Array<Tile> drawTiles = new Array<>();

    public SolidPump(String name){
        super(name);
        hasPower = true;
        liquidRegion = name + "-liquid";
    }

    @Override
    public void update(Tile tile){
        float used = Math.min(powerUse * Timers.delta(), powerCapacity);

        float fraction = 0f;

        if(isMultiblock()){
            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(isValid(other)){
                    fraction += 1f/ size;
                }
            }
        }else{
            if(isValid(tile)) fraction = 1f;
        }

        if(tile.entity.power.amount >= used){
            float maxPump = Math.min(liquidCapacity - tile.entity.liquid.amount, pumpAmount * Timers.delta() * fraction);
            tile.entity.liquid.liquid = result;
            tile.entity.liquid.amount += maxPump;
            tile.entity.power.amount -= used;
        }

        tryDumpLiquid(tile);
    }

    @Override
    public boolean isLayer(Tile tile) {
        if(isMultiblock()){
            for(Tile other : tile.getLinkedTiles(drawTiles)){
                if(isValid(other)){
                    return false;
                }
            }
            return true;
        }else{
            return !isValid(tile);
        }
    }

    protected boolean isValid(Tile tile){
        return !tile.floor().liquid;
    }
}
