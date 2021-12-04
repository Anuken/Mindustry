package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

/** Extend to implement custom drawing behavior for a turret. */
public class DrawTurret extends DrawBlock{
    protected static final Rand rand = new Rand();

    public Seq<TurretPart> parts = new Seq<>();
    public String basePrefix = "";
    /** Overrides the liquid to draw in the liquid region. */
    public @Nullable Liquid liquidDraw;
    public TextureRegion base, liquid, top, heat, preview, outline;

    public DrawTurret(String basePrefix){
        this.basePrefix = basePrefix;
    }

    public DrawTurret(){
    }

    @Override
    public void getRegionsToOutline(Block block, Seq<TextureRegion> out){
        for(var part : parts){
            part.getOutlines(out);
        }
        if(preview.found()){
            out.add(block.region);
        }
    }

    @Override
    public void drawBase(Building build){
        Turret turret = (Turret)build.block;
        TurretBuild tb = (TurretBuild)build;

        Draw.rect(base, build.x, build.y);
        Draw.color();

        Draw.z(Layer.turret - 0.02f);

        Drawf.shadow(preview, build.x + tb.recoilOffset.x - turret.elevation, build.y + tb.recoilOffset.y - turret.elevation, tb.drawrot());

        Draw.z(Layer.turret);

        drawTurret(turret, tb);
        drawHeat(turret, tb);

        if(parts.size > 0){
            if(outline.found()){
                Draw.z(Layer.turret - 0.01f);
                Draw.rect(outline, build.x + tb.recoilOffset.x, build.y + tb.recoilOffset.y, tb.drawrot());
                Draw.z(Layer.turret);
            }

            for(var part : parts){
                part.draw(tb);
            }
        }
    }

    public void drawTurret(Turret block, TurretBuild build){
        Draw.rect(block.region, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());

        if(liquid.found()){
            Liquid toDraw = liquidDraw == null ? build.liquids.current() : liquidDraw;
            Drawf.liquid(liquid, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.liquids.get(toDraw) / block.liquidCapacity, toDraw.color.write(Tmp.c1).a(1f), build.drawrot());
        }

        if(top.found()){
            Draw.rect(top, build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot());
        }
    }

    public void drawHeat(Turret block, TurretBuild build){
        if(build.heat <= 0.00001f || !heat.found()) return;

        Drawf.additive(heat, block.heatColor.write(Tmp.c1).a(build.heat), build.x + build.recoilOffset.x, build.y + build.recoilOffset.y, build.drawrot(), Layer.turretHeat);
    }

    /** Load any relevant texture regions. */
    @Override
    public void load(Block block){
        if(!(block instanceof Turret)) throw new ClassCastException("This drawer can only be used on turrets.");

        preview = Core.atlas.find(block.name + "-preview", block.region);
        outline = Core.atlas.find(block.name + "-outline");
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
        public TextureRegion heat;
        public TextureRegion[] regions;
        public TextureRegion[] outlines;

        /** If true, turret reload is used as the measure of progress. Otherwise, warmup is used. */
        public boolean useReload = true;
        /** If true, parts are mirrored across the turret. Requires -1 and -2 regions. */
        public boolean mirror = true;
        /** If true, an outline is drawn under the part. */
        public boolean outline = true;
        /** If true, the layer is overridden to be under the turret itself. */
        public boolean under = false;
        /** If true, the base + outline regions are drawn. Set to false for heat-only regions. */
        public boolean drawRegion = true;
        /** If true, progress is inverted. */
        public boolean invert = false;
        public boolean useProgressHeat = false;
        public Interp interp = Interp.linear;
        public float layer = -1;
        public float outlineLayerOffset = -0.01f;
        public float rotation, rotMove;
        public float x, y, moveX, moveY;
        public float oscMag = 0f, oscScl = 7f;
        public boolean oscAbs = false;
        public Color heatColor = Pal.turretHeat.cpy();

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

            float progress = useReload ? 1f - build.progress() : build.warmup();

            if(oscMag > 0) progress += oscAbs ? Mathf.absin(oscScl, oscMag) : Mathf.sin(oscScl, oscMag);
            if(invert) progress = 1f - progress;

            progress = interp.apply(progress);

            for(int i = 0; i < regions.length; i++){
                var region = regions[i];
                float sign = i == 1 ? -1 : 1;
                Tmp.v1.set((x + moveX * progress) * sign, y + moveY * progress).rotate((build.rotation - 90));

                float
                    rx = build.x + Tmp.v1.x + build.recoilOffset.x,
                    ry = build.y + Tmp.v1.y + build.recoilOffset.y,
                    rot = i * sign + rotMove * progress * sign + build.rotation - 90;

                Draw.xscl = i == 0 ? 1 : -1;

                if(outline && drawRegion){
                    Draw.z(prevZ + outlineLayerOffset);
                    Draw.rect(outlines[i], rx, ry, rot);
                    Draw.z(prevZ);
                }

                if(drawRegion && region.found()){
                    Draw.rect(region, rx, ry, rot);
                }

                if(heat.found()){
                    Drawf.additive(heat, heatColor.write(Tmp.c1).a(useProgressHeat ? build.warmup() : build.heat), rx, ry, rot, Layer.turretHeat);
                }

                Draw.xscl = 1f;
            }

            Draw.z(z);
        }

        @Override
        public void load(Block block){
            if(under) layer = Layer.turret - 0.0001f;

            if(drawRegion){
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

            heat = Core.atlas.find(block.name + suffix + "-heat");
        }

        @Override
        public void getOutlines(Seq<TextureRegion> out){
            if(outline && drawRegion){
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
