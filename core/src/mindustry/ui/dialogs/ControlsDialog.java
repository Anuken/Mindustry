package mindustry.ui.dialogs;

import arc.input.*;
import arc.scene.ui.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class ControlsDialog extends KeybindDialog{

    public ControlsDialog(){
        setFillParent(true);
        title.setAlignment(Align.center);
        titleTable.row();
        titleTable.add(new Image()).growX().height(3f).pad(4f).get().setColor(Pal.accent);
    }

    @Override
    public void addCloseButton(){
        buttons.button("$back", Icon.left, this::hide).size(230f, 64f);

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back) hide();
        });
    }
}
