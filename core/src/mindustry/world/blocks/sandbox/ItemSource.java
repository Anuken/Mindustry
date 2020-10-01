package mindustry.world.blocks.sandbox;

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

import static mindustry.Vars.*;

public class ItemSource extends Block{

    public ItemSource(String name){
        super(name);
        hasItems = true;
        update = true;
        solid = true;
        group = BlockGroup.transportation;
        configurable = true;
        saveConfig = true;
        noUpdateDisabled = true;

        config(Item.class, (ItemSourceBuild tile, Item item) -> tile.outputItem = item);
        configClear((ItemSourceBuild tile) -> tile.outputItem = null);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
        drawRequestConfigCenter(req, req.config, "center");
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class ItemSourceBuild extends Building{
        Item outputItem;

        @Override
        public void draw(){
            super.draw();

            if(outputItem == null){
                Draw.rect("cross", x, y);
            }else{
                Draw.color(outputItem.color);
                Draw.rect("center", x, y);
                Draw.color();
            }
        }

        @Override
        public void updateTile(){
            if(outputItem == null) return;

            items.set(outputItem, 1);
            dump(outputItem);
            items.set(outputItem, 0);
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.items(), () -> outputItem, this::configure);
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

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
