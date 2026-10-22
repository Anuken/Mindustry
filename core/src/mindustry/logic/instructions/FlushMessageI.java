package mindustry.logic.instructions;

import arc.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

public class FlushMessageI implements LogicInstruction{
    public MessageType type = MessageType.announce;
    public LogicVar duration, outSuccess;

    public FlushMessageI(MessageType type, LogicVar duration, LogicVar outSuccess){
        this.type = type;
        this.duration = duration;
        this.outSuccess = outSuccess;
    }

    public FlushMessageI(){
    }

    @Override
    public void run(LogicExecutor exec){
        //set default to success
        outSuccess.setnum(1);
        if(headless && type != MessageType.mission){
            exec.textBuffer.setLength(0);
            return;
        }

        if(
        type == MessageType.announce && ui.hasAnnouncement() ||
        type == MessageType.notify && ui.hudfrag.hasToast() ||
        type == MessageType.toast && ui.hasAnnouncement()
        ){
            //backwards compatibility; if it is @wait, block execution
            if(outSuccess == logicVars.waitVar()){
                exec.counter.numval--;
                exec.yield = true;
            }else{
                //set outSuccess=false to let user retry.
                outSuccess.setnum(0);
            }
            return;
        }

        String text = UI.formatIcons(exec.textBuffer.toString());
        if(text.startsWith("@")){
            String substr = text.substring(1);
            if(Core.bundle.has(substr)){
                text = Core.bundle.get(substr);
            }
        }

        switch(type){
            case notify -> ui.hudfrag.showToast(Icon.info, text);
            case announce -> ui.announce(text, duration.numf());
            case toast -> ui.showInfoToast(text, duration.numf());
            //TODO desync?
            case mission -> state.rules.mission = text;
        }

        exec.textBuffer.setLength(0);
    }
}
