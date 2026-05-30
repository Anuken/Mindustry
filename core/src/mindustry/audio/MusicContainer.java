package mindustry.audio;

import arc.audio.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;

/** In serialization contexts, data assets may not be loaded yet. This provides a way to lazily get music from the asset manager on demand. */
public class MusicContainer implements JsonSerializable{
    public String name = "";

    private @Nullable Music cached;
    private boolean accessed;

    public MusicContainer(String name){
        this.name = name;
    }

    public MusicContainer(){

    }

    public Music get(){
        if(accessed) return cached;
        accessed = true;

        return cached = SoundControl.findMusic(name);
    }

    @Override
    public void write(Json json){
        json.writeValue(name);
    }

    @Override
    public void read(Json json, JsonValue jsonData){
        name = jsonData.asString();
    }
}
