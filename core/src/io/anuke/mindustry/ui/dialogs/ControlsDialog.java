package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.KeybindDialog;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.graphics.Pal;

public class ControlsDialog extends KeybindDialog{

    public ControlsDialog(){
        setStyle(Core.scene.skin.get("dialog", WindowStyle.class));

        setFillParent(true);
        title.setAlignment(Align.center);
        titleTable.row();
        titleTable.add(new Image("white"))
        .growX().height(3f).pad(4f).get().setColor(Pal.accent);
    }

    @Override
    public void addCloseButton(){
        buttons.addImageTextButton("$back", "icon-arrow-left", 30f, this::hide).size(230f, 64f);

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK)
                hide();
        });
    }
}
