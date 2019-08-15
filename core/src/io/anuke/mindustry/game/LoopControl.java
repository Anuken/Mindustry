package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.mindustry.*;

public class LoopControl{
    private ObjectMap<Sound, SoundData> sounds = new ObjectMap<>();

    public void play(Sound sound, Position pos, float volume){
        if(Vars.headless) return;

        float baseVol = sound.calcFalloff(pos.getX(), pos.getY());
        float vol = baseVol * volume;

        SoundData data = sounds.getOr(sound, SoundData::new);
        data.volume += vol;
        data.volume = Mathf.clamp(data.volume, 0f, 1f);
        data.total += baseVol;
        data.sum.add(pos.getX() * baseVol, pos.getY() * baseVol);
    }

    public void update(){
        float avol = Core.settings.getInt("ambientvol", 100) / 100f;

        sounds.each((sound, data) -> {
            data.curVolume = Mathf.lerpDelta(data.curVolume, data.volume * avol, 0.2f);

            boolean play = data.curVolume > 0.01f;
            float pan = Mathf.isZero(data.total, 0.0001f) ? 0f : sound.calcPan(data.sum.x / data.total, data.sum.y / data.total);
            if(data.soundID <= 0){
                if(play){
                    data.soundID = sound.loop(data.curVolume, 1f, pan);
                }
            }else{
                if(data.curVolume <= 0.01f){
                    sound.stop(data.soundID);
                    data.soundID = -1;
                    return;
                }
                sound.setPan(data.soundID, pan, data.curVolume);
            }

            data.volume = 0f;
            data.total = 0f;
            data.sum.setZero();
        });
    }

    private class SoundData{
        float volume;
        float total;
        Vector2 sum = new Vector2();

        int soundID;
        float curVolume;
    }
}
