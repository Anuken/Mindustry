package mindustry.entities.part;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;

public class ShapePart extends DrawPart{
    public boolean circle = false, hollow = false;
    public int sides = 3;
    public float radius = 3f, radiusTo = -1f, stroke = 1f, strokeTo = -1f;
    public float x, y, rotation;
    public float moveX, moveY, moveRot;
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

        float prog = progress.getClamp(params);
        int len = mirror && params.sideOverride == -1 ? 2 : 1;

        for(int s = 0; s < len; s++){
            //use specific side if necessary
            int i = params.sideOverride == -1 ? s : params.sideOverride;

            float sign = (i == 0 ? 1 : -1) * params.sideMultiplier;
            Tmp.v1.set((x + moveX * prog) * sign, y + moveY * prog).rotate(params.rotation - 90);

            float
            rx = params.x + Tmp.v1.x,
            ry = params.y + Tmp.v1.y,
            rad = radiusTo < 0 ? radius : Mathf.lerp(radius, radiusTo, prog),
            str = strokeTo < 0 ? stroke : Mathf.lerp(stroke, strokeTo, prog);

            if(color != null && colorTo != null){
                Draw.color(color, colorTo, prog);
            }else if(color != null){
                Draw.color(color);
            }
            
            if(!hollow){
                if(!circle){
                    Fill.poly(rx, ry, sides, rad, moveRot * prog * sign + params.rotation - 90 * sign + rotation * sign);
                }else{
                    Fill.circle(rx, ry, rad);
                }
            }else{
                Lines.stroke(str);
                if(!circle){
                    Lines.poly(rx, ry, sides, rad, moveRot * prog * sign + params.rotation - 90 * sign + rotation * sign);
                }else{
                    Lines.circle(rx, ry, rad);
                }
                Lines.stroke(1f);
            }
            if(color != null) Draw.color();
        }

        Draw.z(z);
    }

    @Override
    public void load(String name){

    }
}
