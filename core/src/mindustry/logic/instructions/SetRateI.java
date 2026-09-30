package mindustry.logic.instructions;

import arc.math.*;
import mindustry.logic.*;
import mindustry.world.blocks.logic.*;

public class SetRateI implements LInstruction{
    public LVar amount;

    public SetRateI(LVar amount){
        this.amount = amount;
    }

    public SetRateI(){
    }

    @Override
    public void run(LExecutor exec){
        if(exec.build == null) return;
        exec.build.ipt = Mathf.clamp(amount.numi(), 1, exec.build.block.privileged ? ((LogicBlock)exec.build.block).maxInstructionsPerTick : ((LogicBlock)exec.build.block).instructionsPerTick);
        if(exec.ipt != null){
            exec.ipt.numval = exec.build.ipt;
        }
    }
}
