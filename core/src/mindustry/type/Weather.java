package mindustry.type;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.renderer;

public abstract class Weather extends MappableContent{
    /** Default duration of this weather event in ticks. */
    public float duration = 15f * Time.toMinutes;

    //internals
    public Rand rand = new Rand();
    public Prov<Weatherc> type = WeatherEntity::create;

    public Weather(String name, Prov<Weatherc> type){
        super(name);
        this.type = type;
    }

    public Weather(String name){
        super(name);
    }

    public Weatherc create(){
        return create(1f);
    }

    public Weatherc create(float intensity){
        return create(intensity, duration);
    }

    public Weatherc create(float intensity, float duration){
        Weatherc entity = type.get();
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

    public void update(Weatherc state){

    }

    public void drawOver(Weatherc state){

    }

    public void drawUnder(Weatherc state){

    }

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }

    @Remote(called = Loc.server)
    public static void createWeather(Weather weather, float intensity, float duration){
        weather.create(intensity, duration);
    }

    public static class WeatherEntry{
        /** The type of weather used. */
        public Weather weather;
        /** Minimum and maximum spacing between weather events. Does not include the time of the event itself. */
        public float minFrequency, maxFrequency, minDuration, maxDuration;
        /** Cooldown time before the next weather event takes place. */
        public float cooldown;
        /** Intensity of the weather produced. */
        public float intensity = 1f;

        /** Creates a weather entry with some approximate weather values. */
        public WeatherEntry(Weather weather){
            this(weather, weather.duration/2f, weather.duration * 1.5f, weather.duration/2f, weather.duration * 1.5f);
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

    @EntityDef(value = {Weatherc.class}, pooled = true, isFinal = false)
    @Component
    abstract static class WeatherComp implements Drawc{
        private static final float fadeTime = 60 * 4;

        Weather weather;
        float intensity = 1f, opacity = 1f, life;

        void init(Weather weather){
            this.weather = weather;
        }

        @Override
        public void update(){
            if(life < fadeTime){
                opacity = life / fadeTime;
            }else{
                opacity = Mathf.lerpDelta(opacity, 1f, 0.01f);
            }

            life -= Time.delta();

            weather.update((Weatherc)this);

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
                    weather.drawOver((Weatherc)this);
                    Draw.reset();
                });

                Draw.draw(Layer.debris, () -> {
                    weather.rand.setSeed(0);
                    Draw.alpha(renderer.weatherAlpha() * opacity);
                    weather.drawUnder((Weatherc)this);
                    Draw.reset();
                });
            }
        }
    }
}
