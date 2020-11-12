package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.*;

public class LoopControl{
    private ObjectMap<Sound, SoundData> sounds = new ObjectMap<>();

    public void play(Sound sound, float volume){
        if(Vars.headless) return;

        play(sound, Core.camera.position, volume);
    }

    public void play(Sound sound, Position pos, float volume){
        if(Vars.headless) return;

        float baseVol = sound.calcFalloff(pos.getX(), pos.getY());
        float vol = baseVol * volume;

        SoundData data = sounds.get(sound, SoundData::new);
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
            float pan = Mathf.zero(data.total, 0.0001f) ? 0f : sound.calcPan(data.sum.x / data.total, data.sum.y / data.total);
            if(data.soundID <= 0 || !sound.isPlaying(data.soundID)){
                if(play){
                    data.soundID = sound.loop(data.curVolume, 1f, pan);
                }
            }else{
                if(data.curVolume <= 0.001f){
                    sound.stop();
                    data.soundID = -1;
                    return;
                }
                sound.set(data.soundID, pan, data.curVolume);
            }

            data.volume = 0f;
            data.total = 0f;
            data.sum.setZero();
        });
    }

    private static class SoundData{
        float volume;
        float total;
        Vec2 sum = new Vec2();

        int soundID;
        float curVolume;
    }
}
