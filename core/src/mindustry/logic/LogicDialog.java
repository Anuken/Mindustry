package mindustry.logic;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.dialogs.*;

public class LogicDialog extends BaseDialog{
    LCanvas canvas;
    Cons<String> consumer = s -> Log.info(s);

    public LogicDialog(){
        super("logic");

        clearChildren();

        canvas = new LCanvas();
        addCloseButton();

        stack(canvas, new Table(t -> {
            t.bottom();
            t.add(buttons);
        })).grow();

        hidden(() -> {
            consumer.get(canvas.save());
        });
    }

    public void show(String code, Cons<String> consumer){
        canvas.load(code);
        this.consumer = consumer;

        show();
    }
}
