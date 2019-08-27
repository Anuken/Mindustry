package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.ItemSelection;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class ItemSource extends Block{
    private static Item lastItem;

    public ItemSource(String name){
        super(name);
        hasItems = true;
        update = true;
        solid = true;
        group = BlockGroup.transportation;
        configurable = true;
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void setItemSourceItem(Player player, Tile tile, Item item){
        ItemSourceEntity entity = tile.entity();
        if(entity != null){
            entity.outputItem = item;
        }
    }

    @Override
    public void playerPlaced(Tile tile){
        Core.app.post(() -> Call.setItemSourceItem(null, tile, lastItem));
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        ItemSourceEntity entity = tile.entity();
        if(entity.outputItem == null) return;

        Draw.color(entity.outputItem.color);
        Draw.rect("center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public void update(Tile tile){
        ItemSourceEntity entity = tile.entity();
        if(entity.outputItem == null) return;

        entity.items.set(entity.outputItem, 1);
        tryDump(tile, entity.outputItem);
        entity.items.set(entity.outputItem, 0);
    }

    @Override
    public void buildTable(Tile tile, Table table){
        ItemSourceEntity entity = tile.entity();
        ItemSelection.buildItemTable(table, () -> entity.outputItem, item -> {
            lastItem = item;
            Call.setItemSourceItem(null, tile, item);
        });
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
    }

    @Override
    public TileEntity newEntity(){
        return new ItemSourceEntity();
    }

    public class ItemSourceEntity extends TileEntity{
        Item outputItem;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeShort(outputItem == null ? -1 : outputItem.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            outputItem = content.item(stream.readShort());
        }
    }
}
