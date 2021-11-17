package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

/** Extend to implement custom drawing behavior for a turret. */
public class DrawTurret extends DrawBlock{
    protected static final Rand rand = new Rand();

    public String basePrefix = "";
    public TextureRegion base, liquid, top, heat;

    public DrawTurret(String basePrefix){
        this.basePrefix = basePrefix;
    }

    public DrawTurret(){
    }

    @Override
    public void drawBase(Building build){
        Turret turret = (Turret)build.block;
        TurretBuild tb = (TurretBuild)build;

        Draw.rect(base, build.x, build.y);
        Draw.color();

        Draw.z(Layer.turret);

        Drawf.shadow(build.block.region, build.x + tb.recoilOffset.x - turret.elevation, build.y + tb.recoilOffset.y - turret.elevation, tb.drawrot());

        drawTurret(turret, tb);
        drawHeat(turret, tb);
    }

    public void drawTurret(Turret block, TurretBuild build){
        Draw.rect(block.region, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());

        if(liquid.found()){
            Drawf.liquid(liquid, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.liquids.currentAmount() / block.liquidCapacity, build.liquids.current().color, build.drawrot());
        }

        if(top.found()){
            Draw.rect(top, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());
        }
    }

    public void drawHeat(Turret block, TurretBuild build){
        if(build.heat <= 0.00001f || !heat.found()) return;

        Draw.color(block.heatColor, build.heat);
        Draw.blend(Blending.additive);
        Draw.rect(heat, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());
        Draw.blend();
        Draw.color();
    }

    /** Load any relevant texture regions. */
    @Override
    public void load(Block block){
        if(!(block instanceof Turret)) throw new ClassCastException("This drawer can only be used on turrets.");

        liquid = Core.atlas.find(block.name + "-liquid");
        top = Core.atlas.find(block.name + "-top");
        heat = Core.atlas.find(block.name + "-heat");
        base = Core.atlas.find(block.name + "-base");

        //TODO test this for mods, e.g. exotic
        if(!base.found() && block.minfo.mod != null) base = Core.atlas.find(block.minfo.mod.name + "-block-" + block.size);
        if(!base.found()) base = Core.atlas.find(basePrefix + "block-" + block.size);
    }

    /** @return the generated icons to be used for this block. */
    @Override
    public TextureRegion[] icons(Block block){
        return top.found() ? new TextureRegion[]{base, block.region, top} : new TextureRegion[]{base, block.region};
    }
}
