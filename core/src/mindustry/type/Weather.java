package mindustry.type;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public abstract class Weather extends UnlockableContent{
    /** Default duration of this weather event in ticks. */
    public float duration = 9f * Time.toMinutes;
    public Attributes attrs = new Attributes();

    //internals
    public Rand rand = new Rand();
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
        entity.intensity(intensity);
        entity.init(this);
        entity.life(duration);
        entity.add();
        return entity;
    }

    public boolean isActive(){
        return Groups.weather.find(w -> w.weather() == this) != null;
    }

    public void remove(){
        Entityc e = Groups.weather.find(w -> w.weather() == this);
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
    }

    public void drawOver(WeatherState state){

    }

    public void drawUnder(WeatherState state){

    }

    @Override
    public void displayInfo(Table table){
        //do not
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

        /** Creates a weather entry with some approximate weather values. */
        public WeatherEntry(Weather weather){
            this(weather, weather.duration * 1f, weather.duration * 3f, weather.duration / 2f, weather.duration * 1.5f);
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
            if(renderer.weatherAlpha() > 0.0001f){
                Draw.draw(Layer.weather, () -> {
                    weather.rand.setSeed(0);
                    Draw.alpha(renderer.weatherAlpha() * opacity);
                    weather.drawOver(self());
                    Draw.reset();
                });

                Draw.draw(Layer.debris, () -> {
                    weather.rand.setSeed(0);
                    Draw.alpha(renderer.weatherAlpha() * opacity);
                    weather.drawUnder(self());
                    Draw.reset();
                });
            }
        }
    }
}
