package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

/** Extend to implement custom drawing behavior for a turret. */
public class DrawTurret extends DrawBlock{
    protected static final Rand rand = new Rand();

    public Seq<TurretPart> parts = new Seq<>();
    public String basePrefix = "";
    public TextureRegion base, liquid, top, heat, preview;

    public DrawTurret(String basePrefix){
        this.basePrefix = basePrefix;
    }

    public DrawTurret(){
    }

    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out){
        for(var part : parts){
            part.getOutlines(out);
        }
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

        if(parts.size > 0){
            for(var part : parts){
                part.draw(tb);
            }
        }
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

        preview = Core.atlas.find(block.name + "-preview", block.region);
        liquid = Core.atlas.find(block.name + "-liquid");
        top = Core.atlas.find(block.name + "-top");
        heat = Core.atlas.find(block.name + "-heat");
        base = Core.atlas.find(block.name + "-base");

        for(var part : parts){
            part.load(block);
        }

        //TODO test this for mods, e.g. exotic
        if(!base.found() && block.minfo.mod != null) base = Core.atlas.find(block.minfo.mod.name + "-block-" + block.size);
        if(!base.found()) base = Core.atlas.find(basePrefix + "block-" + block.size);
    }

    /** @return the generated icons to be used for this block. */
    @Override
    public TextureRegion[] icons(Block block){
        TextureRegion showTop = preview.found() ? preview : block.region;
        return top.found() ? new TextureRegion[]{base, showTop, top} : new TextureRegion[]{base, showTop};
    }

    public static class RegionPart extends TurretPart{
        public String suffix = "";
        public boolean mirror = true;
        public TextureRegion[] regions;
        public TextureRegion[] outlines;

        public boolean outline = false;
        public float layer = -1;
        public float outlineLayerOffset = -0.01f;
        public float rotation, rotMove;
        public float originX, originY;
        public float offsetX, offsetY, offsetMoveX, offsetMoveY;

        public RegionPart(String region){
            this.suffix = region;
        }

        public RegionPart(){
        }

        @Override
        public void draw(TurretBuild build){
            float z = Draw.z();
            if(layer > 0){
                Draw.z(layer);
            }
            float prevZ = layer > 0 ? layer : z;

            float progress = build.warmup();

            for(int i = 0; i < regions.length; i++){
                var region = regions[i];
                float sign = i == 1 ? -1 : 1;
                Tmp.v1.set((offsetX + offsetMoveX * progress) * sign, offsetY + offsetMoveY*progress).rotate(build.rotation - 90);

                float
                    x = build.x + Tmp.v1.x,
                    y = build.y + Tmp.v1.y,
                    rot = (i == 0 ? rotation : 180f - rotation) + rotMove * progress * sign + build.rotation,
                    ox = originX + region.width * Draw.scl/2f, oy = originY + region.height * Draw.scl/2f;

                if(outline){
                    Draw.z(prevZ + outlineLayerOffset);

                    Draw.rect(outlines[i],
                    x, y, region.width * Draw.scl, region.height * Draw.scl,
                    ox, oy, rot);

                    Draw.z(prevZ);
                }

                Draw.rect(region,
                    x, y, region.width * Draw.scl, region.height * Draw.scl,
                    ox, oy, rot);
            }

            Draw.z(z);
        }

        @Override
        public void load(Block block){
            if(mirror){
                regions = new TextureRegion[]{
                    Core.atlas.find(block.name + suffix + "1"),
                    Core.atlas.find(block.name + suffix + "2")
                };

                outlines = new TextureRegion[]{
                    Core.atlas.find(block.name + suffix + "1-outline"),
                    Core.atlas.find(block.name + suffix + "2-outline")
                };
            }else{
                regions = new TextureRegion[]{Core.atlas.find(block.name + suffix)};
                outlines = new TextureRegion[]{Core.atlas.find(block.name + suffix + "-outline")};
            }
        }

        @Override
        public void getOutlines(Seq<TextureRegion> out){
            if(outline){
                out.addAll(regions);
            }
        }
    }

    public static abstract class TurretPart{
        public abstract void draw(TurretBuild build);
        public abstract void load(Block block);
        public void getOutlines(Seq<TextureRegion> out){}
    }
}
