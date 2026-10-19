package mindustry.logic.instructions;

import mindustry.logic.*;

import static mindustry.Vars.*;

public class LocalePrintI implements LogicInstruction{
    public LogicVar name;

    public LocalePrintI(LogicVar name){
        this.name = name;
    }

    public LocalePrintI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(exec.textBuffer.length() >= LogicExecutor.maxTextBuffer) return;

        //this should avoid any garbage allocation
        if(name.isobj){
            String name = PrintI.toString(this.name.objval);

            String strValue;

            if(mobile){
                strValue = state.mapLocales.containsProperty(name + ".mobile") ?
                state.mapLocales.getProperty(name + ".mobile") :
                state.mapLocales.getProperty(name);
            }else{
                strValue = state.mapLocales.getProperty(name);
            }

            exec.textBuffer.append(strValue);
        }
    }
}
