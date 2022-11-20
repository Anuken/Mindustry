package mindustry.world.draw;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.Block;
import mindustry.world.blocks.defense.ForceProjector.*;

public class DrawForceHeat extends DrawBlock{
    public TextureRegion heat;
    public Color color;
    public String suffix = "-top";

    @Override
    public void draw(Building build){
        if(!(build instanceof ForceBuild proj) || proj.buildup > 0f) return;

        Draw.color(color);
        Draw.alpha(/*proj.buildup / proj.shieldHealth * 0.75f*/0f);
        Draw.z(Layer.blockAdditive);
        Draw.blend(Blending.additive);
        Draw.rect(heat, proj.x, proj.y);
        Draw.blend();
        Draw.z(Layer.block);
        Draw.reset();
    }

    @Override
    public void load(Block block){
        heat = Core.atlas.find(block.name + suffix);
    }
}
