package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock.PowerEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class PowerDrill extends Drill implements PowerAcceptor {
    public float powerCapacity = 10f;
    /**power use per frame.*/
    public float powerUse = 0.08f;

    private Array<ItemStack> toAdd = new Array<>();

    public PowerDrill(String name){
        super(name);

        bars.add(new BlockBar(Color.YELLOW, true, tile -> tile.<PowerEntity>entity().power / powerCapacity));
    }

    @Override
    public void update(Tile tile){
        toAdd.clear();

        PowerEntity entity = tile.entity();

        float used = Math.min(powerUse * Timers.delta(), powerCapacity-0.1f);

        if(entity.power >= used){
            entity.power -= used;
        }

        for(Tile other : tile.getLinkedTiles(tempTiles)){
            if(isValid(other)){
                toAdd.add(other.floor().drops);
            }
        }

        if(toAdd.size > 0 && entity.power > powerUse && entity.timer.get(timerDrill, 60 * time)
                && tile.entity.totalItems() < capacity){
            for(ItemStack stack : toAdd) offloadNear(tile, stack.item);
            Effects.effect(drillEffect, tile.drawx(), tile.drawy());
        }

        if(entity.timer.get(timerDump, 30)){
            tryDump(tile);
        }
    }

    @Override
    public boolean acceptsPower(Tile tile){
        PowerEntity entity = tile.entity();

        return entity.power + 0.001f <= powerCapacity;
    }

    @Override
    public float addPower(Tile tile, float amount){
        PowerEntity entity = tile.entity();

        float canAccept = Math.min(powerCapacity - entity.power, amount);

        entity.power += canAccept;

        return canAccept;
    }

    @Override
    public void setPower(Tile tile, float power){
        PowerEntity entity = tile.entity();
        entity.power = power;
    }

    @Override
    public TileEntity getEntity() {
        return new PowerEntity();
    }

    @Override
    protected boolean isValid(Tile tile){
        return tile.floor().drops != null;
    }
}
