package arc.audio;

import arc.files.*;
import arc.util.*;

/** Plays a sound from an array at random. */
public class RandomSound extends Sound{
    public Sound[] sounds = {};

    public RandomSound(Sound... sounds){
        this.sounds = sounds;
    }

    public RandomSound(){
    }

    @Override
    public void load(Fi file){}

    @Override
    public int play(float volume, float pitch, float pan, boolean loop, boolean checkFrame, AudioBus bus){
        if(sounds.length > 0){
            return Structs.random(sounds).play(volume, pitch, pan, loop, checkFrame, bus);
        }
        return -1;
    }
}
