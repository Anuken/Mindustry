package io.anuke.mindustry.world.blocks.types.units;

import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.effect.ItemTransferEffect;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.flags.BlockFlag;
import io.anuke.ucore.util.EnumSet;

public class ResupplyPoint extends Block{
    private static Rectangle rect = new Rectangle();

    protected int timerSupply = timers ++;

    protected float supplyRadius = 50f;
    protected float supplyInterval = 5f;

    public ResupplyPoint(String name) {
        super(name);
        update = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.resupplyPoint);
    }

    @Override
    public void update(Tile tile) {

        if(tile.entity.timer.get(timerSupply, supplyInterval)){
            rect.setSize(supplyRadius*2).setCenter(tile.drawx(), tile.drawy());

            Units.getNearby(tile.getTeam(), rect, unit -> {
                if(unit.distanceTo(tile.drawx(), tile.drawy()) > supplyRadius) return;

                for(int i = 0; i < tile.entity.items.items.length; i ++){
                    Item item = Item.getByID(i);
                    if(tile.entity.items.items[i] > 0 && unit.acceptsAmmo(item)){
                        tile.entity.items.items[i] --;
                        unit.addAmmo(item);
                        new ItemTransferEffect(item, tile.drawx(), tile.drawy(), unit).add();
                        return;
                    }
                }
            });
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return tile.entity.items.totalItems() < itemCapacity;
    }
}
