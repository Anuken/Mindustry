package io.anuke.mindustry.world.blocks.storage;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.net.In;
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

        if(entity.items.total() == 0 && entity.timer.get(timerUnload, speed)){
            tile.allNearby(other -> {
                if(other.block() instanceof StorageBlock && entity.items.total() == 0 &&
                        ((StorageBlock)other.block()).hasItem(other, entity.sortItem)){
                    offloadNear(tile, ((StorageBlock)other.block()).removeItem(other, entity.sortItem));
                }
            });
        }

        if(entity.items.total() > 0){
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
        buildItemTable(table, () -> entity.sortItem, item -> CallBlocks.setSortedUnloaderItem(null, tile, item));
    }

    @Override
    public TileEntity getEntity(){
        return new SortedUnloaderEntity();
    }

    @Remote(targets = Loc.both, called = Loc.both, in = In.blocks, forward = true)
    public static void setSortedUnloaderItem(Player player, Tile tile, Item item){
        SortedUnloaderEntity entity = tile.entity();
        entity.sortItem = item;
    }

    public static class SortedUnloaderEntity extends TileEntity{
        public Item sortItem = Items.tungsten;

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
