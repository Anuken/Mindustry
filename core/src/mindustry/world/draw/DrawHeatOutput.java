package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;

public class DrawHeatOutput extends DrawBlock{
    public TextureRegion heat, glow, top1, top2;

    public Color heatColor = new Color(1f, 0.22f, 0.22f, 0.8f);
    public float heatPulse = 0.3f, heatPulseScl = 10f, glowMult = 1.2f;
    public boolean drawRegion = false;

    @Override
    public void drawBase(Building build){
        if(drawRegion) Draw.rect(build.block.region, build.x, build.y);

        Draw.rect(build.rotation > 1 ? top2 : top1, build.x, build.y, build.rotdeg());

        if(build instanceof HeatBlock heater && heater.heat() > 0){
            Draw.z(Layer.blockAdditive);
            Draw.blend(Blending.additive);
            Draw.color(heatColor, heater.heatFrac() * (heatColor.a * (1f - heatPulse + Mathf.absin(heatPulseScl, heatPulse))));
            if(heat.found()) Draw.rect(heat, build.x, build.y, build.rotdeg());
            Draw.color(Draw.getColor().mul(glowMult));
            if(glow.found()) Draw.rect(glow, build.x, build.y);
            Draw.blend();
            Draw.color();
        }
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        if(drawRegion) Draw.rect(block.region, plan.drawx(), plan.drawy());
        Draw.rect(plan.rotation > 1 ? top2 : top1, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    @Override
    public void load(Block block){
        heat = Core.atlas.find(block.name + "-heat");
        glow = Core.atlas.find(block.name + "-glow");
        top1 = Core.atlas.find(block.name + "-top1");
        top2 = Core.atlas.find(block.name + "-top2");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region, top1};
    }

}
