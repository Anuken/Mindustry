package mindustry.logic;

import mindustry.ui.dialogs.*;

public class LogicDialog extends BaseDialog{
    LogicCanvas canvas;

    public LogicDialog(){
        super("logic");

        canvas = new LogicCanvas();

        clear();
        add(canvas).grow();
    }
}
