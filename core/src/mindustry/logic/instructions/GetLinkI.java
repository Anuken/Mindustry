package mindustry.logic.instructions;

import mindustry.logic.*;

public class GetLinkI implements LInstruction{
    public LVar output, index;

    public GetLinkI(LVar output, LVar index){
        this.index = index;
        this.output = output;
    }

    public GetLinkI(){
    }

    @Override
    public void run(LExecutor exec){
        int address = index.numi();

        output.setobj(address >= 0 && address < exec.links.length ? exec.links[address] : null);
    }
}
