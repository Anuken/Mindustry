package mindustry.logic.statements;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.annotations.Annotations.*;
import mindustry.logic.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.instructions.*;
import mindustry.ui.*;

import static mindustry.logic.LCanvas.*;

@RegisterStatement("jump")
public class JumpStatement extends LStatement{
    private static Color last = new Color();

    public transient StatementElem dest;

    public int destIndex;

    public ConditionOp op = ConditionOp.notEqual;
    public String value = "x", compare = "false";

    @Override
    public boolean useWrapping(){
        return false;
    }

    @Override
    public void build(Table table){
        table.add(bundle("if")).padLeft(4);

        last = table.color;
        table.table(this::rebuild);

        table.add().growX();
        table.add(new JumpButton(() -> dest, s -> dest = s, this.elem)).size(30).right().padRight(-8f);

        String name = localizedName();

        //hack way of finding the title label...
        Core.app.post(() -> {
            //must be delayed because parent is added later
            if(table.parent != null){
                Label title = table.parent.find("statement-name");
                if(title != null){
                    title.update(() -> title.setText((dest != null ? name + " -> " + dest.index : name)));
                }
            }
        });

    }

    void rebuild(Table table){
        table.clearChildren();
        table.setColor(last);

        addOp(this, table, op, o -> {
            op = o;
            rebuild(table);
        }, value, str -> value = str, compare, str -> compare = str);
    }

    public static void addOp(LStatement st, Table t, ConditionOp op, Cons<ConditionOp> getter, String comp0, Cons<String> set0, String comp1, Cons<String> set2){
        float w = !isCompact() ? 180f : 140f;

        if(op != ConditionOp.always) st.field(t, comp0, set0).width(w);

        t.button(b -> {
            b.add(selectTranslate(op.symbol));
            b.clicked(() -> st.showSelect(b, ConditionOp.all, op, getter, 3, c -> c.width(95f)));
        }, Styles.logict, () -> {
        }).size(op == ConditionOp.always ? 90f : 48f, 40f).pad(4f).color(t.color);

        if(op != ConditionOp.always) st.field(t, comp1, set2).width(w);
    }

    //elements need separate conversion logic
    @Override
    public void setupUI(){
        if(elem != null && destIndex >= 0 && destIndex < elem.parent.getChildren().size){
            dest = (StatementElem)elem.parent.getChildren().get(destIndex);
        }
    }

    @Override
    public void saveUI(){
        if(elem != null){
            destIndex = dest == null ? -1 : dest.parent.getChildren().indexOf(dest);
        }
    }

    @Override
    public LInstruction build(LAssembler builder){
        return new JumpI(op, builder.var(value), builder.var(compare), destIndex);
    }

    @Override
    public LCategory category(){
        return LCategory.control;
    }
}
