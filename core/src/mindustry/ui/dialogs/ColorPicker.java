package mindustry.ui.dialogs;

import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ColorPicker extends BaseDialog{
    private Cons<Color> cons = c -> {};
    private Color current = new Color();

    public ColorPicker(){
        super("$pickcolor");
    }

    public void show(Color color, Cons<Color> consumer){
        show(color, true, consumer);
    }

    public void show(Color color, boolean alpha, Cons<Color> consumer){
        this.current.set(color);
        this.cons = consumer;
        show();

        cont.clear();
        cont.pane(t -> {
            t.table(Tex.pane, i -> {
                i.stack(new Image(Tex.alphaBg), new Image(){{
                    setColor(current);
                    update(() -> setColor(current));
                }}).size(200f);
            }).colspan(2).padBottom(5);

            float w = 150f;

            t.row();

            t.defaults().padBottom(4);
            t.add("R").color(Pal.remove);
            t.slider(0f, 1f, 0.01f, current.r, current::r).width(w);
            t.row();
            t.add("G").color(Color.lime);
            t.slider(0f, 1f, 0.01f, current.g, current::g).width(w);
            t.row();
            t.add("B").color(Color.royal);
            t.slider(0f, 1f, 0.01f, current.b, current::b).width(w);
            t.row();
            if(alpha){
                t.add("A");
                t.slider(0f, 1f, 0.01f, current.a, current::a).width(w);
                t.row();
            }
        });

        buttons.clear();
        addCloseButton();
        buttons.button("$ok", Icon.ok, () -> {
            cons.get(current);
            hide();
        });
    }
}
