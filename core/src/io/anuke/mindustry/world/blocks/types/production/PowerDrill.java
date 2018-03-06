package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.mindustry.world.blocks.types.PowerBlock.PowerEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class PowerDrill extends Drill implements PowerAcceptor {
    public float powerCapacity = 10f;
    /**power use per frame.*/
    public float powerUse = 0.08f;

    public PowerDrill(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){
        PowerEntity entity = tile.entity();

        int mines = 0;

        float used = Math.min(entity.power * Timers.delta(), powerCapacity-0.1f);

        if(entity.power >= used){
            entity.power -= used;
        }

        if(isMultiblock()){
            for(Tile other : tile.getLinkedTiles(tempTiles)){
                if(isValid(other)){
                    mines ++;
                }
            }
        }else{
            if(isValid(tile)) mines = 1;
        }

        if(mines > 0 && entity.power > powerUse && entity.timer.get(timerDrill, 60 * time)
                && tile.entity.getItem(result) < capacity){
            for(int i = 0; i < mines; i ++) offloadNear(tile, result);
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
}
