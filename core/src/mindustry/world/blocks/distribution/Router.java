package mindustry.world.blocks.distribution;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class Router extends Block{
    public float speed = 8f;

    public Router(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 1;
        group = BlockGroup.transportation;
        unloadable = false;
    }

    public class RouterEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;

        @Override
        public void updateTile(){
            if(lastItem == null && items.any()){
                items.clear();
            }

            if(lastItem != null){
                time += 1f / speed * delta();
                Tilec target = getTileTarget(lastItem, lastInput, false);

                if(target != null && (time >= 1f || !(target.block() instanceof Router))){
                    getTileTarget(lastItem, lastInput, true);
                    target.handleItem(this, lastItem);
                    items.remove(lastItem, 1);
                    lastItem = null;
                }
            }
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return team == source.team() && lastItem == null && items.total() == 0;
        }

        @Override
        public void handleItem(Tilec source, Item item){
            items.add(item, 1);
            lastItem = item;
            time = 0f;
            lastInput = source.tile();
        }

        @Override
        public int removeStack(Item item, int amount){
            int result = super.removeStack(item, amount);
            if(result != 0 && item == lastItem){
                lastItem = null;
            }
            return result;
        }

        Tilec getTileTarget(Item item, Tile from, boolean set){
            int counter = tile.rotation();
            for(int i = 0; i < proximity.size; i++){
                Tilec other = proximity.get((i + counter) % proximity.size);
                if(set) tile.rotation((byte)((tile.rotation() + 1) % proximity.size));
                if(other.tile() == from && from.block() == Blocks.overflowGate) continue;
                if(other.acceptItem(this, item)){
                    return other;
                }
            }
            return null;
        }
    }
}
