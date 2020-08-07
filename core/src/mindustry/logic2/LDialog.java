package mindustry.logic2;

import arc.scene.ui.layout.*;
import mindustry.ui.dialogs.*;

public class LDialog extends BaseDialog{
    LCanvas canvas;

    public LDialog(){
        super("logic");

        canvas = new LCanvas();
        addCloseButton();

        clear();
        stack(canvas, new Table(t -> {
            t.bottom();
            t.add(buttons);
        })).grow();
    }
}
