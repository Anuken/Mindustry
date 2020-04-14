package mindustry.world.blocks.sandbox;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.type.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.content;

public class ItemSource extends Block{
    private static Item lastItem;

    public ItemSource(String name){
        super(name);
        hasItems = true;
        update = true;
        solid = true;
        group = BlockGroup.transportation;
        configurable = true;
        entityType = ItemSourceEntity::new;
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<ItemSourceEntity>ent().outputItem = content.item(value);
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

        ItemSourceEntity entity = tile.ent();
        if(entity.outputItem == null) return;

        Draw.color(entity.outputItem.color);
        Draw.rect("center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public void update(Tile tile){
        ItemSourceEntity entity = tile.ent();
        if(entity.outputItem == null) return;

        entity.items.set(entity.outputItem, 1);
        tryDump(tile, entity.outputItem);
        entity.items.set(entity.outputItem, 0);
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        ItemSourceEntity entity = tile.ent();
        ItemSelection.buildTable(table, content.items(), () -> entity.outputItem, item -> {
            lastItem = item;
            tile.configure(item == null ? -1 : item.id);
        });
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return false;
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
