package mindustry.logic.instructions;

import arc.*;
import arc.audio.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.logic.*;

public class PlaySoundI implements LInstruction{
    public boolean positional;
    public LVar id, volume, pitch, pan, x, y, limit;

    public PlaySoundI(){
    }

    public PlaySoundI(boolean positional, LVar id, LVar volume, LVar pitch, LVar pan, LVar x, LVar y, LVar limit){
        this.positional = positional;
        this.id = id;
        this.volume = volume;
        this.pitch = pitch;
        this.pan = pan;
        this.x = x;
        this.y = y;
        this.limit = limit;
    }

    @Override
    public void run(LExecutor exec){
        Sound sound = Sounds.getSound(id.numi());
        if(sound == null) sound = Sounds.none;

        if(positional){
            sound.at(World.unconv(x.numf()), World.unconv(y.numf()), pitch.numf(), Math.min(volume.numf(), 2f), limit.bool());
        }else{
            sound.play(Math.min(volume.numf() * Core.audio.sfxVolume, 2f), pitch.numf(), pan.numf(), false, limit.bool());
        }
    }
}
