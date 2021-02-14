package mindustry.logic;

import arc.*;
import arc.func.*;
import arc.scene.ui.TextButton.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.logic.LStatements.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static mindustry.logic.LCanvas.*;

public class LogicDialog extends BaseDialog{
    public LCanvas canvas;
    Cons<String> consumer = s -> {};

    public LogicDialog(){
        super("logic");

        clearChildren();

        canvas = new LCanvas();
        shouldPause = true;

        addCloseListener();

        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide).name("back");

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, t -> {
                    TextButtonStyle style = Styles.cleart;
                    t.defaults().size(280f, 60f).left();

                    t.button("@schematic.copy", Icon.copy, style, () -> {
                        dialog.hide();
                        Core.app.setClipboardText(canvas.save());
                    }).marginLeft(12f);
                    t.row();
                    t.button("@schematic.copy.import", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            canvas.load(Core.app.getClipboardText().replace("\r\n", "\n"));
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null);
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).name("edit");

        buttons.button("@add", Icon.add, () -> {
            BaseDialog dialog = new BaseDialog("@add");
            dialog.cont.pane(t -> {
                t.background(Tex.button);
                int i = 0;
                for(Prov<LStatement> prov : LogicIO.allStatements){
                    LStatement example = prov.get();
                    if(example instanceof InvalidStatement || example.hidden()) continue;

                    TextButtonStyle style = new TextButtonStyle(Styles.cleart);
                    style.fontColor = example.color();
                    style.font = Fonts.outline;

                    t.button(example.name(), style, () -> {
                        canvas.add(prov.get());
                        dialog.hide();
                    }).size(140f, 50f).self(c -> tooltip(c, "lst." + example.name()));
                    if(++i % 2 == 0) t.row();
                }
            });
            dialog.addCloseButton();
            dialog.show();
        }).disabled(t -> canvas.statements.getChildren().size >= LExecutor.maxInstructions);

        add(canvas).grow().name("canvas");

        row();

        add(buttons).growX().name("canvas");

        hidden(() -> consumer.get(canvas.save()));

        onResize(() -> canvas.rebuild());
    }

    public void show(String code, Cons<String> modified){
        canvas.statements.clearChildren();
        canvas.rebuild();
        try{
            canvas.load(code);
        }catch(Throwable t){
            Log.err(t);
            canvas.load("");
        }
        this.consumer = result -> {
            if(!result.equals(code)){
                modified.get(result);
            }
        };

        show();
    }
}
