package mindustry.entities.part;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;

import static arc.util.Tmp.*;

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
    /** If true, the heat region produces light. */
    public boolean heatLight = false;
    /** Progress function for determining position/rotation. */
    public PartProgress progress = PartProgress.warmup;
    /** Progress function for scaling. */
    public PartProgress growProgress = PartProgress.warmup;
    /** Progress function for heat alpha. */
    public PartProgress heatProgress = PartProgress.heat;
    public Blending blending = Blending.normal;
    public float layer = -1, layerOffset = 0f, heatLayerOffset = 1f, turretHeatLayer = Layer.turretHeat;
    public float outlineLayerOffset = -0.001f;
    public float x, y, xScl = 1f, yScl = 1f, rotation;
    public float moveX, moveY, growX, growY, moveRot;
    public float heatLightOpacity = 0.3f;
    public @Nullable Color color, colorTo, mixColor, mixColorTo;
    public Color heatColor = Pal.turretHeat.cpy();
    public Seq<DrawPart> children = new Seq<>();
    public Seq<PartMove> moves = new Seq<>();

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
        float prog = progress.getClamp(params);
        v31.set(getMoved());
        v1.set(getScaled());

        int len = mirror && params.sideOverride == -1 ? 2 : 1;
        float preXscl = Draw.xscl, preYscl = Draw.yscl;
        Draw.xscl *= xScl + v1.x;
        Draw.yscl *= yScl + v1.y;

        for(int s = 0; s < len; s++){
            //use specific side if necessary
            int i = params.sideOverride == -1 ? s : params.sideOverride;

            //can be null
            var region = drawRegion ? regions[Math.min(i, regions.length - 1)] : null;
            float sign = (i == 0 ? 1 : -1) * params.sideMultiplier;
            v1.set(v31.x * sign, v31.y).rotateRadExact((params.rotation - 90) * Mathf.degRad);

            float
                rx = params.x + v1.x,
                ry = params.y + v1.y,
                rot = v31.z * sign + params.rotation - 90;

            Draw.xscl *= sign;

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

                if(mixColor != null && mixColorTo != null){
                    Draw.mixcol(mixColor, mixColorTo, prog);
                }else if(mixColor != null){
                    Draw.mixcol(mixColor, mixColor.a);
                }

                Draw.blend(blending);
                Draw.rect(region, rx, ry, rot);
                Draw.blend();
                if(color != null) Draw.color();
            }

            if(heat.found()){
                float hprog = heatProgress.getClamp(params);
                heatColor.write(c1).a(hprog * heatColor.a);
                Drawf.additive(heat, c1, rx, ry, rot, turretShading ? turretHeatLayer : Draw.z() + heatLayerOffset);
                if(heatLight) Drawf.light(rx, ry, heat, rot, c1, heatLightOpacity * hprog);
            }

            Draw.xscl *= sign;
        }

        Draw.color();
        Draw.mixcol();

        Draw.z(z);

        //draw child, if applicable - only at the end
        //TODO lots of copy-paste here
        if(children.size > 0){
            for(int s = 0; s < len; s++){
                int i = (params.sideOverride == -1 ? s : params.sideOverride);
                float sign = (i == 1 ? -1 : 1) * params.sideMultiplier;
                v1.set(v31.x * sign, v31.y).rotateRadExact((params.rotation - 90) * Mathf.degRad);

                childParam.sideMultiplier = params.sideMultiplier;
                childParam.life = params.life;
                childParam.sideOverride = i;
                DrawPart.params.draw(children, null, params.warmup, params.reload, params.smoothReload, params.heat, params.recoil, params.charge, params.x + v1.x, params.y + v1.y, v31.z * sign + params.rotation);
            }
        }

        Draw.scl(preXscl, preYscl);
    }

    @Override
    public void update(PartParams params){
        if(children.size == 0) return;

        v31.set(getMoved());
        int len = mirror && params.sideOverride == -1 ? 2 : 1;
        for(int s = 0; s < len; s++){
            int i = (params.sideOverride == -1 ? s : params.sideOverride);
            float sign = (i == 1 ? -1 : 1) * params.sideMultiplier;

            childParam.sideMultiplier = params.sideMultiplier;
            childParam.life = params.life;
            childParam.sideOverride = i;

            v1.set(v31.x * sign, v31.y).rotateRadExact((params.rotation - 90) * Mathf.degRad);
            DrawPart.params.update(children, null, params.warmup, params.reload, params.smoothReload, params.heat, params.recoil, params.charge, params.x + v1.x, params.y + v1.y, v31.z * sign + params.rotation);
        }
    }

    public Vec3 getMoved(){
        float prog = progress.getClamp(params);
        v31.set(x + moveX * prog, y + moveY * prog, rotation + moveRot * prog);

        if(moves.size > 0){
            for(int i = 0; i < moves.size; i++){
                var move = moves.get(i);
                float p = move.progress.getClamp(params);
                v31.add(move.x * p, move.y * p, move.rot * p);
            }
        }

        return v31;
    }

    public Vec2 getScaled(){
        float sclProg = growProgress.getClamp(params);
        v1.set(growX * sclProg, growY * sclProg);

        if(moves.size > 0){
            for(int i = 0; i < moves.size; i++){
                var move = moves.get(i);
                float p = move.progress.getClamp(params);
                v1.add(move.gx * p, move.gy * p);
            }
        }

        return v1;
    }

    @Override
    public void load(String name){
        String realName = this.name == null ? name + suffix : this.name;

        if(drawRegion){
            if(mirror && turretShading){
                regions = new TextureRegion[]{
                Core.atlas.find(realName + "-r"),
                Core.atlas.find(realName + "-l")
                };

                outlines = new TextureRegion[]{
                Core.atlas.find(realName + "-r-outline"),
                Core.atlas.find(realName + "-l-outline")
                };
            }else{
                regions = new TextureRegion[]{Core.atlas.find(realName)};
                outlines = new TextureRegion[]{Core.atlas.find(realName + "-outline")};
            }
        }

        heat = Core.atlas.find(realName + "-heat");
        for(var child : children){
            child.turretShading = turretShading;
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
