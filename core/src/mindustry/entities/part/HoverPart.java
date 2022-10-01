package mindustry.entities.part;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;

public class HoverPart extends DrawPart{
    public float radius = 4f;
    public float x, y, rotation, phase = 50f, stroke = 3f, minStroke = 0.12f;
    public int circles = 2;
    public Color color = Color.white;
    public boolean mirror = false;
    public float layer = -1f, layerOffset = 0f;

    @Override
    public void draw(PartParams params){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        if(under && turretShading) Draw.z(z - 0.0001f);

        Draw.z(Draw.z() + layerOffset);

        int len = mirror && params.sideOverride == -1 ? 2 : 1;

        Draw.color(color);


        for(int c = 0; c < circles; c++){
            float fin = ((Time.time / phase + (float)c / circles) % 1f);
            Lines.stroke((1f-fin) * stroke + minStroke);

            for(int s = 0; s < len; s++){
                //use specific side if necessary
                int i = params.sideOverride == -1 ? s : params.sideOverride;

                float sign = (i == 0 ? 1 : -1) * params.sideMultiplier;
                Tmp.v1.set((x) * sign, y).rotate(params.rotation - 90);

                float
                rx = params.x + Tmp.v1.x,
                ry = params.y + Tmp.v1.y;

                Lines.square(rx, ry, radius * fin, params.rotation - 45f);
            }
        }

        Draw.reset();

        Draw.z(z);
    }

    @Override
    public void load(String name){

    }
}
