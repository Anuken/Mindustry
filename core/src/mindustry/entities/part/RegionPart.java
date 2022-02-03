package mindustry.entities.part;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;

public class RegionPart extends WeaponPart{
    public String suffix = "";
    public TextureRegion heat;
    public TextureRegion[] regions = {};
    public TextureRegion[] outlines = {};

    /** If true, turret reload is used as the measure of progress. Otherwise, warmup is used. */
    public boolean useReload = true;
    /** If true, parts are mirrored across the turret. Requires -1 and -2 regions. */
    public boolean mirror = false;
    /** If true, an outline is drawn under the part. */
    public boolean outline = true;
    /** If true, the base + outline regions are drawn. Set to false for heat-only regions. */
    public boolean drawRegion = true;
    /** If true, progress is inverted. */
    public boolean invert = false;
    public Blending blending = Blending.normal;
    public boolean useProgressHeat = false;
    public Interp interp = Interp.linear;
    public float layer = -1;
    public float outlineLayerOffset = -0.01f;
    public float rotation, rotMove;
    public float x, y, moveX, moveY;
    public float oscMag = 0f, oscScl = 7f;
    public boolean oscAbs = false;
    public @Nullable Color color, colorTo;
    public Color heatColor = Pal.turretHeat.cpy();

    public RegionPart(String region){
        this.suffix = region;
    }

    public RegionPart(){
    }

    @Override
    public void draw(PartParams params){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        //TODO 'under' should not be special cased like this...
        if(under && turretShading) Draw.z(z - 0.0001f);

        float prevZ = Draw.z();
        float progress = useReload ? 1f - params.reload : params.warmup;

        if(oscMag > 0) progress += oscAbs ? Mathf.absin(oscScl, oscMag) : Mathf.sin(oscScl, oscMag);
        if(invert) progress = 1f - progress;

        progress = interp.apply(progress);
        int len = mirror ? 2 : 1;

        for(int i = 0; i < len; i++){
            //can be null
            var region = drawRegion ? regions[Math.min(i, regions.length - 1)] : null;
            float sign = i == 1 ? -1 : 1;
            Tmp.v1.set((x + moveX * progress) * sign, y + moveY * progress).rotate((params.rotation - 90));

            float
                rx = params.x + Tmp.v1.x,
                ry = params.y + Tmp.v1.y,
                rot = i * sign + rotMove * progress * sign + params.rotation - 90;

            Draw.xscl = i == 0 ? 1 : -1;

            if(outline && drawRegion){
                Draw.z(prevZ + outlineLayerOffset);
                Draw.rect(outlines[Math.min(i, regions.length - 1)], rx, ry, rot);
                Draw.z(prevZ);
            }

            if(drawRegion && region.found()){
                if(color != null && colorTo != null){
                    Draw.color(color, colorTo, progress);
                }else if(color != null){
                    Draw.color(color);
                }
                Draw.blend(blending);
                Draw.rect(region, rx, ry, rot);
                Draw.blend();
                if(color != null) Draw.color();
            }

            if(heat.found()){
                Drawf.additive(heat, heatColor.write(Tmp.c1).a((useProgressHeat ? params.warmup : params.heat) * heatColor.a), rx, ry, rot, turretShading ? Layer.turretHeat : z + 1f);
            }

            Draw.xscl = 1f;
        }

        Draw.z(z);
    }

    @Override
    public void load(String name){
        if(drawRegion){
            //TODO l/r
            if(mirror && turretShading){
                regions = new TextureRegion[]{
                Core.atlas.find(name + suffix + "1"),
                Core.atlas.find(name + suffix + "2")
                };

                outlines = new TextureRegion[]{
                Core.atlas.find(name + suffix + "1-outline"),
                Core.atlas.find(name + suffix + "2-outline")
                };
            }else{
                regions = new TextureRegion[]{Core.atlas.find(name + suffix)};
                outlines = new TextureRegion[]{Core.atlas.find(name + suffix + "-outline")};
            }
        }

        heat = Core.atlas.find(name + suffix + "-heat");
    }

    @Override
    public void getOutlines(Seq<TextureRegion> out){
        if(outline && drawRegion){
            out.addAll(regions);
        }
    }
}