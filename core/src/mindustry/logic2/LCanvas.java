package mindustry.logic2;

import arc.input.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.logic2.LStatements.*;
import mindustry.ui.*;

public class LCanvas extends Table{

    private Table statements;
    private Seq<StatementElem> elems = new Seq<>();

    public LCanvas(){
        //left();

        pane(t -> {
            t.setClip(false);
            statements = t;
            statements.defaults().width(300f).pad(4f);
            t.marginRight(1);
        }).growY().get().setClip(false);

        add(new AssignStatement());
        add(new AssignStatement());
        add(new OpStatement());
    }

    void add(LStatement statement){
        StatementElem e = new StatementElem(statement);
        elems.add(e);

        statements.add(e);
        statements.row();
    }

    public class StatementElem extends Table{
        LStatement st;

        public StatementElem(LStatement st){
            this.st = st;

            background(Tex.whitePane);
            setColor(st.category().color);
            margin(0f);

            table(Tex.whiteui, t -> {
                t.color.set(color);

                t.margin(8f);
                t.touchable = Touchable.enabled;

                t.add(st.name()).style(Styles.outlineLabel).color(color).padRight(8);
                t.add().growX();
                t.button(Icon.cancel, Styles.onlyi, () -> {
                    //TODO disconnect things
                    remove();
                    elems.remove(this);
                });

                t.addListener(new InputListener(){
                    float lastx, lasty;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                        Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));
                        lastx = v.x;
                        lasty = v.y;
                        toFront();
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer){
                        Vec2 v = localToStageCoordinates(Tmp.v1.set(x, y));

                        translation.add(v.x - lastx, v.y - lasty);
                        lastx = v.x;
                        lasty = v.y;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                        translation.setZero();
                    }
                });
            }).growX().padBottom(6);

            row();

            table(t -> {
                t.setColor(color);
                st.build(t);
            }).pad(8).padTop(2);

            marginBottom(7);
        }
    }
}
