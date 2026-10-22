package mindustry.logic.instructions;

import arc.audio.*;
import mindustry.audio.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class PlayMusicI implements LogicInstruction{
    public LogicVar name, interrupt;

    public PlayMusicI(){
    }

    public PlayMusicI(LogicVar name, LogicVar interrupt){
        this.name = name;
        this.interrupt = interrupt;
    }

    @Override
    public void run(LogicExecutor exec){
        if(headless) return;

        //null music = stop
        Music music = SoundControl.findMusic(PrintI.toString(name.obj()));
        control.sound.playMusic(music, interrupt.bool());
    }
}
