package mindustry.content;

import arc.graphics.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.type.weather.*;
import mindustry.world.meta.*;

public class Weathers implements ContentList{
    public static Weather
    rain,
    snow,
    sandstorm,
    sporestorm,
    fog,
    suspendParticles;

    @Override
    public void load(){
        snow = new ParticleWeather("snow"){{
            particleRegion = "particle";
            sizeMax = 13f;
            sizeMin = 2.6f;
            density = 1200f;
            attrs.set(Attribute.light, -0.15f);

            sound = Sounds.windhowl;
            soundVol = 0f;
            soundVolOscMag = 1.5f;
            soundVolOscScl = 1100f;
            soundVolMin = 0.02f;
        }};

        rain = new RainWeather("rain"){{
            attrs.set(Attribute.light, -0.2f);
            attrs.set(Attribute.water, 0.2f);
            status = StatusEffects.wet;
            sound = Sounds.rain;
            soundVol = 0.25f;
        }};

        sandstorm = new ParticleWeather("sandstorm"){{
            color = noiseColor = Color.valueOf("f7cba4");
            particleRegion = "particle";
            drawNoise = true;
            useWindVector = true;
            sizeMax = 140f;
            sizeMin = 70f;
            minAlpha = 0f;
            maxAlpha = 0.2f;
            density = 1500f;
            baseSpeed = 5.4f;
            attrs.set(Attribute.light, -0.1f);
            attrs.set(Attribute.water, -0.1f);
            opacityMultiplier = 0.35f;
            force = 0.1f;
            sound = Sounds.wind;
            soundVol = 0.8f;
            duration = 7f * Time.toMinutes;
        }};

        sporestorm = new ParticleWeather("sporestorm"){{
            color = noiseColor = Color.valueOf("7457ce");
            particleRegion = "circle-small";
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
            opacityMultiplier = 0.5f;
            force = 0.1f;
            sound = Sounds.wind;
            soundVol = 0.7f;
            duration = 7f * Time.toMinutes;
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
            opacityMultiplier = 0.47f;
        }};

        suspendParticles = new ParticleWeather("suspend-particles"){{
            color = noiseColor = Color.valueOf("a7c1fa");
            particleRegion = "particle";
            statusGround = false;
            useWindVector = true;
            hidden = true;
            sizeMax = 4f;
            sizeMin = 1.4f;
            minAlpha = 0.5f;
            maxAlpha = 1f;
            density = 10000f;
            baseSpeed = 0.03f;
        }};
    }
}
