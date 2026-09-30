package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;

@RegisterStatement("end")
public class EndStatement extends LStatement{
    @Override
    public void build(Table table){

    }

    @Override
    public LInstruction build(LAssembler builder){
        return new EndI();
    }

    @Override
    public LCategory category(){
        return LCategory.control;
    }
}
