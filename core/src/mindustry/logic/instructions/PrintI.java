package mindustry.logic.instructions;

import mindustry.game.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;

public class PrintI implements LInstruction{
    public LVar value;

    public PrintI(LVar value){
        this.value = value;
    }

    PrintI(){
    }

    @Override
    public void run(LExecutor exec){

        if(exec.textBuffer.length() >= LExecutor.maxTextBuffer) return;

        //this should avoid any garbage allocation
        if(value.isobj){
            String strValue = toString(value.objval);

            exec.textBuffer.append(strValue, 0, Math.min(strValue.length(), LExecutor.maxTextBuffer - exec.textBuffer.length()));
        }else{
            //display integer version when possible
            if(Math.abs(value.numval - Math.round(value.numval)) < 0.00001){
                exec.textBuffer.append(Math.round(value.numval));
            }else{
                exec.textBuffer.append(value.numval);
            }
        }
    }

    public static String toString(Object obj){
        return
        obj == null ? "null" :
        obj instanceof String s ? s :
        obj instanceof MappableContent content ? content.name :
        obj instanceof Content ? "[content]" :
        obj instanceof Building build ? build.block.name :
        obj instanceof Unit unit ? unit.type.name :
        obj instanceof Enum<?> e ? e.name() :
        obj instanceof Team team ? team.name :
        "[object]";
    }
}
