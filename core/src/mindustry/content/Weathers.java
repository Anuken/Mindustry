package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow,
    sandstorm;

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
            public void drawOver(Weatherc state){
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

        //TODO should apply wet effect
        rain = new Weather("rain"){
            float yspeed = 5f, xspeed = 1.5f, padding = 16f, size = 40f, density = 1200f;
            TextureRegion[] splashes = new TextureRegion[12];

            @Override
            public void load(){
                super.load();

                for(int i = 0; i < splashes.length; i++){
                    splashes[i] = Core.atlas.find("splash-" + i);
                }
            }

            @Override
            public void drawOver(Weatherc state){
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / density * state.intensity());
                Lines.stroke(0.75f);
                float alpha = Draw.getColor().a;
                Draw.color(Color.royal, Color.white, 0.3f);

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

            @Override
            public void drawUnder(Weatherc state){
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / density * state.intensity()) / 2;
                Lines.stroke(0.75f);

                float t = Time.time() / 22f;

                for(int i = 0; i < total; i++){
                    float offset = rand.random(0f, 1f);
                    float time = t + offset;

                    int pos = (int)((time));
                    float life = time % 1f;
                    float x = (rand.random(0f, world.unitWidth()) + pos*953);
                    float y = (rand.random(0f, world.unitHeight()) - pos*453);

                    x -= Tmp.r1.x;
                    y -= Tmp.r1.y;
                    x = Mathf.mod(x, Tmp.r1.width);
                    y = Mathf.mod(y, Tmp.r1.height);
                    x += Tmp.r1.x;
                    y += Tmp.r1.y;

                    if(Tmp.r3.setCentered(x, y, life * 4f).overlaps(Tmp.r2)){
                        Tile tile = world.tileWorld(x, y);

                        if(tile != null && tile.floor().liquidDrop == Liquids.water){
                            Draw.color(Tmp.c1.set(tile.floor().mapColor).mul(1.5f).a(state.opacity()));
                            Draw.rect(splashes[(int)(life * (splashes.length - 1))], x, y);
                        }else{
                            Draw.color(Color.royal, Color.white, 0.3f);
                            Draw.alpha(Mathf.slope(life) * state.opacity());

                            float space = 45f;
                            for(int j : new int[]{-1, 1}){
                                Tmp.v1.trns(90f + j*space, 1f + 5f * life);
                                Lines.lineAngle(x + Tmp.v1.x, y + Tmp.v1.y, 90f + j*space, 3f * (1f - life));
                            }
                        }
                    }
                }
            }
        };

        sandstorm = new Weather("sandstorm"){
            TextureRegion region;
            float yspeed = 0.3f, xspeed = 6f, padding = 110f, size = 110f, invDensity = 800f;
            Vec2 force = new Vec2(0.4f, 0.01f);
            Color color = Color.valueOf("f7cba4");

            @Override
            public void load(){
                super.load();

                region = Core.atlas.find("circle-shadow");
            }

            @Override
            public void drawOver(Weatherc state){
                rand.setSeed(0);
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / invDensity * state.intensity());
                Draw.tint(color);
                float baseAlpha = Draw.getColor().a;

                for(Unitc unit : Groups.unit){
                    unit.impulse(force.x * state.intensity(), force.y * state.intensity());
                }

                for(int i = 0; i < total; i++){
                    float scl = rand.random(0.5f, 1f);
                    float scl2 = rand.random(0.5f, 1f);
                    float sscl = rand.random(0.5f, 1f);
                    float x = (rand.random(0f, world.unitWidth()) + Time.time() * xspeed * scl2);
                    float y = (rand.random(0f, world.unitHeight()) - Time.time() * yspeed * scl);
                    float alpha = rand.random(0.2f);

                    x += Mathf.sin(y, rand.random(30f, 80f), rand.random(1f, 7f));

                    x -= Tmp.r1.x;
                    y -= Tmp.r1.y;
                    x = Mathf.mod(x, Tmp.r1.width);
                    y = Mathf.mod(y, Tmp.r1.height);
                    x += Tmp.r1.x;
                    y += Tmp.r1.y;

                    if(Tmp.r3.setCentered(x, y, size * sscl).overlaps(Tmp.r2)){
                        Draw.alpha(alpha * baseAlpha);
                        //Fill.circle(x, y, size * sscl / 2f);
                        Draw.rect(region, x, y, size * sscl, size * sscl);
                    }
                }
            }
        };
    }
}
