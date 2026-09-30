package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("noop")
public class InvalidStatement extends LStatement{

    @Override
    public void build(Table table){
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new NoopI();
    }
}
