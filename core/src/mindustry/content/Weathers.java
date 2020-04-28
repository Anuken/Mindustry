package mindustry.content;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.type.*;

import static mindustry.Vars.world;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow;

    @Override
    public void load(){
        snow = new Weather("snow"){
            Rand rand = new Rand();

            @Override
            public void draw(){
                rand.setSeed(0);
                float yspeed = 2f, xspeed = 0.25f;
                float padding = 16f;
                float size = 12f;
                Core.camera.bounds(Tmp.r1);
                Tmp.r1.grow(padding);

                for(int i = 0; i < 100; i++){
                    float scl = rand.random(0.5f, 1f);
                    float scl2 = rand.random(0.5f, 1f);
                    float sscl = rand.random(0.2f, 1f);
                    float x = (rand.random(0f, world.unitWidth()) + Time.time() * xspeed * scl2);
                    float y = (rand.random(0f, world.unitHeight()) - Time.time() * yspeed * scl);

                    x += Mathf.sin(y, rand.random(30f, 80f), rand.random(1f, 7f));

                    x -= Tmp.r1.x;
                    y -= Tmp.r1.y;
                    x = Mathf.mod(x, Tmp.r1.width);
                    y = Mathf.mod(y, Tmp.r1.height);
                    x += Tmp.r1.x;
                    y += Tmp.r1.y;

                    Draw.rect("circle-shadow", x, y, size * sscl, size * sscl);
                }
                //TODO
            }
        };
    }
}
