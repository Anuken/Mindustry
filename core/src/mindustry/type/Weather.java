package mindustry.type;

import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.type.Weather.*;

import java.io.*;

import static mindustry.Vars.*;

public abstract class Weather<T extends WeatherEntity> extends MappableContent{
    protected float duration = 100f;

    public Weather(String name){
        super(name);
    }

    public abstract void update(T entity);

    public abstract void draw(T entity);

    @Override
    public ContentType getContentType(){
        return ContentType.weather;
    }

    /** Represents the in-game state of a weather event. */
    @SuppressWarnings("unchecked")
    public static class WeatherEntity extends BaseEntity implements SaveTrait, DrawTrait{
        /** How long this event has been occuring in ticks. */
        protected float life;
        /** Type of weather that is being simulated. */
        protected @NonNull
        Weather weather;

        public WeatherEntity(Weather weather){
            this.weather = weather;
        }

        @Override
        public void update(){
            weather.update(this);
        }

        @Override
        public void draw(){
            weather.draw(this);
        }

        @CallSuper
        @Override
        public void writeSave(DataOutput stream) throws IOException{
            stream.writeShort(weather.id);
        }

        @CallSuper
        @Override
        public void readSave(DataInput stream, byte version) throws IOException{
            weather = content.getByID(ContentType.weather, stream.readShort());
        }

        @Override
        public byte version(){
            return 0;
        }

        @Override
        public TypeID getTypeID(){
            return null;
        }

        @Override
        public EntityGroup targetGroup(){
            return weatherGroup;
        }
    }
}
