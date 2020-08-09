package mindustry.logic;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.logic.LCanvas.*;
import mindustry.logic.LExecutor.*;
import mindustry.ui.*;

/**
 * A statement is an intermediate representation of an instruction, to be used mostly in UI.
 * Contains all relevant variable information. */
public abstract class LStatement{
    public transient @Nullable StatementElem elem;

    public abstract void build(Table table);
    public abstract LCategory category();
    public abstract LInstruction build(LAssembler builder);

    protected void field(Table table, String value, Cons<String> setter){
        table.field(value, Styles.nodeField, setter)
            .size(130f, 40f).pad(2f).color(table.color);
    }

    protected <T> void showSelect(Button b, T[] values, T current, Cons<T> getter){
        Table t = new Table(Tex.button);

        //triggers events behind the element to simulate deselection
        Element hitter = new Element();

        Runnable hide = () -> {
            Core.app.post(hitter::remove);
            t.actions(Actions.fadeOut(0.3f, Interp.fade), Actions.remove());
        };

        hitter.fillParent = true;
        hitter.tapped(hide);
        Core.scene.add(hitter);

        Core.scene.add(t);

        t.update(() -> {
            b.localToStageCoordinates(Tmp.v1.set(b.getWidth()/2f, b.getHeight()/2f));
            Tmp.v1.clamp(0, 0, Core.graphics.getWidth() - b.getWidth(), Core.graphics.getHeight() - b.getHeight());
            t.setPosition(Tmp.v1.x, Tmp.v1.y, Align.center);
        });
        t.actions(Actions.alpha(0), Actions.fadeIn(0.3f, Interp.fade));

        ButtonGroup<Button> group = new ButtonGroup<>();
        int i = 0;
        t.defaults().size(56f, 40f);

        for(T p : values){
            t.button(p.toString(), Styles.clearTogglet, () -> {
                getter.get(p);
                hide.run();
            }).checked(current == p).group(group);

            if(++i % 4 == 0) t.row();
        }

        t.pack();
    }

    public void write(StringBuilder builder){
        LogicIO.write(this,builder);
    }

    public void setupUI(){

    }

    public void saveUI(){

    }

    public String name(){
        return getClass().getSimpleName().replace("Statement", "");
    }
}
