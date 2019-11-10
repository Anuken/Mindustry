package io.anuke.mindustry.world.blocks.storage;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

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
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, content.item(req.config), "unloader-center");
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
        if(lastItem != null){
            tile.configure(lastItem.id);
        }
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.entity.items.clear();
        tile.<UnloaderEntity>entity().sortItem = content.item(value);
    }

    @Override
    public void update(Tile tile){
        UnloaderEntity entity = tile.entity();

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

        UnloaderEntity entity = tile.entity();

        Draw.color(entity.sortItem == null ? Color.clear : entity.sortItem.color);
        Draw.rect("unloader-center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        UnloaderEntity entity = tile.entity();
        ItemSelection.buildItemTable(table, () -> entity.sortItem, item -> {
            lastItem = item;
            tile.configure(item == null ? -1 : item.id);
        });
    }

    @Override
    public TileEntity newEntity(){
        return new UnloaderEntity();
    }

    public static class UnloaderEntity extends TileEntity{
        public Item sortItem = null;

        @Override
        public int config(){
            return sortItem == null ? -1 : sortItem.id;
        }

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
