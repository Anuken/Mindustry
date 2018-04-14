package io.anuke.mindustry.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Tile;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.debug;
import static io.anuke.mindustry.Vars.state;

public class Inventory {
    private final int[] empty = new int[Item.getAllItems().size];

    public void clearItems(){
        Arrays.fill(items(), 0);

        if(debug){
            for(Item item : Item.getAllItems()){
                if(item.material) items()[item.id] = 99999;
            }
        }else{
            addItem(Items.iron, 40);
        }
    }

    public void fill(){
        Arrays.fill(items(), 999999999);
    }

    public int getAmount(Item item){
        return items()[item.id];
    }

    public void addItem(Item item, int amount){
        items()[item.id] += amount;
    }

    public boolean hasItems(ItemStack[] items){
        for(ItemStack stack : items)
            if(!hasItem(stack))
                return false;
        return true;
    }

    public boolean hasItems(ItemStack[] items, int scaling){
        for(ItemStack stack : items)
            if(!hasItem(stack.item, stack.amount * scaling))
                return false;
        return true;
    }

    public boolean hasItem(ItemStack req){
        return items()[req.item.id] >= req.amount;
    }

    public boolean hasItem(Item item, int amount){
        return items()[item.id] >= amount;
    }

    public void removeItem(ItemStack req){
        items()[req.item.id] -= req.amount;
        if(items()[req.item.id] < 0) items()[req.item.id] = 0; //prevents negative item glitches in multiplayer
    }

    public void removeItems(ItemStack... reqs){
        for(ItemStack req : reqs)
            removeItem(req);
    }

    public int[] writeItems(){
        return items();
    }

    public int[] readItems(){
        return items();
    }

    /*
    public int[] getItems(){
        updated = true;
        return items();
    }*/

    private int[] items(){
        ObjectSet<TeamData> set = state.teams.getTeams(true);
        if(set.size == 0) return empty;
        Array<Tile> tiles = set.first().cores;
        if(tiles.size == 0) return empty;
        return tiles.first().entity.inventory.items;
    }
}
