package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.ItemSelection;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class Sorter extends Block{
    private static Item lastItem;

    protected int bufferCapacity = 20;
    protected float speed = 45f;

    public Sorter(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.transportation;
        configurable = true;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public void playerPlaced(Tile tile){
        Core.app.post(() -> Call.setSorterItem(null, tile, lastItem));
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void setSorterItem(Player player, Tile tile, Item item){
        SorterEntity entity = tile.entity();
        if(entity != null){
            entity.sortItem = item;
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        SorterEntity entity = tile.entity();
        if(entity.sortItem == null) return;

        Draw.color(entity.sortItem.color);
        Draw.rect("center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public void update(Tile tile){
        SorterEntity entity = tile.entity();

        for(int i = 0; i < 4; i++){
            Item item = entity.buffer.poll(i);
            if(item != null){
                Tile other = getTileTarget(item, tile, tile.getNearby(i), true);
                if(other != null && other.block().acceptItem(item, other, tile)){
                    other.block().handleItem(item, other, tile);
                    entity.buffer.remove(i);
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        SorterEntity entity = tile.entity();
        return entity.buffer.accepts(tile.relativeTo(source.x, source.y));
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        SorterEntity entity = tile.entity();
        int buffer = tile.relativeTo(source.x, source.y);
        if(entity.buffer.accepts(buffer)){
            entity.buffer.accept(buffer, item);
        }
    }

    @Nullable
    Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
        SorterEntity entity = dest.entity();

        int dir = source.relativeTo(dest.x, dest.y);
        if(dir == -1) return null;
        Tile to;

        if(item == entity.sortItem){
            to = dest.getNearby(dir);
        }else{
            Tile a = dest.getNearby(Mathf.mod(dir - 1, 4));
            Tile b = dest.getNearby(Mathf.mod(dir + 1, 4));
            boolean ac = a != null && a.block().acceptItem(item, a, dest);
            boolean bc = b != null && b.block().acceptItem(item, b, dest);

            if(ac && !bc){
                to = a;
            }else if(bc && !ac){
                to = b;
            }else if(!bc){
                return null;
            }else{
                if(dest.rotation() == 0){
                    to = a;
                    if(flip) dest.rotation((byte)1);
                }else{
                    to = b;
                    if(flip) dest.rotation((byte)0);
                }
            }
        }

        return to;
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SorterEntity entity = tile.entity();
        ItemSelection.buildItemTable(table, () -> entity.sortItem, item -> {
            lastItem = item;
            Call.setSorterItem(null, tile, item);
        });
    }

    @Override
    public TileEntity newEntity(){
        return new SorterEntity();
    }

    public class SorterEntity extends TileEntity{
        DirectionalItemBuffer buffer = new DirectionalItemBuffer(bufferCapacity, speed);
        Item sortItem;

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeShort(sortItem == null ? -1 : sortItem.id);
            buffer.write(stream);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            sortItem = content.item(stream.readShort());
            if(revision == 1){
                buffer.read(stream);
            }
        }
    }
}
