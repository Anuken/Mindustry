package mindustry.ui.dialogs;

import arc.func.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.utils.*;
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
            }).colspan(2).padBottom(5);

            float w = 150f;

            t.row();

            TextField hField = new TextField(current.toString());
            hField.setValidator(h -> h.length() == 6 || h.length() == 8); //TODO: Check for valid hex. Somehow. Anuke help.

            t.defaults().padBottom(4);
            t.add("R").color(Pal.remove);
            Slider rs = t.slider(0f, 1f, 0.01f, current.r, r -> {
                current.r(r);
                hField.setText(current.toString());
            }).width(w).get();
            t.row();
            t.add("G").color(Color.lime);
            Slider gs = t.slider(0f, 1f, 0.01f, current.g, g -> {
                current.g(g);
                hField.setText(current.toString());
            }).width(w).get();
            t.row();
            t.add("B").color(Color.royal);
            Slider bs = t.slider(0f, 1f, 0.01f, current.b, b -> {
                current.b(b);
                hField.setText(current.toString());
            }).width(w).get();
            t.row();
            Slider as = null;
            if(alpha){
                t.add("A");
                as = t.slider(0f, 1f, 0.01f, current.a, a -> {
                    current.a(a);
                    hField.setText(current.toString());
                }).width(w).get();
                t.row();
            }
            t.add("Hex");
            t.add(hField);
            Slider javaWhy = as; //mUsT bE fInAl Or EfFeCtIvElY fInAl
            hField.changed(() -> {
                if(hField.isValid()){
                    Color.valueOf(current, hField.getText());
                    rs.setValue(current.r);
                    bs.setValue(current.b);
                    gs.setValue(current.g);
                    if(alpha) javaWhy.setValue(current.a);
                }
            });
        });

        buttons.clear();
        addCloseButton();
        buttons.button("@ok", Icon.ok, () -> {
            cons.get(current);
            hide();
        });
    }
}
