package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock.PowerEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class PowerDrill extends Drill{
    /**power use per frame.*/
    public float powerUse = 0.08f;

    private Array<ItemStack> toAdd = new Array<>();

    public PowerDrill(String name){
        super(name);

        hasPower = true;
    }

    @Override
    public void update(Tile tile){
        toAdd.clear();

        PowerEntity entity = tile.entity();

        float used = Math.min(powerUse * Timers.delta(), powerCapacity-0.1f);

        if(entity.power.amount >= used){
            entity.power.amount -= used;
        }

        for(Tile other : tile.getLinkedTiles(tempTiles)){
            if(isValid(other)){
                toAdd.add(other.floor().drops);
            }
        }

        if(toAdd.size > 0 && entity.power.amount > powerUse && entity.timer.get(timerDrill, 60 * time)
                && tile.entity.inventory.totalItems() < itemCapacity){
            for(ItemStack stack : toAdd) offloadNear(tile, stack.item);
            Effects.effect(drillEffect, tile.drawx(), tile.drawy());
        }

        if(entity.timer.get(timerDump, 30)){
            tryDump(tile);
        }
    }

    @Override
    protected boolean isValid(Tile tile){
        return tile.floor().drops != null;
    }
}
