package mindustry.world.blocks.power;

import mindustry.gen.*;
import mindustry.type.Item;
import mindustry.world.*;

import static mindustry.Vars.netServer;
import static mindustry.content.Items.thorium;

public class DecayGenerator extends ItemLiquidGenerator{

    private int share = timers++;

    public DecayGenerator(String name){
        super(true, false, name);
        hasItems = true;
        hasLiquids = false;
        sync = true;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.radioactivity;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        if(!tile.entity.timer.get(share, 20)) return;

        tile.entity.proximity().each(other -> {
            if(other.block != tile.block) return;

            if(tile.entity.items.has(thorium, other.entity.items.get(thorium) + 2)){
                tile.entity.items.remove(thorium, 1);
                netServer.titanic.add(tile);
                Call.transferItemTo(thorium, 1, tile.drawx(), tile.drawy(), other);
            }
        });
    }
}
