package mindustry.ui.dialogs;

import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ColorPicker extends BaseDialog{
    private Cons<Color> cons = c -> {};
    Color current = new Color();

    public ColorPicker(){
        super("@pickcolor");
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
            }).colspan(3).padBottom(5);

            float w = 150f;

            t.row();

            t.defaults().padBottom(4).padLeft(8f);
            t.add("R").color(Pal.remove).padLeft(0f);
            t.slider(0f, 1f, 0.01f, current.r, current::r).width(w);
            t.field(String.valueOf((int)(current.r * 255)), TextFieldFilter.digitsOnly, r -> current.r(Strings.parseInt(r) / 255f)).valid(this::valid).width(w / 2f);
            t.row();
            t.add("G").color(Color.lime).padLeft(0f);
            t.slider(0f, 1f, 0.01f, current.g, current::g).width(w);
            t.field(String.valueOf((int)(current.g * 255)), TextFieldFilter.digitsOnly, g -> current.g(Strings.parseInt(g) / 255f)).valid(this::valid).width(w / 2f);
            t.row();
            t.add("B").color(Color.royal).padLeft(0f);
            t.slider(0f, 1f, 0.01f, current.b, current::b).width(w);
            t.field(String.valueOf((int)(current.b * 255)), TextFieldFilter.digitsOnly, b -> current.b(Strings.parseInt(b) / 255f)).valid(this::valid).width(w / 2f);
            t.row();
            if(alpha){
                t.add("A").padLeft(0f);
                t.slider(0f, 1f, 0.01f, current.a, current::a).width(w);
                t.field(String.valueOf((int)(current.a * 255)), TextFieldFilter.digitsOnly, a -> current.a(Strings.parseInt(a) / 255f)).valid(this::valid).width(w / 2f);
                t.row();
            }
        });

        buttons.clear();
        addCloseButton();
        buttons.button("@ok", Icon.ok, () -> {
            cons.get(current);
            hide();
        });
    }

    boolean valid(String s){
        if(s.isEmpty()) return false;
        int val = Strings.parseInt(s);
        return 0f <= val && val <= 255f;
    }
}
