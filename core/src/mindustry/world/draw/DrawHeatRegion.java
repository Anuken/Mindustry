package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.HeatCrafter.*;

/** Not standalone. */
public class DrawHeatRegion extends DrawBlock{
    public Color heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
    public float heatPulse = 0.3f, heatPulseScl = 10f;

    public TextureRegion heat;
    public String suffix = "-glow";

    public DrawHeatRegion(String suffix){
        this.suffix = suffix;
    }

    public DrawHeatRegion(){
    }

    @Override
    public void drawBase(Building build){

        Draw.z(Layer.blockAdditive);
        if(build instanceof HeatCrafterBuild hc && hc.heat > 0){
            Draw.blend(Blending.additive);
            Draw.color(heatColor, Mathf.clamp(hc.heat / hc.heatRequirement()) * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse))));
            Draw.rect(heat, build.x, build.y);
            Draw.blend();
            Draw.color();
        }
        Draw.z(Layer.block);

    }

    @Override
    public void load(Block block){
        heat = Core.atlas.find(block.name + suffix);
    }
}
