package mindustry.logic.instructions;

import mindustry.logic.*;

public class FormatI implements LogicInstruction{
    public LogicVar value;

    public FormatI(LogicVar value){
        this.value = value;
    }

    FormatI(){
    }

    @Override
    public void run(LogicExecutor exec){

        int placeholderIndex = -1;
        int placeholderNumber = 10;

        for(int i = 0; i < exec.textBuffer.length(); i++){
            if(exec.textBuffer.charAt(i) == '{' && exec.textBuffer.length() - i > 2){
                char numChar = exec.textBuffer.charAt(i + 1);

                if(numChar >= '0' && numChar <= '9' && exec.textBuffer.charAt(i + 2) == '}'){
                    if(numChar - '0' < placeholderNumber){
                        placeholderNumber = numChar - '0';
                        placeholderIndex = i;
                    }
                }
            }
        }

        if(placeholderIndex == -1) return;

        //this should avoid any garbage allocation
        if(value.isobj){
            String strValue = PrintI.toString(value.objval);

            exec.textBuffer.replace(placeholderIndex, placeholderIndex + 3, strValue);
        }else{
            //display integer version when possible
            if(Math.abs(value.numval - Math.round(value.numval)) < 0.00001){
                exec.textBuffer.replace(placeholderIndex, placeholderIndex + 3, Math.round(value.numval) + "");
            }else{
                exec.textBuffer.replace(placeholderIndex, placeholderIndex + 3, value.numval + "");
            }
        }

        if(exec.textBuffer.length() > LogicExecutor.maxTextBuffer){
            exec.textBuffer.setLength(LogicExecutor.maxTextBuffer);
        }
    }
}
