package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.graphics.Color;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.SelectionTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static io.anuke.mindustry.Vars.*;

public class SortedUnloader extends Unloader implements SelectionTrait{

    public SortedUnloader(String name){
        super(name);
        configurable = true;
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

        if(entity.items.total() == 0 && entity.timer.get(timerUnload, speed)){
            tile.allNearby(other -> {
                if(other.getTeam() == tile.getTeam() && other.block() instanceof StorageBlock && entity.items.total() == 0 &&
                ((entity.sortItem == null && other.entity.items.total() > 0) || ((StorageBlock) other.block()).hasItem(other, entity.sortItem))){
                    offloadNear(tile, ((StorageBlock) other.block()).removeItem(other, entity.sortItem));
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

        Draw.color(entity.sortItem == null ? Color.WHITE : entity.sortItem.color);
        Draw.rect("blank", tile.worldx(), tile.worldy(), 2f, 2f);
        Draw.color();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SortedUnloaderEntity entity = tile.entity();
        buildItemTable(table, true, () -> entity.sortItem, item -> Call.setSortedUnloaderItem(null, tile, item));
    }

    @Override
    public TileEntity newEntity(){
        return new SortedUnloaderEntity();
    }

    public static class SortedUnloaderEntity extends TileEntity{
        public Item sortItem = null;

        @Override
        public void write(DataOutputStream stream) throws IOException{
            stream.writeByte(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            byte id = stream.readByte();
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}
