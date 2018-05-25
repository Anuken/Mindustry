package io.anuke.mindustry.world.blocks.types.storage;

import com.badlogic.gdx.math.Rectangle;
import io.anuke.mindustry.entities.ItemTransfer;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.EnumSet;

import static io.anuke.mindustry.Vars.state;

public class CoreBlock extends StorageBlock {
    private static Rectangle rect = new Rectangle();

    protected int timerSupply = timers ++;

    protected float supplyRadius = 50f;
    protected float supplyInterval = 5f;

    public CoreBlock(String name) {
        super(name);

        solid = true;
        update = true;
        unbreakable = true;
        size = 3;
        hasItems = true;
        itemCapacity = 1000;
        flags = EnumSet.of(BlockFlag.resupplyPoint, BlockFlag.target);
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        if(acceptItem(item, tile, tile) && hasItems && source.team == tile.getTeam()){
            return Math.min(itemCapacity - tile.entity.items.getItem(item), amount);
        }else{
            return 0;
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        return tile.entity.items.items[item.id]< itemCapacity && item.type == ItemType.material;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(Palette.accent);
        Lines.dashCircle(tile.drawx(), tile.drawy(), supplyRadius);
        Draw.color();
    }

    @Override
    public void onDestroyed(Tile tile){
        //TODO more dramatic effects
        super.onDestroyed(tile);

        if(state.teams.has(tile.getTeam())){
            state.teams.get(tile.getTeam()).cores.removeValue(tile, true);
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Net.server() || !Net.active()) super.handleItem(item, tile, source);
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
                        ItemTransfer.create(item, tile.drawx(), tile.drawy(), unit, () -> {});
                        return;
                    }
                }
            });
        }
    }
}
