package io.anuke.mindustry.world.blocks.storage;

import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.data;
import static io.anuke.mindustry.Vars.world;

public class LaunchPad extends StorageBlock{
    protected final int timerLaunch = timers++;
    /**Time inbetween launches.*/
    protected float launchTime;

    public LaunchPad(String name){
        super(name);
        update = true;
        hasItems = true;
        solid = true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return item.type == ItemType.material && super.acceptItem(item, tile, source);
    }

    @Override
    public void update(Tile tile){
        TileEntity entity = tile.entity;

        if(entity.cons.valid() && world.isZone()){
            for(Item item : Vars.content.items()){
                if(entity.items.get(item) >= itemCapacity && entity.timer.get(timerLaunch, launchTime / entity.timeScale)){
                    //TODO play animation of some sort
                    Effects.effect(Fx.dooropenlarge, tile);
                    data.addItem(item, entity.items.get(item));
                    entity.items.set(item, 0);
                }
            }
        }
    }
}
