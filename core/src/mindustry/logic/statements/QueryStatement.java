package mindustry.logic.statements;

import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

@RegisterStatement("query")
public class QueryStatement extends LStatement{
    public QueryShape shape = QueryShape.circle;
    public QueryType type = QueryType.unit;
    public String team = "null", x = "0", y = "0", w = "10", h = "10";

    @Override
    public void build(Table table){
        table.clearChildren();

        table.button(shape == QueryShape.circle ? bundle("circle") : bundle("rect"), Styles.logict, () -> {
            shape = shape == QueryShape.circle ? QueryShape.rect : QueryShape.circle;
            build(table);
        }).size(80f, 40f).pad(4f).color(table.color);

        table.button(b -> {
            b.label(() -> bundle(type));
            b.clicked(() -> showSelect(b, QueryType.queryable, type, o -> {
                type = o;
                build(table);
            }, 4, c -> c.width(100f)));
        }, Styles.logict, () -> {
        }).size(100f, 40f).pad(4f).color(table.color);

        fields(table, "team", team, str -> team = str);

        fields(table, "x", x, str -> x = str);
        fields(table, "y", y, str -> y = str);

        if(shape == QueryShape.circle){
            fields(table, "radius", w, str -> w = str);
        }else{
            fields(table, "width", w, str -> w = str);
            fields(table, "height", h, str -> h = str);
        }
    }

    @Override
    public boolean privileged(){
        return true;
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new QueryI(shape, type, builder.var(team), builder.var(x), builder.var(y), builder.var(w), builder.var(h));
    }

    @Override
    public LCategory category(){
        return LCategory.world;
    }
}
