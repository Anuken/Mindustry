package io.anuke.mindustry.world.blocks.storage;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.ItemSelection;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class Unloader extends Block{
    protected float speed = 1f;
    protected final int timerUnload = timers++;

    private static Item lastItem;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        hasItems = true;
        configurable = true;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return !(to.block() instanceof StorageBlock);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void playerPlaced(Tile tile){
        Core.app.post(() -> Call.setSortedUnloaderItem(null, tile, lastItem));
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void setSortedUnloaderItem(Player player, Tile tile, Item item){
        SortedUnloaderEntity entity = tile.entity();
        entity.items.clear();
        entity.sortItem = item;
    }

    @Override
    public void update(Tile tile){
        SortedUnloaderEntity entity = tile.entity();

        if(tile.entity.timer.get(timerUnload, speed / entity.timeScale) && tile.entity.items.total() == 0){
            for(Tile other : tile.entity.proximity()){
                if(other.interactable(tile.getTeam()) && other.block().unloadable && other.block().hasItems && entity.items.total() == 0 &&
                ((entity.sortItem == null && other.entity.items.total() > 0) || hasItem(other, entity.sortItem))){
                    offloadNear(tile, removeItem(other, entity.sortItem));
                }
            }
        }

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

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        SortedUnloaderEntity entity = tile.entity();

        Draw.color(entity.sortItem == null ? Color.clear : entity.sortItem.color);
        Fill.square(tile.worldx(), tile.worldy(), 1f);
        Draw.color();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SortedUnloaderEntity entity = tile.entity();
        ItemSelection.buildItemTable(table, () -> entity.sortItem, item -> {
            lastItem = item;
            Call.setSortedUnloaderItem(null, tile, item);
        });
    }

    @Override
    public TileEntity newEntity(){
        return new SortedUnloaderEntity();
    }

    public static class SortedUnloaderEntity extends TileEntity{
        public Item sortItem = null;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            byte id = stream.readByte();
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}
