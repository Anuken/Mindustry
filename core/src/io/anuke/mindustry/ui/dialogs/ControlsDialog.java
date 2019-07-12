package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.graphics.Pal;

public class ControlsDialog extends KeybindDialog{

    public ControlsDialog(){
        setStyle(Core.scene.skin.get("dialog", WindowStyle.class));

        setFillParent(true);
        title.setAlignment(Align.center);
        titleTable.row();
        titleTable.add(new Image("whiteui"))
        .growX().height(3f).pad(4f).get().setColor(Pal.accent);
        if(Vars.mobile){
            cont.row();
            cont.add("$keybinds.mobile")
            .center().growX().wrap().get().setAlignment(Align.center);
        }
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
