package mindustry.world.blocks.storage;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.content;

public class Unloader extends Block{
    public float speed = 1f;
    public final int timerUnload = timers++;

    private static Item lastItem;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        hasItems = true;
        configurable = true;
    config(Item.class, (tile, item) -> {
            tile.items.clear();
            tile.<UnloaderEntity>ent().sortItem = item;
        });

        configClear(tile -> tile.<UnloaderEntity>ent().sortItem = null);
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, (Item)req.config, "unloader-center");
    }

    @Override
    public boolean canDump(Tile to, Item item){
        return !(to.block() instanceof StorageBlock);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void playerPlaced(){
        if(lastItem != null){
            tile.configure(lastItem);
        }
    }

    @Override
    public void updateTile(){
        if(timer(timerUnload, speed / timeScale()) && tile.items.total() == 0){
            for(Tile other : tile.proximity()){
                if(other.interactable(team) && other.block().unloadable && other.block().hasItems && items.total() == 0 &&
                ((sortItem == null && other.items.total() > 0) || hasItem(other, sortItem))){
                    offloadNear(tile, removeItem(other, sortItem));
                }
            }
        }

        if(items.total() > 0){
            tryDump(tile);
        }
    }

    /**
     * Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.
     */
    private Item removeItem(Item item){
        Tilec entity = tile.entity;

        if(item == null){
            return items.take();
        }else{
            if(items.has(item)){
                items.remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    private boolean hasItem(Item item){
        Tilec entity = tile.entity;
        if(item == null){
            return items.total() > 0;
        }else{
            return items.has(item);
        }
    }

    @Override
    public void draw(){
        super.draw();

        Draw.color(sortItem == null ? Color.clear : sortItem.color);
        Draw.rect("unloader-center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public void buildConfiguration(Table table){
        ItemSelection.buildTable(table, content.items(), () -> tile.<UnloaderEntity>ent().sortItem, item -> tile.configure(lastItem = item));
    }

    public class UnloaderEntity extends TileEntity{
        public Item sortItem = null;

        @Override
        public Item config(){
            return sortItem;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte id = read.b();
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}
