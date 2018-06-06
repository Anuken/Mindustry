package io.anuke.mindustry.world.blocks.storage;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.SelectionTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SortedUnloader extends Unloader implements SelectionTrait{

    public SortedUnloader(String name){
        super(name);
        configurable = true;
    }

    //TODO call event

    @Override
    public void update(Tile tile){
        SortedUnloaderEntity entity = tile.entity();

        if(entity.items.totalItems() == 0 && entity.timer.get(timerUnload, speed)){
            tile.allNearby(other -> {
                if(other.block() instanceof StorageBlock && entity.items.totalItems() == 0 &&
                        ((StorageBlock)other.block()).hasItem(other, entity.sortItem)){
                    offloadNear(tile, ((StorageBlock)other.block()).removeItem(other, entity.sortItem));
                }
            });
        }

        if(entity.items.totalItems() > 0){
            tryDump(tile);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        SortedUnloaderEntity entity = tile.entity();

        Draw.color(entity.sortItem.color);
        Draw.rect("blank", tile.worldx(), tile.worldy(), 2f, 2f);
        Draw.color();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SortedUnloaderEntity entity = tile.entity();
        buildItemTable(table, () -> entity.sortItem, item -> entity.sortItem = item);
    }

    @Override
    public TileEntity getEntity(){
        return new SortedUnloaderEntity();
    }

    public static class SortedUnloaderEntity extends TileEntity{
        public Item sortItem = Items.iron;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeByte(sortItem.id);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            sortItem = Item.all().get(stream.readByte());
        }
    }
}
