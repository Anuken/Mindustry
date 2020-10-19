package mindustry.type.weather;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;

public class ParticleWeather extends Weather{
    public String particleRegion = "circle-shadow";
    public Color color = Color.white.cpy();
    public TextureRegion region;
    public float yspeed = -2f, xspeed = 0.25f, padding = 16f, sizeMin = 2.4f, sizeMax = 12f, density = 1200f, minAlpha = 1f, maxAlpha = 1f, force = 0, noiseScale = 2000f, baseSpeed = 6.1f;
    public float sinSclMin = 30f, sinSclMax = 80f, sinMagMin = 1f, sinMagMax = 7f;

    public Color stormColor = color;
    public boolean drawStorm = false, drawParticles = true, useWindVector = false;
    public String noisePath = "noiseAlpha";
    public @Nullable Texture noise;

    public ParticleWeather(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();

        region = Core.atlas.find(particleRegion);

        //load noise texture
        //TODO mod support
        if(drawStorm){
            Core.assets.load("sprites/" + noisePath + ".png", Texture.class);
        }
    }

    @Override
    public void update(WeatherState state){
        float speed = force * state.intensity;
        if(speed > 0.001f){
            float windx = state.windVector.x * speed, windy = state.windVector.y * speed;

            for(Unit unit : Groups.unit){
                unit.impulse(windx, windy);
            }
        }
    }

    @Override
    public void drawOver(WeatherState state){

        if(drawStorm){
            if(noise == null){
                noise = Core.assets.get("sprites/" + noisePath + ".png", Texture.class);
                noise.setWrap(TextureWrap.repeat);
                noise.setFilter(TextureFilter.linear);
            }

            drawNoise(noise, stormColor, noiseScale, state.opacity, baseSpeed, state.intensity, state.windVector);
        }

        if(drawParticles){
            float windx, windy;
            if(useWindVector){
                float speed = baseSpeed * state.intensity;
                windx = state.windVector.x * speed;
                windy = state.windVector.y * speed;
            }else{
                windx = this.xspeed;
                windy = this.yspeed;
            }

            drawParticles(region, color, sizeMin, sizeMax, density, state.intensity, state.opacity, windx, windy, minAlpha, maxAlpha, sinSclMin, sinSclMax, sinMagMin, sinMagMax);
        }
    }
}
