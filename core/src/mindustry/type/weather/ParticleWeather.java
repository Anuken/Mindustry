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

    public Color noiseColor = color;
    public boolean drawNoise = false, drawParticles = true, useWindVector = false, randomParticleRotation = false;
    public int noiseLayers = 1;
    public float noiseLayerSpeedM = 1.1f, noiseLayerAlphaM = 0.8f, noiseLayerSclM = 0.99f, noiseLayerColorM = 1f;
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
        if(drawNoise && Core.assets != null){
            Core.assets.load("sprites/" + noisePath + ".png", Texture.class);
        }
    }

    @Override
    public void update(WeatherState state){
        float speed = force * state.intensity * Time.delta;
        if(speed > 0.001f){
            float windx = state.windVector.x * speed, windy = state.windVector.y * speed;

            for(Unit unit : Groups.unit){
                unit.impulse(windx, windy);
            }
        }
    }

    @Override
    public void drawOver(WeatherState state){

        float windx, windy;
        if(useWindVector){
            float speed = baseSpeed * state.intensity;
            windx = state.windVector.x * speed;
            windy = state.windVector.y * speed;
        }else{
            windx = this.xspeed;
            windy = this.yspeed;
        }

        if(drawNoise){
            if(noise == null){
                noise = Core.assets.get("sprites/" + noisePath + ".png", Texture.class);
                noise.setWrap(TextureWrap.repeat);
                noise.setFilter(TextureFilter.linear);
            }

            float sspeed = 1f, sscl = 1f, salpha = 1f, offset = 0f;
            Color col = Tmp.c1.set(noiseColor);
            for(int i = 0; i < noiseLayers; i++){
                drawNoise(noise, noiseColor, noiseScale * sscl, state.opacity * salpha * opacityMultiplier, sspeed * (useWindVector ? 1f : baseSpeed), state.intensity, windx, windy, offset);
                sspeed *= noiseLayerSpeedM;
                salpha *= noiseLayerAlphaM;
                sscl *= noiseLayerSclM;
                offset += 0.29f;
                col.mul(noiseLayerColorM);
            }
        }

        if(drawParticles){
            drawParticles(region, color, sizeMin, sizeMax, density, state.intensity, state.opacity, windx, windy, minAlpha, maxAlpha, sinSclMin, sinSclMax, sinMagMin, sinMagMax, randomParticleRotation);
        }
    }
}
