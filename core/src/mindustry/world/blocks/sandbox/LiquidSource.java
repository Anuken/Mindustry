package mindustry.world.blocks.sandbox;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LiquidSource extends Block{
    public @Load("cross") TextureRegion crossRegion;
    public @Load("source-bottom") TextureRegion bottomRegion;

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
        group = BlockGroup.liquids;
        envEnabled = Env.any;
        clearOnDoubleTap = true;

        config(Liquid.class, (LiquidSourceBuild tile, Liquid l) -> tile.source = l);
        configClear((LiquidSourceBuild tile) -> tile.source = null);
    }

    @Override
    public void setBars(){
        super.setBars();

        removeBar("liquid");
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
        drawPlanConfigCenter(plan, plan.config, "center", true);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{bottomRegion, region};
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

            Draw.rect(bottomRegion, x, y);

            if(source == null){
                Draw.rect(crossRegion, x, y);
            }else{
                LiquidBlock.drawTiledFrames(size, x, y, 0f, source, 1f);
            }

            Draw.rect(block.region, x, y);
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(LiquidSource.this, table, content.liquids(), () -> source, this::configure);
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
