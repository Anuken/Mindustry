package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.ItemSelection;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class Sorter extends Block{
    private static Item lastItem;

    public Sorter(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
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
        Draw.rect("blank", tile.worldx(), tile.worldy(), 4f, 4f);
        Draw.color();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, false);

        return to != null && to.block().acceptItem(item, to, tile);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, true);

        to.block().handleItem(item, to, tile);
    }

    boolean isSame(Tile tile, Tile other){
        return other != null && other.block() == this && other.<SorterEntity>entity().sortItem == tile.<SorterEntity>entity().sortItem;
    }

    Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
        SorterEntity entity = dest.entity();

        int dir = source.relativeTo(dest.x, dest.y);
        if(dir == -1) return null;
        Tile to;

        if(item == entity.sortItem){
            //prevent 3-chains
            if(isSame(dest, source) && isSame(dest, dest.getNearby(dir))){
                return null;
            }
            to = dest.getNearby(dir);
        }else{
            Tile a = dest.getNearby(Mathf.mod(dir - 1, 4));
            Tile b = dest.getNearby(Mathf.mod(dir + 1, 4));
            boolean ac = a != null && !(a.block().instantTransfer && source.block().instantTransfer) &&
                    a.block().acceptItem(item, a, dest);
            boolean bc = b != null && !(b.block().instantTransfer && source.block().instantTransfer) &&
                    b.block().acceptItem(item, b, dest);

            if(ac && !bc){
                to = a;
            }else if(bc && !ac){
                to = b;
            }else if(!bc){
                return null;
            }else{
                if(dest.rotation() == 0){
                    to = a;
                    if(flip)
                        dest.rotation((byte)1);
                }else{
                    to = b;
                    if(flip)
                        dest.rotation((byte)0);
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
        Item sortItem;

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeShort(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            sortItem = content.item(stream.readShort());
            if(revision == 1){
                new DirectionalItemBuffer(20, 45f).read(stream);
            }
        }
    }
}
