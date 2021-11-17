package mindustry.world.blocks.production;

import arc.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

import java.util.*;

/** A crafter that requires contact from heater blocks to craft. */
public class HeatCrafter extends GenericCrafter{
    /** Base heat requirement for 100% efficiency. */
    public float heatRequirement = 10f;
    /** After heat meets this requirement, excess heat will be scaled by this number. */
    public float overheatScale = 1f;
    /** Maximum possible efficiency after overheat. */
    public float maxEfficiency = 4f;

    public HeatCrafter(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("heat", (HeatCrafterBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.heatpercent", (int)entity.heat),
            () -> Pal.lightOrange,
            () -> entity.heat / heatRequirement));

        //TODO unnecessary?
        bars.add("efficiency", (HeatCrafterBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.efficiency", (int)(entity.efficiencyScale() * 100)),
            () -> Pal.ammo,
            entity::efficiencyScale));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.input, heatRequirement, StatUnit.heatUnits);
    }

    public class HeatCrafterBuild extends GenericCrafterBuild{
        //TODO sideHeat could be smooth
        public float[] sideHeat = new float[4];
        public float heat = 0f;

        @Override
        public void updateTile(){
            Arrays.fill(sideHeat, 0f);
            heat = 0f;

            for(var edge : getEdges()){
                Building build = nearby(edge.x, edge.y);
                if(build != null && build.team == team && build instanceof HeatBlock heater && (!build.block.rotate || (relativeTo(build) + 2) % 4 == build.rotation)){
                    //heat is distributed across building size
                    float add = heater.heat() / build.block.size;

                    sideHeat[Mathf.mod(relativeTo(build), 4)] += add;
                    heat += add;
                }
            }
            super.updateTile();
        }

        public float heatRequirement(){
            return heatRequirement;
        }

        @Override
        public float warmupTarget(){
            return Mathf.clamp(heat / heatRequirement);
        }

        @Override
        public float getProgressIncrease(float base){
            return super.getProgressIncrease(base) * efficiencyScale();
        }

        public float efficiencyScale(){
            float over = Math.max(heat - heatRequirement, 0f);
            return Math.min(Mathf.clamp(heat / heatRequirement) + over / heatRequirement * overheatScale, maxEfficiency);
        }
    }
}
