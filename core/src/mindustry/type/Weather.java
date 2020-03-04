package mindustry.type;

import mindustry.ctype.*;

public abstract class Weather extends MappableContent{
    protected float duration = 100f;

    public Weather(String name){
        super(name);
    }

    public abstract void update();

    public abstract void draw();

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }

    //TODO implement
}
