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
        noUpdateDisabled = true;
        displayFlow = false;

        config(Liquid.class, (LiquidSourceBuild tile, Liquid l) -> tile.source = l);
        configClear((LiquidSourceBuild tile) -> tile.source = null);
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

    public class LiquidSourceBuild extends Building{
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

            if(source == null){
                Draw.rect("cross", x, y);
            }else{
                Draw.color(source.color);
                Draw.rect("center", x, y);
                Draw.color();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.liquids(), () -> source, this::configure);
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
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(source == null ? -1 : source.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int id = revision == 1 ? read.s() : read.b();
            source = id == -1 ? null : content.liquid(id);
        }
    }
}
