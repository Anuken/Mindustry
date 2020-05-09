package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow;

    @Override
    public void load(){
        snow = new Weather("snow"){
            TextureRegion region;
            float yspeed = 2f, xspeed = 0.25f, padding = 16f, size = 12f, density = 1200f;

            @Override
            public void load(){
                super.load();

                region = Core.atlas.find("circle-shadow");
            }

            @Override
            public void draw(Weatherc state){
                rand.setSeed(0);
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / density * state.intensity());

                for(int i = 0; i < total; i++){
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

                    if(Tmp.r3.setCentered(x, y, size * sscl).overlaps(Tmp.r2)){
                        Draw.rect(region, x, y, size * sscl, size * sscl);
                    }
                }
            }
        };

        rain = new Weather("rain"){
            float yspeed = 7f, xspeed = 2f, padding = 16f, size = 40f, density = 1000f;

            @Override
            public void draw(Weatherc state){
                rand.setSeed(0);
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / density * state.intensity());
                Lines.stroke(0.75f);
                Draw.color(Color.royal, Color.white, 0.3f);
                float alpha = Draw.getColor().a;

                for(int i = 0; i < total; i++){
                    float scl = rand.random(0.5f, 1f);
                    float scl2 = rand.random(0.5f, 1f);
                    float sscl = rand.random(0.2f, 1f);
                    float x = (rand.random(0f, world.unitWidth()) + Time.time() * xspeed * scl2);
                    float y = (rand.random(0f, world.unitHeight()) - Time.time() * yspeed * scl);
                    float tint = rand.random(1f) * alpha;

                    x -= Tmp.r1.x;
                    y -= Tmp.r1.y;
                    x = Mathf.mod(x, Tmp.r1.width);
                    y = Mathf.mod(y, Tmp.r1.height);
                    x += Tmp.r1.x;
                    y += Tmp.r1.y;

                    if(Tmp.r3.setCentered(x, y, size * sscl).overlaps(Tmp.r2)){
                        Draw.alpha(tint);
                        Lines.lineAngle(x, y, Angles.angle(xspeed * scl2, - yspeed * scl), size*sscl/2f);
                    }
                }
            }
        };
    }
}
