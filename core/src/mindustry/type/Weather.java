package mindustry.type;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.renderer;

public abstract class Weather extends MappableContent{
    /** Default duration of this weather event in ticks. */
    public float duration = 15f * Time.toMinutes;
    public Attributes attrs = new Attributes();

    //internals
    public Rand rand = new Rand();
    public Prov<WeatherState> type = WeatherState::create;

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

    public void drawOver(WeatherState state){

    }

    public void drawUnder(WeatherState state){

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

    @EntityDef(value = {WeatherStatec.class}, pooled = true, isFinal = false)
    @Component(base = true)
    abstract static class WeatherStateComp implements Drawc{
        private static final float fadeTime = 60 * 4;

        Weather weather;
        float intensity = 1f, opacity = 0f, life;

        void init(Weather weather){
            this.weather = weather;
        }

        @Override
        public void update(){
            if(life < fadeTime){
                opacity = life / fadeTime;
            }else{
                opacity = Mathf.lerpDelta(opacity, 1f, 0.004f);
            }

            life -= Time.delta;

            weather.update(base());

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
                    weather.drawOver(base());
                    Draw.reset();
                });

                Draw.draw(Layer.debris, () -> {
                    weather.rand.setSeed(0);
                    Draw.alpha(renderer.weatherAlpha() * opacity);
                    weather.drawUnder(base());
                    Draw.reset();
                });
            }
        }
    }
}
