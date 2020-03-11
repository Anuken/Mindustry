package mindustry.type;

import arc.func.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.gen.*;

public abstract class Weather extends MappableContent{
    protected float duration = 100f;
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

    public void update(){

    }

    public void draw(){

    }

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }

    @EntityDef(value = {Weatherc.class}, pooled = true, isFinal = false)
    @Component
    abstract class WeatherComp implements Posc, DrawLayerWeatherc{
        Weather weather;

        void init(Weather weather){
            this.weather = weather;
        }

        @Override
        public void drawWeather(){
            weather.draw();
        }

        @Override
        public float clipSize(){
            return Float.MAX_VALUE;
        }
    }
}
