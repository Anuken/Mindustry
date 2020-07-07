package mindustry.world.blocks.sandbox;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.content;

public class LiquidSource extends Block{

    public LiquidSource(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        liquidCapacity = 100f;
        configurable = true;
        outputsLiquid = true;
        saveConfig = true;

        config(Liquid.class, (LiquidSourceEntity tile, Liquid l) -> tile.source = l);
        configClear((LiquidSourceEntity tile) -> tile.source = null);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.remove("liquid");
    }

    @Override
    public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
        drawRequestConfigCenter(req, req.config, "center");
    }

    public class LiquidSourceEntity extends Building{
        public @Nullable Liquid source = null;

        @Override
        public void updateTile(){
            if(source == null){
                liquids.clear();
            }else{
                liquids.add(source, liquidCapacity);
                dumpLiquid(source);
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(source != null){
                Draw.color(source.color);
                Draw.rect("center", x, y);
                Draw.color();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.liquids(), () -> source, liquid -> configure(liquid));
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
        public Liquid config(){
            return source;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(source == null ? -1 : source.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte id = read.b();
            source = id == -1 ? null : content.liquid(id);
        }
    }
}
