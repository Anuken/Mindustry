package mindustry.entities.effect;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

/** Plays a sound effect when created and simultaneously renders an effect. */
public class SoundEffect extends Effect{
    public Sound sound = Sounds.none;
    public float minPitch = 0.8f;
    public float maxPitch = 1.2f;
    public float minVolume = 1f;
    public float maxVolume = 1f;
    public Effect effect;

    public SoundEffect(){
        startDelay = -1;
    }

    public SoundEffect(Sound sound, Effect effect){
        this();
        this.sound = sound;
        this.effect = effect;
    }

    @Override
    public void init(){
        if(startDelay < 0){
            startDelay = effect.startDelay;
        }
    }

    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;

        if(startDelay > 0){
            Time.run(startDelay, () -> sound.at(x, y, Mathf.random(minPitch, maxPitch), Mathf.random(minVolume, maxVolume)));
        }else{
            sound.at(x, y, Mathf.random(minPitch, maxPitch), Mathf.random(minVolume, maxVolume));
        }
        
        effect.create(x, y, rotation, color, data);
    }
}
