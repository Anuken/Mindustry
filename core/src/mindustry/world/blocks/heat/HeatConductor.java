package mindustry.world.blocks.heat;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;

public class HeatConductor extends Block{
    public float visualMaxHeat = 15f;
    public DrawBlock drawer = new DrawDefault();
    public boolean splitHeat = false;

    public HeatConductor(String name){
        super(name);
        update = solid = rotate = true;
        rotateDraw = false;
        size = 3;
        noUpdateDisabled = true;
        hasHeat = true;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("heat", (HeatConductorBuild entity) ->
            new Bar(
                () -> Core.bundle.format("bar.heatamount", Strings.autoFixed(entity.heatModule.output + 0.001f, 1)),
                () -> Pal.lightOrange, () -> entity.heatModule.output / visualMaxHeat));
    }

    @Override
    public boolean emitsHeat(){
        return true;
    }

    @Override
    public float heatInputMax(){
        return visualMaxHeat;
    }

    @Override
    public float heatOutputMax(){
        return visualMaxHeat * (splitHeat ? 3f : 1f);
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    public class HeatConductorBuild extends Building{
        public IntSet cameFrom = new IntSet();
        public long lastHeatUpdate = -1;

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public void updateTile(){
            updateHeat();
        }

        public void updateHeat(){
            if(lastHeatUpdate == Vars.state.updateId) return;

            lastHeatUpdate = Vars.state.updateId;
            heatModule.input = enabled ? calculateHeat(heatModule.sides, cameFrom) : 0f;
            heatModule.output = heatModule.input;
        }

        @Override
        public float warmup(){
            return heatModule.output;
        }
    }
}
