package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.HeatCrafter.*;

public class DrawHeatRegion extends DrawBlock{
    public Color color = new Color(1f, 0.22f, 0.22f, 0.8f);
    public float pulse = 0.3f, pulseScl = 10f;

    public TextureRegion heat;
    public String suffix = "-glow";

    public DrawHeatRegion(String suffix){
        this.suffix = suffix;
    }

    public DrawHeatRegion(){
    }

    @Override
    public void draw(Building build){
        Draw.z(Layer.blockAdditive);
        if(build instanceof HeatCrafterBuild hc && hc.heat > 0){
            Draw.blend(Blending.additive);
            Draw.color(color, Mathf.clamp(hc.heat / hc.heatRequirement()) * (color.a * (1f - pulse + Mathf.absin(pulseScl, pulse))));
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
