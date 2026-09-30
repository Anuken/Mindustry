package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("stop")
public class StopStatement extends LStatement{

    @Override
    public void build(Table table){
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new StopI();
    }

    @Override
    public LCategory category(){
        return LCategory.control;
    }
}
