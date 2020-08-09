package mindustry.logic;

import arc.*;
import arc.func.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class LogicDialog extends BaseDialog{
    LCanvas canvas;
    Cons<String> consumer = s -> Log.info(s);

    public LogicDialog(){
        super("logic");

        clearChildren();

        canvas = new LCanvas();
        addCloseButton();

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButtonStyle style = Styles.cleart;
                    t.defaults().size(280f, 60f).left();
                    t.row();
                    t.button("@schematic.copy.import", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            canvas.load(Core.app.getClipboardText());
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                    t.row();
                    t.button("@schematic.copy", Icon.copy, style, () -> {
                        dialog.hide();
                        Core.app.setClipboardText(canvas.save());
                    }).marginLeft(12f);
                });
            });

            dialog.addCloseButton();
            dialog.show();
        });

        stack(canvas, new Table(t -> {
            t.bottom();
            t.add(buttons);
        })).grow();

        hidden(() -> {
            consumer.get(canvas.save());
        });
    }

    public void show(String code, Cons<String> consumer){
        try{
            canvas.load(code);
        }catch(Throwable t){
            t.printStackTrace();
            canvas.load("");
        }
        this.consumer = consumer;

        show();
    }
}
