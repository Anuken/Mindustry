package mindustry.logic.instructions;

import arc.*;
import arc.audio.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.logic.*;

public class PlaySoundI implements LogicInstruction{
    public boolean positional;
    public LogicVar id, volume, pitch, pan, x, y, limit;

    public PlaySoundI(){
    }

    public PlaySoundI(boolean positional, LogicVar id, LogicVar volume, LogicVar pitch, LogicVar pan, LogicVar x, LogicVar y, LogicVar limit){
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
    public void run(LogicExecutor exec){
        Sound sound = Sounds.getSound(id.numi());
        if(sound == null) sound = Sounds.none;

        if(positional){
            sound.at(World.unconv(x.numf()), World.unconv(y.numf()), pitch.numf(), Math.min(volume.numf(), 2f), limit.bool());
        }else{
            sound.play(Math.min(volume.numf() * Core.audio.sfxVolume, 2f), pitch.numf(), pan.numf(), false, limit.bool());
        }
    }
}
