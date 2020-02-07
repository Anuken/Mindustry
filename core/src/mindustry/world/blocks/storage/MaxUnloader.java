package mindustry.world.blocks.storage;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.type.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import java.io.*;

import static mindustry.Vars.content;

public class MaxUnloader extends Block{
    public float speed = 1f;
    public final int timerUnload = timers++;

    public MaxUnloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 75;
        hasItems = true;
        configurable = false;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return !(to.block() instanceof StorageBlock);
    }

    @Override
    public void update(Tile tile){
        TileEntity entity = tile.ent();

        if(tile.entity.timer.get(timerUnload, speed / entity.timeScale) && tile.entity.items.total() == 0){
            for(Tile other : tile.entity.proximity()){
                if(other.interactable(tile.getTeam()) && other.block().unloadable && other.block().hasItems && entity.items.total() == 0){
                    int nThreshold = other.block().getMaximumAccepted(other, null) * 9/16;
                    // Remain inactive until total items is above threshold to let other unloaders have first dibs at items.
                    if(other.entity.items.total() > nThreshold){
                        // If the target is a vault, try to take an item that is above threshold.
                         if(other.block() instanceof Vault) {
                             Item iitem = other.entity.items.takeMaxItem(nThreshold);
                             if(iitem != null)
                                 offloadNear(tile, iitem);
                         }
                         if(other.block() instanceof LaunchPad) {
                             // If the target is a launcher, take whichever item there is the most of.
                              offloadNear(tile, other.entity.items.takeMaxItem(0));
                         }
                    }
                }
            }
        }

        // Still got to empty items that got pushed into the block to avoid stalling.
        if(entity.items.total() > 0){
            tryDump(tile);
        }
    }

    /**
     * Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.
     */
    private Item removeItem(Tile tile, Item item){
        TileEntity entity = tile.entity;

        if(item == null){
            return entity.items.take();
        }else{
            if(entity.items.has(item)){
                entity.items.remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    private boolean hasItem(Tile tile, Item item){
        TileEntity entity = tile.entity;
        if(item == null){
            return entity.items.total() > 0;
        }else{
            return entity.items.has(item);
        }
    }
}
