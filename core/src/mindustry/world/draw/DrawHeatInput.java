package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.blocks.production.HeatCrafter.*;

/** Not standalone. */
public class DrawHeatInput extends DrawBlock{
    public Color heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
    public float heatPulse = 0.3f, heatPulseScl = 10f;

    public TextureRegion heat;

    @Override
    public void draw(GenericCrafterBuild build){

        Draw.z(Layer.blockAdditive);
        if(build instanceof HeatCrafterBuild hc){
            for(int i = 0; i < 4; i++){
                if(hc.sideHeat[i] > 0){
                    Draw.blend(Blending.additive);
                    Draw.color(heatColor, hc.sideHeat[i] / hc.heatRequirement() * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse))));
                    Draw.rect(heat, build.x, build.y, i * 90f);
                    Draw.blend();
                    Draw.color();
                }
            }
        }
        Draw.z(Layer.block);

    }

    @Override
    public void load(Block block){
        heat = Core.atlas.find(block.name + "-heat");
    }
}
