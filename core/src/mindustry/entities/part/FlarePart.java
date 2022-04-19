package mindustry.entities.part;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.graphics.*;

public class FlarePart extends DrawPart{
    public int sides = 4;
    public float radius = 100f, radiusTo = -1f, stroke = 6f, innerScl = 0.5f, innerRadScl = 0.33f;
    public float x, y, rotation, rotMove;
    public boolean followRotation;
    public Color color1 = Pal.techBlue, color2 = Color.white;
    public PartProgress progress = PartProgress.warmup;
    public float layer = Layer.effect;

    @Override
    public void draw(PartParams params){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);

        float prog = progress.getClamp(params);
        int i = params.sideOverride == -1 ? 0 : params.sideOverride;

        float sign = (i == 0 ? 1 : -1) * params.sideMultiplier;
        Tmp.v1.set(x * sign, y).rotate(params.rotation - 90);

        float
        rx = params.x + Tmp.v1.x,
        ry = params.y + Tmp.v1.y,
        rot = (followRotation ? params.rotation : 0f) + rotMove * prog + rotation,
        rad = radiusTo < 0 ? radius : Mathf.lerp(radius, radiusTo, prog);

        Draw.color(color1);
        for(int j = 0; j < sides; j++){
            Drawf.tri(rx, ry, stroke, rad, j * 360f / sides + rot);
        }

        Draw.color(color2);
        for(int j = 0; j < sides; j++){
            Drawf.tri(rx, ry, stroke * innerScl, rad * innerRadScl, j * 360f / sides + rot);
        }

        Draw.color();
        Draw.z(z);
    }

    @Override
    public void load(String name){

    }
}
