package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Team;

import static io.anuke.mindustry.Vars.*;

public class RestartDialog extends FloatingDialog{
    private Team winner;

    public RestartDialog(){
        super("$text.gameover");
        setFillParent(false);
        shown(this::rebuild);
    }

    public void show(Team winner){
        this.winner = winner;
        show();
    }

    void rebuild(){
        buttons().clear();
        content().clear();

        buttons().margin(10);

        if(state.mode.isPvp){
            content().add(Core.bundle.format("text.gameover.pvp",winner.localized())).pad(6);
            buttons().addButton("$text.menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);
        }else{
            if(control.isHighScore()){
                content().add("$text.highscore").pad(6);
                content().row();
            }
            content().add(Core.bundle.format("text.wave.lasted", state.wave)).pad(12);

            buttons().addButton("$text.menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);
        }
    }
}
