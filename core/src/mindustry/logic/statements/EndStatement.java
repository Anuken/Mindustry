package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("end")
public class EndStatement extends LogicStatement{
    @Override
    public void build(Table table){

    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new EndI();
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.control;
    }
}
