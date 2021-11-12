package mindustry.world.blocks.production;

import arc.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;

/** A crafter that requires contact from heater blocks to craft. */
public class HeatCrafter extends GenericCrafter{
    /** Base heat requirement for 100% efficiency. */
    public float heatRequirement = 10f;
    /** After heat meets this requirement, excess heat will be scaled by this number. */
    public float overheatScale = 0.25f;
    /** Maximum possible efficiency after overheat. */
    public float maxEfficiency = 2f;

    public HeatCrafter(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("heat", (AttributeCrafterBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.heatpercent", (int)entity.lastHeat),
            () -> Pal.lightOrange,
            () -> entity.lastHeat / heatRequirement));

        //TODO unnecessary?
        bars.add("efficiency", (AttributeCrafterBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.efficiency", (int)(entity.efficiencyScale() * 100)),
            () -> Pal.ammo,
            entity::efficiencyScale));
    }

    @Override
    public void setStats(){
        super.setStats();

        //TODO heat stats
    }

    public class AttributeCrafterBuild extends GenericCrafterBuild{
        public float lastHeat = 0f;

        @Override
        public void updateTile(){
            lastHeat = 0f;
            for(var edge : getEdges()){
                Building build = nearby(edge.x, edge.y);
                if(build != null && build.team == team && build instanceof HeatBlock heater && (!build.block.rotate || (relativeTo(build) + 2) % 4 == build.rotation)){
                    //heat is distributed across building size
                    lastHeat += heater.heat() / build.block.size;
                }
            }
            super.updateTile();
        }

        @Override
        public float getProgressIncrease(float base){
            return super.getProgressIncrease(base) * efficiencyScale();
        }

        public float efficiencyScale(){
            float over = Math.max(lastHeat - heatRequirement, 0f);
            return Math.min(Mathf.clamp(lastHeat / heatRequirement) + over / heatRequirement * overheatScale, maxEfficiency);
        }
    }
}
