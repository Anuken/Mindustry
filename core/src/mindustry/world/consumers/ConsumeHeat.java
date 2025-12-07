package mindustry.world.consumers;

import arc.*;
import arc.math.Mathf;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

/** Calories-burning consumption type */
public class ConsumeHeat extends Consume{
    public float heatRequirement;
    public float overheatScale = 1f;
    public float maxEfficiency = 4f;
    public boolean optional = false;

    public ConsumeHeat(float heatRequirement){
        this.heatRequirement = heatRequirement;
    }

    public ConsumeHeat optional(float overheatScale, float maxEfficiency){
        this.optional = true;
        this.overheatScale = overheatScale;
        this.maxEfficiency = maxEfficiency;
        return this;
    }

    @Override
    public void apply(Block block){
        // Ensure the block has heat-related methods
        if(!(block instanceof HeatConsumer)){
            throw new IllegalArgumentException("Blocks that use ConsumeHeat must implement the HeatConsumer interface");
        }
    }

    @Override
    public void build(Building build, Table table){
        // Display calorie information in the UI
        table.table(t -> {
            t.left();
            t.add(Core.bundle.get("consumes.heat") + ": ").color(Pal.lightOrange);
            t.add("[lightgray]" + (int)heatRequirement + " heat units").padLeft(4f);
        }).left().padTop(4f);
    }

    @Override
    public void update(Building build){
        // If the building implements HeatConsumer, update the heat
        HeatConsumer heatBuild = (HeatConsumer) build;
        // Heat calculation logic can be added here
    }

    @Override
    public float efficiency(Building build){
        if(build instanceof HeatConsumer heatBuild){
            float[] sideHeat = heatBuild.sideHeat();
            float heat = calculateHeat(sideHeat);

            // Computational efficiency
            float over = Math.max(heat - heatRequirement, 0f);
            return Math.min(
                    Mathf.clamp(heat / heatRequirement) + over / heatRequirement * overheatScale,
                    maxEfficiency
            );
        }
        return 0f;
    }

    @Override
    public void display(Stats stats){
        stats.add(Stat.input, heatRequirement, StatUnit.heatUnits);
        if(optional){
            stats.add(Stat.maxEfficiency, (int)(maxEfficiency * 100f), StatUnit.percent);
        }
    }

    /** Calculate the heat received from surrounding blocks */
    private float calculateHeat(float[] sideHeat){
        float total = 0f;
        for(float heat : sideHeat){
            total += heat;
        }
        return total;
    }
}