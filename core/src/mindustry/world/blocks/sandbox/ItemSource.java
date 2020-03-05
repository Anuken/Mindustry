package mindustry.world.blocks.sandbox;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

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

        config(Item.class, (tile, item) -> tile.<ItemSourceEntity>ent().outputItem = item);
        configClear(tile -> tile.<ItemSourceEntity>ent().outputItem = null);
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastItem != null){
            Core.app.post(() -> tile.configure(lastItem));
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, req.config, "center");
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

        entity.items().set(entity.outputItem, 1);
        tryDump(tile, entity.outputItem);
        entity.items().set(entity.outputItem, 0);
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        ItemSourceEntity entity = tile.ent();
        ItemSelection.buildTable(table, content.items(), () -> entity.outputItem, item -> tile.configure(lastItem = item));
    }

    @Override
    public boolean acceptItem(Tile tile, Tile source, Item item){
        return false;
    }

    public class ItemSourceEntity extends TileEntity{
        Item outputItem;

        @Override
        public Item config(){
            return outputItem;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(outputItem == null ? -1 : outputItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            outputItem = content.item(read.s());
        }
    }
}
