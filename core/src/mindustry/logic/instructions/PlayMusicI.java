package mindustry.logic.instructions;

import arc.audio.*;
import mindustry.audio.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class PlayMusicI implements LInstruction{
    public LVar name, interrupt;

    public PlayMusicI(){
    }

    public PlayMusicI(LVar name, LVar interrupt){
        this.name = name;
        this.interrupt = interrupt;
    }

    @Override
    public void run(LExecutor exec){
        if(headless) return;

        //null music = stop
        Music music = SoundControl.findMusic(PrintI.toString(name.obj()));
        control.sound.playMusic(music, interrupt.bool());
    }
}
