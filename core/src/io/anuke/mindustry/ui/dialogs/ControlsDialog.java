package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.input.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;

public class ControlsDialog extends KeybindDialog{

    public ControlsDialog(){
        setFillParent(true);
        title.setAlignment(Align.center);
        titleTable.row();
        titleTable.add(new Image()).growX().height(3f).pad(4f).get().setColor(Pal.accent);
    }

    @Override
    public void addCloseButton(){
        buttons.addImageTextButton("$back", Icon.arrowLeftSmall, this::hide).size(230f, 64f);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK)
                hide();
        });
    }
}
