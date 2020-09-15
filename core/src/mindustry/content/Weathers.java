package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow,
    sandstorm,
    sporestorm;

    @Override
    public void load(){
        snow = new Weather("snow"){
            TextureRegion region;
            float yspeed = 2f, xspeed = 0.25f, padding = 16f, size = 12f, density = 1200f;

            {
                attrs.set(Attribute.light, -0.15f);
            }

            @Override
            public void load(){
                super.load();

                region = Core.atlas.find("circle-shadow");
            }

            @Override
            public void drawOver(WeatherState state){
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
            float yspeed = 5f, xspeed = 1.5f, padding = 16f, size = 40f, density = 1200f;
            TextureRegion[] splashes = new TextureRegion[12];

            {
                attrs.set(Attribute.light, -0.2f);
                attrs.set(Attribute.water, 0.2f);
                status = StatusEffects.wet;
            }

            @Override
            public void load(){
                super.load();

                for(int i = 0; i < splashes.length; i++){
                    splashes[i] = Core.atlas.find("splash-" + i);
                }
            }

            @Override
            public void drawOver(WeatherState state){
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
            public void drawUnder(WeatherState state){
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
            float size = 140f, padding = size, invDensity = 1500f, baseSpeed = 6.1f;
            float force = 0.45f;
            Color color = Color.valueOf("f7cba4");
            Texture noise;

            {
                attrs.set(Attribute.light, -0.1f);
            }

            @Override
            public void load(){
                region = Core.atlas.find("circle-shadow");
                noise = new Texture("sprites/noiseAlpha.png");
                noise.setWrap(TextureWrap.repeat);
                noise.setFilter(TextureFilter.linear);
            }

            @Override
            public void dispose(){
                noise.dispose();
            }

            @Override
            public void update(WeatherState state){
                float speed = force * state.intensity;
                float windx = state.windVector.x * speed, windy = state.windVector.y * speed;

                for(Unit unit : Groups.unit){
                    unit.impulse(windx, windy);
                }
            }

            @Override
            public void drawOver(WeatherState state){
                Draw.tint(color);
                float speed = baseSpeed * state.intensity;
                float windx = state.windVector.x * speed, windy = state.windVector.y * speed;

                float scale = 1f / 2000f;
                float scroll = Time.time() * scale;
                Tmp.tr1.texture = noise;
                Core.camera.bounds(Tmp.r1);
                Tmp.tr1.set(Tmp.r1.x*scale, Tmp.r1.y*scale, (Tmp.r1.x + Tmp.r1.width)*scale, (Tmp.r1.y + Tmp.r1.height)*scale);
                Tmp.tr1.scroll(-windx * scroll, windy * scroll);
                Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);

                rand.setSeed(0);
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / invDensity * state.intensity());
                Draw.tint(color);
                float baseAlpha = Draw.getColor().a;

                for(int i = 0; i < total; i++){
                    float scl = rand.random(0.5f, 1f);
                    float scl2 = rand.random(0.5f, 1f);
                    float sscl = rand.random(0.5f, 1f);
                    float x = (rand.random(0f, world.unitWidth()) + Time.time() * windx * scl2);
                    float y = (rand.random(0f, world.unitHeight()) + Time.time() * windy * scl);
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
                        Draw.rect(region, x, y, size * sscl, size * sscl);
                    }
                }
            }
        };

        sporestorm = new Weather("sporestorm"){
            TextureRegion region;
            float size = 5f, padding = size, invDensity = 2000f, baseSpeed = 4.3f, force = 0.28f;
            Color color = Color.valueOf("7457ce");
            Texture noise;

            {
                attrs.set(Attribute.spores, 1f);
                attrs.set(Attribute.light, -0.15f);
                status = StatusEffects.sporeSlowed;
                statusGround = false;
            }

            @Override
            public void load(){
                region = Core.atlas.find("circle-shadow");
                noise = new Texture("sprites/noiseAlpha.png");
                noise.setWrap(TextureWrap.repeat);
                noise.setFilter(TextureFilter.linear);
            }

            @Override
            public void update(WeatherState state){
                float speed = force * state.intensity;
                float windx = state.windVector.x * speed, windy = state.windVector.y * speed;

                for(Unit unit : Groups.unit){
                    unit.impulse(windx, windy);
                }
            }

            @Override
            public void dispose(){
                noise.dispose();
            }

            @Override
            public void drawOver(WeatherState state){
                Draw.alpha(state.opacity * 0.8f);
                Draw.tint(color);

                float speed = baseSpeed * state.intensity;
                float windx = state.windVector.x * speed, windy = state.windVector.y * speed;

                float scale = 1f / 2000f;
                float scroll = Time.time() * scale;
                Tmp.tr1.texture = noise;
                Core.camera.bounds(Tmp.r1);
                Tmp.tr1.set(Tmp.r1.x*scale, Tmp.r1.y*scale, (Tmp.r1.x + Tmp.r1.width)*scale, (Tmp.r1.y + Tmp.r1.height)*scale);
                Tmp.tr1.scroll(-windx * scroll, windy * scroll);
                Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);

                rand.setSeed(0);
                Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
                Tmp.r1.grow(padding);
                Core.camera.bounds(Tmp.r2);
                int total = (int)(Tmp.r1.area() / invDensity * state.intensity());
                Draw.tint(color);
                float baseAlpha = state.opacity;
                Draw.alpha(baseAlpha);

                for(int i = 0; i < total; i++){
                    float scl = rand.random(0.5f, 1f);
                    float scl2 = rand.random(0.5f, 1f);
                    float sscl = rand.random(0.5f, 1f);
                    float x = (rand.random(0f, world.unitWidth()) + Time.time() * windx * scl2);
                    float y = (rand.random(0f, world.unitHeight()) + Time.time() * windy * scl);
                    float alpha = rand.random(0.1f, 0.8f);

                    x += Mathf.sin(y, rand.random(30f, 80f), rand.random(1f, 7f));

                    x -= Tmp.r1.x;
                    y -= Tmp.r1.y;
                    x = Mathf.mod(x, Tmp.r1.width);
                    y = Mathf.mod(y, Tmp.r1.height);
                    x += Tmp.r1.x;
                    y += Tmp.r1.y;

                    if(Tmp.r3.setCentered(x, y, size * sscl).overlaps(Tmp.r2)){
                        Draw.alpha(alpha * baseAlpha);
                        Fill.circle(x, y, size * sscl / 2f);
                    }
                }
            }
        };
    }
}
