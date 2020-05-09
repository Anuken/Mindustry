package mindustry.type;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.renderer;

public abstract class Weather extends MappableContent{
    protected float duration = 100f;
    protected Rand rand = new Rand();
    protected Prov<Weatherc> type = WeatherEntity::create;

    public Weather(String name, Prov<Weatherc> type){
        super(name);
        this.type = type;
    }

    public Weather(String name){
        super(name);
    }

    public void create(){
        Weatherc entity = type.get();
        entity.init(this);
        entity.add();
    }

    public void remove(){
        Entityc e = Groups.weather.find(w -> w.weather() == this);
        if(e != null) e.remove();
    }

    public void update(Weatherc state){

    }

    public void draw(Weatherc state){

    }

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }

    @EntityDef(value = {Weatherc.class}, pooled = true, isFinal = false)
    @Component
    abstract class WeatherComp implements Posc, Drawc{
        Weather weather;
        float intensity = 1f, opacity = 1f;

        void init(Weather weather){
            this.weather = weather;
        }

        @Override
        public void update(){
            weather.update((Weatherc)this);
        }

        @Override
        public void draw(){
            if(renderer.weatherAlpha() > 0.0001f){
                Draw.draw(Layer.weather, () -> {
                    Draw.alpha(renderer.weatherAlpha() * opacity);
                    weather.draw((Weatherc)this);
                    Draw.reset();
                });
            }
        }

        @Override
        public float clipSize(){
            return Float.MAX_VALUE;
        }
    }
}
