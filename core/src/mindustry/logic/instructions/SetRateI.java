package mindustry.logic.instructions;

import arc.math.*;
import mindustry.logic.*;
import mindustry.world.blocks.logic.*;

public class SetRateI implements LogicInstruction{
    public LogicVar amount;

    public SetRateI(LogicVar amount){
        this.amount = amount;
    }

    public SetRateI(){
    }

    @Override
    public void run(LogicExecutor exec){
        if(exec.build == null) return;
        exec.build.ipt = Mathf.clamp(amount.numi(), 1, exec.build.block.privileged ? ((LogicBlock)exec.build.block).maxInstructionsPerTick : ((LogicBlock)exec.build.block).instructionsPerTick);
        if(exec.ipt != null){
            exec.ipt.numval = exec.build.ipt;
        }
    }
}
