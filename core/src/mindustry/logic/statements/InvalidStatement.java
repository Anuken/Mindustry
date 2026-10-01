package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("noop")
public class InvalidStatement extends LogicStatement{

    @Override
    public void build(Table table){
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new NoopI();
    }
}
