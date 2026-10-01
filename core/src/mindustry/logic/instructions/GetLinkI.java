package mindustry.logic.instructions;

import mindustry.logic.*;

public class GetLinkI implements LogicInstruction{
    public LogicVar output, index;

    public GetLinkI(LogicVar output, LogicVar index){
        this.index = index;
        this.output = output;
    }

    public GetLinkI(){
    }

    @Override
    public void run(LogicExecutor exec){
        int address = index.numi();

        output.setobj(address >= 0 && address < exec.links.length ? exec.links[address] : null);
    }
}
