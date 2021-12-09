package mindustry.world.blocks.heat;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;

public class HeatConductor extends Block{
    public float visualMaxHeat = 10f;
    public DrawBlock drawer = new DrawBlock();

    public HeatConductor(String name){
        super(name);
        update = solid = rotate = true;
        size = 3;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("heat", (HeatConductorBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat / visualMaxHeat));
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public void drawRequestRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    public class HeatConductorBuild extends Building implements HeatBlock{
        public float heat = 0f;

        @Override
        public void draw(){
            drawer.drawBase(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLights(this);
        }

        @Override
        public void updateTile(){
            heat = calculateHeat(null);
        }

        @Override
        public float warmup(){
            return heat;
        }

        @Override
        public float heat(){
            return heat;
        }

        @Override
        public float heatFrac(){
            return heat / visualMaxHeat;
        }
    }
}
