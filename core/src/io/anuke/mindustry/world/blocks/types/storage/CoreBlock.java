package io.anuke.mindustry.world.blocks.types.storage;

import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.state;

public class CoreBlock extends StorageBlock {

    public CoreBlock(String name) {
        super(name);

        solid = true;
        destructible = true;
        unbreakable = true;
        size = 3;
        hasInventory = true;
        itemCapacity = 2000;
    }

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
    /*

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return item.material && state.inventory.getAmount(item) < capacity;
    }

    @Override
    public Item removeItem(Tile tile, Item item){
        for(int i = 0; i < state.inventory.getItems().length; i ++){
            if(state.inventory.getItems()[i] > 0 && (item == null || item.id == i)){
                if(Net.server() || !Net.active()) state.inventory.getItems()[i] --;
                return Item.getByID(i);
            }
        }
        return null;
    }

    @Override
    public boolean hasItem(Tile tile, Item item){
        for(int i = 0; i < state.inventory.getItems().length; i ++){
            if(state.inventory.getItems()[i] > 0 && (item == null || item.id == i)){
                return true;
            }
        }
        return false;
    }*/
}
