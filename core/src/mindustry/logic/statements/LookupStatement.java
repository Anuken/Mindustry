package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.type.*;
import mindustry.ui.*;

@RegisterStatement("lookup")
public class LookupStatement extends LogicStatement{
    public ContentType type = ContentType.item;
    public String result = "result", id = "0";

    @Override
    public void build(Table table){
        fields(table, result, str -> result = str).width(120f);

        table.add(bundle("-lookup"));

        table.button(b -> {
            b.label(() -> bundle(type));
            b.clicked(() -> showSelect(b, GlobalVars.lookableContent, type, o -> {
                type = o;
            }));
        }, Styles.logict, () -> {
        }).size(64f, 40f).pad(4f).color(table.color);

        table.add(" # ");

        fields(table, id, str -> id = str);
    }

    @Override
    public LogicInstruction build(LogicAssembler builder){
        return new LookupI(builder.var(result), builder.var(id), type);
    }

    @Override
    public LogicCategory category(){
        return LogicCategory.operation;
    }
}
