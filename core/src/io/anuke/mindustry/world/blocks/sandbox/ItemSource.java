package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.arc.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;

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

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<ItemSourceEntity>entity().outputItem = content.item(value);
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastItem != null){
            Core.app.post(() -> tile.configure(lastItem.id));
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, content.item(req.config), "center");
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
            tile.configure(item == null ? -1 : item.id);
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
        public int config(){
            return outputItem == null ? -1 : outputItem.id;
        }

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
