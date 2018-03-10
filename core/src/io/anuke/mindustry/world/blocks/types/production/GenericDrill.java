package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

/**Generic drill, can use both power and liquids. Set 'resource' to null to make it drill any block with drops.*/
public class GenericDrill extends Drill{
    /**power use per frame.*/
    public float powerUse = 0.08f;
    /**liquid use per frame.*/
    protected float liquidUse = 0.1f;
    protected Liquid inputLiquid = Liquid.water;

    private Array<Item> toAdd = new Array<>();

    public GenericDrill(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){
        toAdd.clear();

        DrillEntity entity = tile.entity();

        float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());
        float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());

        if((!hasPower || entity.power.amount >= powerUsed)
                && (!hasLiquids || entity.liquid.amount >= liquidUsed)){
            if(hasPower) entity.power.amount -= powerUsed;
            if(hasLiquids) entity.liquid.amount -= liquidUsed;
            entity.time += Timers.delta();
        }else{
            return;
        }

        for(Tile other : tile.getLinkedTiles(tempTiles)){
            if(isValid(other)){
                toAdd.add(result == null ? other.floor().drops.item : result);
            }
        }

        if(toAdd.size > 0 && entity.power.amount > powerUse && entity.time >= drillTime
                && tile.entity.inventory.totalItems() < itemCapacity){
            for(Item item : toAdd) offloadNear(tile, item);
            Effects.effect(drillEffect, tile.drawx(), tile.drawy());
            entity.time = 0f;
        }

        if(entity.timer.get(timerDump, 30)){
            tryDump(tile);
        }
    }

    @Override
    protected boolean isValid(Tile tile){
        if(resource == null) {
            return tile.floor().drops != null;
        }else{
            return tile.floor() == resource || (resource.drops != null && resource.drops.equals(tile.floor().drops));
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
    }

    @Override
    public TileEntity getEntity() {
        return new DrillEntity();
    }

    static class DrillEntity extends TileEntity{
        public float time;
    }
}
