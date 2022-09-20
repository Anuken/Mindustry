package mindustry.entities.part;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.graphics.*;

public class HaloPart extends DrawPart{
    public boolean hollow = false, tri = false;
    public int shapes = 3;
    public int sides = 3;
    public float radius = 3f, radiusTo = -1f, stroke = 1f, strokeTo = -1f;
    public float triLength = 1f, triLengthTo = -1f;
    public float haloRadius = 10f, haloRadiusTo = -1f;
    public float x, y, shapeRotation;
    public float moveX, moveY, shapeMoveRot;
    public float haloRotateSpeed = 0f, haloRotation = 0f;
    public float rotateSpeed = 0f;
    public Color color = Color.white;
    public @Nullable Color colorTo;
    public boolean mirror = false;
    public PartProgress progress = PartProgress.warmup;
    public float layer = -1f, layerOffset = 0f;

    @Override
    public void draw(PartParams params){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        if(under && turretShading) Draw.z(z - 0.0001f);

        Draw.z(Draw.z() + layerOffset);

        float
        prog = progress.getClamp(params),
        baseRot = Time.time * rotateSpeed,
        rad = radiusTo < 0 ? radius : Mathf.lerp(radius, radiusTo, prog),
        triLen = triLengthTo < 0 ? triLength : Mathf.lerp(triLength, triLengthTo, prog),
        str = strokeTo < 0 ? stroke : Mathf.lerp(stroke, strokeTo, prog),
        haloRad = haloRadiusTo < 0 ? haloRadius : Mathf.lerp(haloRadius, haloRadiusTo, prog);

        int len = mirror && params.sideOverride == -1 ? 2 : 1;

        for(int s = 0; s < len; s++){
            //use specific side if necessary
            int i = params.sideOverride == -1 ? s : params.sideOverride;

            float sign = (i == 0 ? 1 : -1) * params.sideMultiplier;
            Tmp.v1.set((x + moveX * prog) * sign, y + moveY * prog).rotate(params.rotation - 90);

            float
            rx = params.x + Tmp.v1.x,
            ry = params.y + Tmp.v1.y;

            if(color != null && colorTo != null){
                Draw.color(color, colorTo, prog);
            }else if(color != null){
                Draw.color(color);
            }

            float haloRot = (haloRotation + haloRotateSpeed * Time.time) * sign;

            for(int v = 0; v < shapes; v++){
                float rot = haloRot + v * 360f / shapes + params.rotation;
                float shapeX = Angles.trnsx(rot, haloRad) + rx, shapeY = Angles.trnsy(rot, haloRad) + ry;
                float pointRot = rot + shapeMoveRot * prog * sign + shapeRotation * sign + baseRot * sign;

                if(tri){
                    if(rad > 0.001 && triLen > 0.001){
                        Drawf.tri(shapeX, shapeY, rad, triLen, pointRot);
                    }
                }else if(!hollow){
                    if(rad > 0.001){
                        Fill.poly(shapeX, shapeY, sides, rad, pointRot);
                    }
                }else if(str > 0.001){
                    Lines.stroke(str);
                    Lines.poly(shapeX, shapeY, sides, rad, pointRot);
                    Lines.stroke(1f);
                }
            }

            if(color != null) Draw.color();
        }

        Draw.z(z);
    }

    @Override
    public void load(String name){

    }
}
