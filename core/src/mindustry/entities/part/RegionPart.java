package mindustry.entities.part;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;

public class RegionPart extends DrawPart{
    protected PartParams childParam = new PartParams();

    /** Appended to unit/weapon/block name and drawn. */
    public String suffix = "";
    /** Overrides suffix if set. */
    public @Nullable String name;
    public TextureRegion heat;
    public TextureRegion[] regions = {};
    public TextureRegion[] outlines = {};

    /** If true, parts are mirrored across the turret. Requires -1 and -2 regions. */
    public boolean mirror = false;
    /** If true, an outline is drawn under the part. */
    public boolean outline = true;
    /** If true, the base + outline regions are drawn. Set to false for heat-only regions. */
    public boolean drawRegion = true;
    /** Progress function for determining position/rotation. */
    public PartProgress progress = PartProgress.warmup;
    /** Progress function for heat alpha. */
    public PartProgress heatProgress = PartProgress.heat;
    public Blending blending = Blending.normal;
    public Interp interp = Interp.linear;
    public float layer = -1, layerOffset = 0f;
    public float outlineLayerOffset = -0.001f;
    public float rotation, rotMove;
    public float x, y, moveX, moveY;
    public @Nullable Color color, colorTo;
    public Color heatColor = Pal.turretHeat.cpy();
    public Seq<DrawPart> children = new Seq<>();

    public RegionPart(String region){
        this.suffix = region;
    }

    public RegionPart(String region, Blending blending, Color color){
        this.suffix = region;
        this.blending = blending;
        this.color = color;
        outline = false;
    }

    public RegionPart(){
    }

    @Override
    public void draw(PartParams params){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        //TODO 'under' should not be special cased like this...
        if(under && turretShading) Draw.z(z - 0.0001f);
        Draw.z(Draw.z() + layerOffset);

        float prevZ = Draw.z();
        float prog = progress.get(params);

        prog = interp.apply(prog);
        int len = mirror && params.sideOverride == -1 ? 2 : 1;

        for(int s = 0; s < len; s++){
            //use specific side if necessary
            int i = params.sideOverride == -1 ? s : params.sideOverride;

            //can be null
            var region = drawRegion ? regions[Math.min(i, regions.length - 1)] : null;
            float sign = i == 1 ? -1 : 1;
            Tmp.v1.set((x + moveX * prog) * sign, y + moveY * prog).rotate(params.rotation - 90);

            float
                rx = params.x + Tmp.v1.x,
                ry = params.y + Tmp.v1.y,
                rot = rotMove * prog * sign + params.rotation - 90;

            Draw.xscl = i == 0 ? 1 : -1;

            if(outline && drawRegion){
                Draw.z(prevZ + outlineLayerOffset);
                Draw.rect(outlines[Math.min(i, regions.length - 1)], rx, ry, rot);
                Draw.z(prevZ);
            }

            if(drawRegion && region.found()){
                if(color != null && colorTo != null){
                    Draw.color(color, colorTo, prog);
                }else if(color != null){
                    Draw.color(color);
                }
                Draw.blend(blending);
                Draw.rect(region, rx, ry, rot);
                Draw.blend();
                if(color != null) Draw.color();
            }

            if(heat.found()){
                Drawf.additive(heat, heatColor.write(Tmp.c1).a(heatProgress.get(params) * heatColor.a), rx, ry, rot, turretShading ? Layer.turretHeat : z + 1f);
            }

            Draw.xscl = 1f;
        }

        Draw.z(z);

        //draw child, if applicable - only at the end
        //TODO lots of copy-paste here
        if(children.size > 0){
            for(int s = 0; s < len; s++){
                int i = (params.sideOverride == -1 ? s : params.sideOverride);
                float sign = i == 1 ? -1 : 1;
                Tmp.v1.set((x + moveX * prog) * sign, y + moveY * prog).rotate(params.rotation - 90);

                childParam.set(params.warmup, params.reload, params.smoothReload, params.heat, params.x + Tmp.v1.x, params.y + Tmp.v1.y, i * sign + rotMove * prog * sign + params.rotation);
                childParam.sideOverride = i;
                for(var child : children){
                    child.draw(childParam);
                }
            }
        }
    }

    @Override
    public void load(String name){
        String realName = this.name == null ? name + suffix : this.name;

        if(drawRegion){
            //TODO l/r
            if(mirror && turretShading){
                regions = new TextureRegion[]{
                Core.atlas.find(realName + "1"),
                Core.atlas.find(realName + "2")
                };

                outlines = new TextureRegion[]{
                Core.atlas.find(realName + "1-outline"),
                Core.atlas.find(realName + "2-outline")
                };
            }else{
                regions = new TextureRegion[]{Core.atlas.find(realName)};
                outlines = new TextureRegion[]{Core.atlas.find(realName + "-outline")};
            }
        }

        heat = Core.atlas.find(realName + "-heat");
        for(var child : children){
            child.load(name);
        }
    }

    @Override
    public void getOutlines(Seq<TextureRegion> out){
        if(outline && drawRegion){
            out.addAll(regions);
        }
        for(var child : children){
            child.getOutlines(out);
        }
    }
}