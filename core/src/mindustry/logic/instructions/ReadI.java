package mindustry.logic.instructions;

import arc.struct.*;
import mindustry.logic.*;

public class ReadI implements LInstruction{
    public LVar target, position, output;

    public ReadI(LVar target, LVar position, LVar output){
        this.target = target;
        this.position = position;
        this.output = output;
    }

    public ReadI(){
    }

    @Override
    public void run(LExecutor exec){
        Object targetObj = target.obj();
        if(targetObj instanceof LReadable read){
            if(!read.readable(exec)){
                output.setobj(null);
                return;
            }
            read.read(position, output);
        }else{
            int address = position.numi();
            if(targetObj instanceof CharSequence str){
                output.setnum(address < 0 || address >= str.length() ? Double.NaN : (int)str.charAt(address));
            }else if(targetObj instanceof Seq<?> seq){
                output.setobj(address < 0 || address >= seq.size ? null : seq.get(address));
            }else{
                output.setobj(null);
            }
        }
    }
}
