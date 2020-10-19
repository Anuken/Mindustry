package mindustry.content;

import arc.graphics.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.type.weather.*;
import mindustry.world.meta.*;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow,
    sandstorm,
    sporestorm,
    fog;

    @Override
    public void load(){
        snow = new ParticleWeather("snow"){{
            sizeMax = 13f;
            sizeMin = 2.6f;
            density = 1200f;
            attrs.set(Attribute.light, -0.15f);
        }};

        rain = new RainWeather("rain"){{
            attrs.set(Attribute.light, -0.2f);
            attrs.set(Attribute.water, 0.2f);
            status = StatusEffects.wet;
        }};

        sandstorm = new ParticleWeather("sandstorm"){{
            color = noiseColor = Color.valueOf("f7cba4");
            drawNoise = true;
            useWindVector = true;
            sizeMax = 140f;
            sizeMin = 70f;
            minAlpha = 0f;
            maxAlpha = 0.2f;
            density = 1500f;
            baseSpeed = 6.1f;
            attrs.set(Attribute.light, -0.1f);
            attrs.set(Attribute.water, -0.1f);
            opacityMultiplier = 0.8f;
            force = 0.1f;
        }};

        sporestorm = new ParticleWeather("sporestorm"){{
            color = noiseColor = Color.valueOf("7457ce");
            particleRegion = "circle";
            drawNoise = true;
            statusGround = false;
            useWindVector = true;
            sizeMax = 5f;
            sizeMin = 2.5f;
            minAlpha = 0.1f;
            maxAlpha = 0.8f;
            density = 2000f;
            baseSpeed = 4.3f;
            attrs.set(Attribute.spores, 1f);
            attrs.set(Attribute.light, -0.15f);
            status = StatusEffects.sporeSlowed;
            opacityMultiplier = 0.85f;
            force = 0.1f;
        }};

        fog = new ParticleWeather("fog"){{
            duration = 15f * Time.toMinutes;
            noiseLayers = 3;
            noiseLayerSclM = 0.8f;
            noiseLayerAlphaM = 0.7f;
            noiseLayerSpeedM = 2f;
            noiseLayerSclM = 0.6f;
            baseSpeed = 0.05f;
            color = noiseColor = Color.grays(0.4f);
            noiseScale = 1100f;
            noisePath = "fog";
            drawParticles = false;
            drawNoise = true;
            useWindVector = false;
            xspeed = 1f;
            yspeed = 0.01f;
            attrs.set(Attribute.light, -0.3f);
            attrs.set(Attribute.water, 0.05f);
            opacityMultiplier = 0.45f;
        }};
    }
}
