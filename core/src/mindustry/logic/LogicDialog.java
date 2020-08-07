package mindustry.logic;

import arc.scene.ui.layout.*;
import mindustry.ui.dialogs.*;

public class LogicDialog extends BaseDialog{
    LogicCanvas canvas;

    public LogicDialog(){
        super("logic");

        canvas = new LogicCanvas();
        addCloseButton();

        clear();
        stack(canvas, new Table(t -> {
            t.bottom();
            t.add(buttons);
        })).grow();
    }
}
