package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.ucore.scene.ui.Dialog;

import static io.anuke.mindustry.Vars.control;

public class RestartDialog extends Dialog {
    
    public RestartDialog(){
        super("$text.gameover", "dialog");

        shown(() -> {
            content().clearChildren();
            if(control.isHighScore()){
                content().add("$text.highscore").pad(6);
                content().row();
            }
            content().add("$text.lasted").pad(12).get();
            content().add("[accent]" + control.getWave());
            pack();
        });

        getButtonTable().addButton("$text.menu", ()-> {
            hide();
            state.set(State.menu);
            control.reset();
        }).size(130f, 60f);
    }
}
