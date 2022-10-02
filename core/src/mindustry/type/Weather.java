package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class Weather extends UnlockableContent{
    /** Global random variable used for rendering. */
    public static final Rand rand = new Rand();

    /** Default duration of this weather event in ticks. */
    public float duration = 10f * Time.toMinutes;
    public float opacityMultiplier = 1f;
    public Attributes attrs = new Attributes();
    public Sound sound = Sounds.none;
    public float soundVol = 0.1f, soundVolMin = 0f;
    public float soundVolOscMag = 0f, soundVolOscScl = 20f;
    public boolean hidden = false;

    //internals
    public Prov<WeatherState> type = WeatherState::create;
    public StatusEffect status = StatusEffects.none;
    public float statusDuration = 60f * 2;
    public boolean statusAir = true, statusGround = true;

    public Weather(String name, Prov<WeatherState> type){
        super(name);
        this.type = type;
    }

    public Weather(String name){
        super(name);
    }

    public WeatherState create(){
        return create(1f);
    }

    public WeatherState create(float intensity){
        return create(intensity, duration);
    }

    public WeatherState create(float intensity, float duration){
        WeatherState entity = type.get();
        entity.intensity(Mathf.clamp(intensity));
        entity.init(this);
        entity.life(duration);
        entity.add();
        return entity;
    }

    @Nullable
    public WeatherState instance(){
        return Groups.weather.find(w -> w.weather() == this);
    }

    public boolean isActive(){
        return instance() != null;
    }

    public void remove(){
        var e = instance();
        if(e != null) e.remove();
    }

    public void update(WeatherState state){

    }

    public void updateEffect(WeatherState state){
        if(status != StatusEffects.none){
            if(state.effectTimer <= 0){
                state.effectTimer = statusDuration - 5f;

                Groups.unit.each(u -> {
                    if(u.checkTarget(statusAir, statusGround)){
                        u.apply(status, statusDuration);
                    }
                });
            }else{
                state.effectTimer -= Time.delta;
            }
        }

        if(!headless && sound != Sounds.none){
            float noise = soundVolOscMag > 0 ? (float)Math.abs(Noise.rawNoise(Time.time / soundVolOscScl)) * soundVolOscMag : 0;
            control.sound.loop(sound, Math.max((soundVol + noise) * state.opacity, soundVolMin));
        }
    }

    public void drawOver(WeatherState state){

    }

    public void drawUnder(WeatherState state){

    }

    public static void drawParticles(TextureRegion region, Color color,
                              float sizeMin, float sizeMax,
                              float density, float intensity, float opacity,
                              float windx, float windy,
                              float minAlpha, float maxAlpha,
                              float sinSclMin, float sinSclMax, float sinMagMin, float sinMagMax,
                              boolean randomParticleRotation){
        rand.setSeed(0);
        Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
        Tmp.r1.grow(sizeMax * 1.5f);
        Core.camera.bounds(Tmp.r2);
        int total = (int)(Tmp.r1.area() / density * intensity);
        Draw.color(color, opacity);

        for(int i = 0; i < total; i++){
            float scl = rand.random(0.5f, 1f);
            float scl2 = rand.random(0.5f, 1f);
            float size = rand.random(sizeMin, sizeMax);
            float x = (rand.random(0f, world.unitWidth()) + Time.time * windx * scl2);
            float y = (rand.random(0f, world.unitHeight()) + Time.time * windy * scl);
            float alpha = rand.random(minAlpha, maxAlpha);
            float rotation = randomParticleRotation ? rand.random(0f, 360f) : 0f;

            x += Mathf.sin(y, rand.random(sinSclMin, sinSclMax), rand.random(sinMagMin, sinMagMax));

            x -= Tmp.r1.x;
            y -= Tmp.r1.y;
            x = Mathf.mod(x, Tmp.r1.width);
            y = Mathf.mod(y, Tmp.r1.height);
            x += Tmp.r1.x;
            y += Tmp.r1.y;

            if(Tmp.r3.setCentered(x, y, size).overlaps(Tmp.r2)){
                Draw.alpha(alpha * opacity);
                Draw.rect(region, x, y, size, size, rotation);
            }
        }

        Draw.reset();
    }

    public static void drawRain(float sizeMin, float sizeMax, float xspeed, float yspeed, float density, float intensity, float stroke, Color color){
        rand.setSeed(0);
        float padding = sizeMax*0.9f;

        Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
        Tmp.r1.grow(padding);
        Core.camera.bounds(Tmp.r2);
        int total = (int)(Tmp.r1.area() / density * intensity);
        Lines.stroke(stroke);
        float alpha = Draw.getColor().a;
        Draw.color(color);

        for(int i = 0; i < total; i++){
            float scl = rand.random(0.5f, 1f);
            float scl2 = rand.random(0.5f, 1f);
            float size = rand.random(sizeMin, sizeMax);
            float x = (rand.random(0f, world.unitWidth()) + Time.time * xspeed * scl2);
            float y = (rand.random(0f, world.unitHeight()) - Time.time * yspeed * scl);
            float tint = rand.random(1f) * alpha;

            x -= Tmp.r1.x;
            y -= Tmp.r1.y;
            x = Mathf.mod(x, Tmp.r1.width);
            y = Mathf.mod(y, Tmp.r1.height);
            x += Tmp.r1.x;
            y += Tmp.r1.y;

            if(Tmp.r3.setCentered(x, y, size).overlaps(Tmp.r2)){
                Draw.alpha(tint);
                Lines.lineAngle(x, y, Angles.angle(xspeed * scl2, - yspeed * scl), size/2f);
            }
        }
    }

    public static void drawSplashes(TextureRegion[] splashes, float padding, float density, float intensity, float opacity, float timeScale, float stroke, Color color, Liquid splasher){
        Tmp.r1.setCentered(Core.camera.position.x, Core.camera.position.y, Core.graphics.getWidth() / renderer.minScale(), Core.graphics.getHeight() / renderer.minScale());
        Tmp.r1.grow(padding);
        Core.camera.bounds(Tmp.r2);
        int total = (int)(Tmp.r1.area() / density * intensity) / 2;
        Lines.stroke(stroke);
        rand.setSeed(0);

        float t = Time.time / timeScale;

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

                //only create splashes on specific liquid.
                if(tile != null && tile.floor().liquidDrop == splasher){
                    Draw.color(Tmp.c1.set(tile.floor().mapColor).mul(1.5f).a(opacity));
                    Draw.rect(splashes[(int)(life * (splashes.length - 1))], x, y);
                }else if(tile != null && tile.floor().liquidDrop == null && !tile.floor().solid){
                    Draw.color(color);
                    Draw.alpha(Mathf.slope(life) * opacity);

                    float space = 45f;
                    for(int j : new int[]{-1, 1}){
                        Tmp.v1.trns(90f + j*space, 1f + 5f * life);
                        Lines.lineAngle(x + Tmp.v1.x, y + Tmp.v1.y, 90f + j*space, 3f * (1f - life));
                    }
                }
            }
        }
    }

    public static void drawNoiseLayers(Texture noise, Color color, float noisescl, float opacity, float baseSpeed, float intensity, float vwindx, float vwindy,
                                       int layers, float layerSpeedM , float layerAlphaM, float layerSclM, float layerColorM){
        float sspeed = 1f, sscl = 1f, salpha = 1f, offset = 0f;
        Color col = Tmp.c1.set(color);
        for(int i = 0; i < layers; i++){
            drawNoise(noise, col, noisescl * sscl, salpha * opacity, sspeed * baseSpeed, intensity, vwindx, vwindy, offset);
            sspeed *= layerSpeedM;
            salpha *= layerAlphaM;
            sscl *= layerSclM;
            offset += 0.29f;
            col.mul(layerColorM);
        }
    }

    public static void drawNoise(Texture noise, Color color, float noisescl, float opacity, float baseSpeed, float intensity, float vwindx, float vwindy, float offset){
        Draw.alpha(opacity);
        Draw.tint(color);

        float speed = baseSpeed * intensity;
        float windx = vwindx * speed, windy = vwindy * speed;

        float scale = 1f / noisescl;
        float scroll = Time.time * scale + offset;
        Tmp.tr1.texture = noise;
        Core.camera.bounds(Tmp.r1);
        Tmp.tr1.set(Tmp.r1.x*scale, Tmp.r1.y*scale, (Tmp.r1.x + Tmp.r1.width)*scale, (Tmp.r1.y + Tmp.r1.height)*scale);
        Tmp.tr1.scroll(-windx * scroll, -windy * scroll);
        Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);
    }

    @Override
    public boolean isHidden(){
        return true;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }

    @Remote(called = Loc.server)
    public static void createWeather(Weather weather, float intensity, float duration, float windX, float windY){
        weather.create(intensity, duration).windVector.set(windX, windY);
    }

    public static class WeatherEntry{
        /** The type of weather used. */
        public Weather weather;
        /** Minimum and maximum spacing between weather events. Does not include the time of the event itself. */
        public float minFrequency, maxFrequency, minDuration, maxDuration;
        /** Cooldown time before the next weather event takes place This is *state*, not configuration. */
        public float cooldown;
        /** Intensity of the weather produced. */
        public float intensity = 1f;
        /** If true, this weather is always active. */
        public boolean always = false;

        /** Creates a weather entry with some approximate weather values. */
        public WeatherEntry(Weather weather){
            this(weather, weather.duration * 2f, weather.duration * 6f, weather.duration / 2f, weather.duration * 1.5f);
        }

        public WeatherEntry(Weather weather, float minFrequency, float maxFrequency, float minDuration, float maxDuration){
            this.weather = weather;
            this.minFrequency = minFrequency;
            this.maxFrequency = maxFrequency;
            this.minDuration = minDuration;
            this.maxDuration = maxDuration;
            //specifies cooldown to something random
            this.cooldown = Mathf.random(minFrequency, maxFrequency);
        }

        //mods
        public WeatherEntry(){

        }
    }

    @EntityDef(value = {WeatherStatec.class}, pooled = true, isFinal = false)
    @Component(base = true)
    abstract static class WeatherStateComp implements Drawc, Syncc{
        private static final float fadeTime = 60 * 4;

        Weather weather;
        float intensity = 1f, opacity = 0f, life, effectTimer;
        Vec2 windVector = new Vec2().setToRandomDirection();

        void init(Weather weather){
            this.weather = weather;
        }

        @Override
        public void update(){
            if(life < fadeTime){
                opacity = Math.min(life / fadeTime, opacity);
            }else{
                opacity = Mathf.lerpDelta(opacity, 1f, 0.004f);
            }

            life -= Time.delta;

            weather.update(self());
            weather.updateEffect(self());

            if(life < 0){
                remove();
            }
        }

        @Override
        public void draw(){
            if(renderer.weatherAlpha > 0.0001f && renderer.drawWeather && Core.settings.getBool("showweather")){
                Draw.draw(Layer.weather, () -> {
                    Draw.alpha(renderer.weatherAlpha * opacity * weather.opacityMultiplier);
                    weather.drawOver(self());
                    Draw.reset();
                });

                Draw.draw(Layer.debris, () -> {
                    Draw.alpha(renderer.weatherAlpha * opacity * weather.opacityMultiplier);
                    weather.drawUnder(self());
                    Draw.reset();
                });
            }
        }
    }
}
