package mindustry.audio;

import arc.audio.*;
import arc.util.*;

/** In serialization contexts, data assets may not be loaded yet. This provides a way to lazily get music from the asset manager on demand. */
public class MusicContainer{
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
}
