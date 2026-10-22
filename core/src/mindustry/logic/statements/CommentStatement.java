package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.logic.*;
import mindustry.ui.*;

//TODO broken
//@RegisterStatement("#")
public class CommentStatement extends LogicStatement{
    public String comment = "";

    @Override
    public void build(Table table){
        table.area(comment, Styles.nodeArea, v -> comment = v).growX().height(90f).padLeft(2).padRight(6).color(table.color);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return null;
    }
}
