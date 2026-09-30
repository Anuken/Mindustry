package mindustry.logic.instructions;

import mindustry.logic.*;

import static mindustry.Vars.*;

public class LocalePrintI implements LInstruction{
    public LVar name;

    public LocalePrintI(LVar name){
        this.name = name;
    }

    public LocalePrintI(){
    }

    @Override
    public void run(LExecutor exec){
        if(exec.textBuffer.length() >= LExecutor.maxTextBuffer) return;

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
