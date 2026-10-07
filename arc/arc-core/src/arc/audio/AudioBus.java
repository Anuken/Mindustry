package arc.audio;

import arc.*;
import arc.util.*;

import static arc.audio.Soloud.*;

public class AudioBus extends AudioSource{
    public int id;

    public AudioBus(){
        if(Core.audio != null && Core.audio.initialized){
            init();
        }
    }

    @Override
    public void setFilter(int index, @Nullable AudioFilter filter){
        if(handle == 0) return;
        sourceFilter(handle, index, filter == null ? 0 : filter.handle);
    }

    AudioBus init(){
        if(handle != 0) return this;
        this.handle = busNew();
        this.id = sourcePlay(handle);
        return this;
    }

    public boolean playing(){
        return handle != 0 && Core.audio.isPlaying(id);
    }

    public void play(){
        if(handle == 0 || idValid(id)) return;
        id = sourcePlay(handle);
    }

    @Override
    public void stop(){
        Core.audio.stop(id);
        id = 0;
    }

    @Override
    public float getLength(){
        return 0;
    }

    public void fadeFilterParam(int filter, int attribute, float value, float timeSec){
        Core.audio.fadeFilterParam(id, filter, attribute, value, timeSec);
    }

    public void setFilterParam(int filter, int attribute, float value){
        Core.audio.setFilterParam(id, filter, attribute, value);
    }

    public void setVolume(float volume){
        Core.audio.setVolume(id, volume);
    }
}
