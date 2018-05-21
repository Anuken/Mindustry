package io.anuke.mindustry.type;

import com.badlogic.gdx.utils.Array;

public class WeatherEvent {
    private static final Array<WeatherEvent> all = new Array<>();
    private static int lastid;

    public final int id;
    public final String name;

    public WeatherEvent(String name){
        this.id = lastid ++;
        this.name = name;

        all.add(this);
    }

}
