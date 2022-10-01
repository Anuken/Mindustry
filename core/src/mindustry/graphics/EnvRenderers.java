package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class EnvRenderers{

    public static void init(){

        Color waterColor = Color.valueOf("353982");
        Rand rand = new Rand();

        Core.assets.load("sprites/rays.png", Texture.class).loaded = t -> {
            t.setFilter(TextureFilter.linear);
        };

        Color particleColor = Color.valueOf("a7c1fa");
        float windSpeed = 0.03f, windAngle = 45f;
        float windx = Mathf.cosDeg(windAngle) * windSpeed, windy = Mathf.sinDeg(windAngle) * windSpeed;

        renderer.addEnvRenderer(Env.underwater, () -> {
            Draw.draw(Layer.light + 1, () -> {
                Draw.color(waterColor, 0.4f);
                Fill.rect(Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height);
                Draw.reset();

                Blending.additive.apply();
                Draw.blit(Shaders.caustics);
                Blending.normal.apply();
            });

            Draw.z(Layer.light + 2);

            int rays = 50;
            float timeScale = 2000f;
            rand.setSeed(0);

            Draw.blend(Blending.additive);

            float t = Time.time / timeScale;
            Texture tex = Core.assets.get("sprites/rays.png", Texture.class);

            for(int i = 0; i < rays; i++){
                float offset = rand.random(0f, 1f);
                float time = t + offset;

                int pos = (int)time;
                float life = time % 1f;
                float opacity = rand.random(0.2f, 0.7f) * Mathf.slope(life) * 0.7f;
                float x = (rand.random(0f, world.unitWidth()) + (pos % 100)*753) % world.unitWidth();
                float y = (rand.random(0f, world.unitHeight()) + (pos % 120)*453) % world.unitHeight();
                float rot = rand.range(7f);
                float sizeScale = 1f + rand.range(0.3f);

                float topDst = (Core.camera.position.y + Core.camera.height/2f) - (y + tex.height/2f + tex.height*1.9f*sizeScale/2f);
                float invDst = topDst/1000f;
                opacity = Math.min(opacity, -invDst);

                if(opacity > 0.01){
                    Draw.alpha(opacity);
                    Draw.rect(Draw.wrap(tex), x, y + tex.height/2f, tex.width*2*sizeScale, tex.height*2*sizeScale, rot);
                    Draw.color();
                }
            }

            //suspended particles
            Draw.draw(Layer.weather, () -> {
                Weather.drawParticles(
                    Core.atlas.find("particle"), particleColor,
                    1.4f, 4f, //minmax size
                    10000f, 1f, 1f, //density
                    windx, windy, //wind vectors
                    0.5f, 1f, //minmax alpha
                    30f, 80f, //sinscl
                    1f, 7f, //sinmag
                    false
                );
            });

            Draw.blend();
        });

        Core.assets.load("sprites/distortAlpha.png", Texture.class);

        renderer.addEnvRenderer(Env.scorching, () -> {
            Texture tex = Core.assets.get("sprites/distortAlpha.png", Texture.class);
            if(tex.getMagFilter() != TextureFilter.linear){
                tex.setFilter(TextureFilter.linear);
                tex.setWrap(TextureWrap.repeat);
            }

            //TODO layer looks better? should not be conditional
            Draw.z(state.rules.fog ? Layer.fogOfWar + 1 : Layer.weather - 1);
            Weather.drawNoiseLayers(tex, Color.scarlet, 1000f, 0.24f, 0.4f, 1f, 1f, 0f, 4, -1.3f, 0.7f, 0.8f, 0.9f);
            Draw.reset();
        });
    }

}
