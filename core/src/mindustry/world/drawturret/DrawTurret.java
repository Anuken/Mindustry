package mindustry.world.drawturret;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

/** Extend to implement custom drawing behavior for a turret. */
public class DrawTurret{
    protected static final Rand rand = new Rand();

    /** Draws the block. */
    public void draw(Turret block, TurretBuild build){
        Draw.rect(block.region, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.rotation - 90);
    }

    public void drawHeat(Turret block, TurretBuild build){
        if(build.heat <= 0.00001f || !block.heatRegion.found()) return;

        Draw.color(block.heatColor, build.heat);
        Draw.blend(Blending.additive);
        Draw.rect(block.heatRegion, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.rotation - 90);
        Draw.blend();
        Draw.color();
    }

    /** Load any relevant texture regions. */
    public void load(Turret block){

    }

    /** @return the generated icons to be used for this block. */
    public TextureRegion[] icons(Turret block){
        return new TextureRegion[]{block.baseRegion, block.region};
    }
}
