package io.anuke.mindustry.world.blocks.types.storage;

import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public class CoreBlock extends StorageBlock {
    protected int capacity = 1000;

    public CoreBlock(String name) {
        super(name);

        health = 800;
        solid = true;
        destructible = true;
        size = 3;
        hasInventory = false;
    }

    public void onDestroyed(Tile tile){
        //TODO more dramatic effects
        super.onDestroyed(tile);
        world.getAllyCores().removeValue(tile, true);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Net.server() || !Net.active()) state.inventory.addItem(item, 1);
    }

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
    }
}
