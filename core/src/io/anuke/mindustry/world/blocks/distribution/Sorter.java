package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.SelectionTrait;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Sorter extends Block implements SelectionTrait{

    public Sorter(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
        outputsItems = true;
        group = BlockGroup.transportation;
        configurable = true;
    }

    @Remote(targets = Loc.both, called = Loc.both, in = In.blocks, forward = true)
    public static void setSorterItem(Player player, Tile tile, Item item){
        SorterEntity entity = tile.entity();
        entity.sortItem = item;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        //TODO call event for change

        SorterEntity entity = tile.entity();

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
                if(dest.getDump() == 0){
                    to = a;
                    if(flip)
                        dest.setDump((byte) 1);
                }else{
                    to = b;
                    if(flip)
                        dest.setDump((byte) 0);
                }
            }
        }

        return to;
    }

    @Override
    public void buildTable(Tile tile, Table table){
        SorterEntity entity = tile.entity();
        buildItemTable(table, () -> entity.sortItem, item -> CallBlocks.setSorterItem(null, tile, item));
    }

    @Override
    public TileEntity getEntity(){
        return new SorterEntity();
    }

    public static class SorterEntity extends TileEntity{
        public Item sortItem = Items.tungsten;

        @Override
        public void write(DataOutputStream stream) throws IOException{
            stream.writeByte(sortItem.id);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            sortItem = Item.all().get(stream.readByte());
        }
    }
}
