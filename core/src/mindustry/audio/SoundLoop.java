package mindustry.audio;

import arc.audio.*;
import arc.math.*;
import arc.util.*;

/** A simple class for playing a looping sound at a position.*/
public class SoundLoop{
    private static final float fadeSpeed = 0.05f;

    private final Sound sound;
    private int id = -1;
    private float volume, baseVolume;

    public SoundLoop(Sound sound, float baseVolume){
        this.sound = sound;
        this.baseVolume = baseVolume;
    }

    public void update(float x, float y, boolean play){
        if(baseVolume < 0) return;

        if(id < 0){
            if(play){
                id = sound.loop(sound.calcVolume(x, y) * volume * baseVolume, 1f, sound.calcPan(x, y));
            }
        }else{
            //fade the sound in or out
            if(play){
                volume = Mathf.clamp(volume + fadeSpeed * Time.delta);
            }else{
                volume = Mathf.clamp(volume - fadeSpeed * Time.delta);
                if(volume <= 0.001f){
                    sound.stop(id);
                    id = -1;
                    return;
                }
            }
            sound.setPan(id, sound.calcPan(x, y), sound.calcVolume(x, y) * volume * baseVolume);
        }
    }

    public void stop(){
        if(id != -1){
            sound.stop(id);
            id = -1;
            volume = baseVolume = -1;
        }
    }
}
