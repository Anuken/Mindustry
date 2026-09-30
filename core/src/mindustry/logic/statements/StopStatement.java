package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("stop")
public class StopStatement extends LogicStatement{

    @Override
    public void build(Table table){
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new StopI();
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.control;
    }
}
