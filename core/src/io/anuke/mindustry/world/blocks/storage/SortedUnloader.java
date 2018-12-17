package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.graphics.Color;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.SelectionTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.threads;

public class SortedUnloader extends Block implements SelectionTrait{
    protected float speed = 1f;
    protected final int timerUnload = timers++;

    private static Item lastItem;

    public SortedUnloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        hasItems = true;
        configurable = true;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        Block block = to.target().block();
        return !(block instanceof StorageBlock);
    }

    @Override
    public void setBars(){}

    @Override
    public void playerPlaced(Tile tile){
        threads.runDelay(() -> Call.setSortedUnloaderItem(null, tile, lastItem));
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

        if(tile.entity.timer.get(timerUnload, speed) && tile.entity.items.total() == 0){
            for(Tile other : tile.entity.proximity()){
                if(other.getTeam() == tile.getTeam() && other.block() instanceof StorageBlock && entity.items.total() == 0 &&
                ((entity.sortItem == null && other.entity.items.total() > 0) || ((StorageBlock) other.block()).hasItem(other, entity.sortItem))){
                    offloadNear(tile, ((StorageBlock) other.block()).removeItem(other, entity.sortItem));
                }
            }
        }

        if(entity.items.total() > 0){
            tryDump(tile);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        SortedUnloaderEntity entity = tile.entity();

        Draw.color(entity.sortItem == null ? Color.WHITE : entity.sortItem.color);
        Draw.rect("blank", tile.worldx(), tile.worldy(), 2f, 2f);
        Draw.color();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SortedUnloaderEntity entity = tile.entity();
        buildItemTable(table, () -> entity.sortItem, item -> {
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
        public void writeConfig(DataOutput stream) throws IOException{
            stream.writeByte(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void readConfig(DataInput stream) throws IOException{
            byte id = stream.readByte();
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}
