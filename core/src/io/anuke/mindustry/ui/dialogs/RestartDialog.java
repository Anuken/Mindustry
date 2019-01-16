package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Team;

import static io.anuke.mindustry.Vars.*;

public class RestartDialog extends FloatingDialog{
    private Team winner;

    public RestartDialog(){
        super("$gameover");
        setFillParent(false);
        shown(this::rebuild);
    }

    public void show(Team winner){
        this.winner = winner;
        show();
    }

    void rebuild(){
        buttons.clear();
        cont.clear();

        buttons.margin(10);

        if(state.rules.pvp){
            cont.add(Core.bundle.format("gameover.pvp",winner.localized())).pad(6);
            buttons.addButton("$menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);
        }else{
            if(control.isHighScore()){
                cont.add("$highscore").pad(6);
                cont.row();
            }
            cont.add(Core.bundle.format("wave.lasted", state.wave)).pad(12);

            buttons.addButton("$menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);
        }
    }
}
