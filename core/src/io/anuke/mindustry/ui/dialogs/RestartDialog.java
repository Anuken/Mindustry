package io.anuke.mindustry.ui.dialogs;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.ucore.util.Bundles;

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
            content().add(Bundles.format("text.gameover.pvp",winner.localized())).pad(6);
            buttons().addButton("$text.menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);
        }else if(world.getSector() == null){
            if(control.isHighScore()){
                content().add("$text.highscore").pad(6);
                content().row();
            }
            content().add(Bundles.format("text.wave.lasted", state.wave)).pad(12);

            buttons().addButton("$text.menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);
        }else{
            content().add("$text.sector.gameover");
            buttons().addButton("$text.menu", () -> {
                hide();
                state.set(State.menu);
                logic.reset();
            }).size(130f, 60f);

            buttons().addButton("$text.sector.retry", () -> {
                Sector sector = world.getSector();
                ui.loadLogic(() -> world.sectors.playSector(sector));
                hide();
            }).size(130f, 60f);
        }
    }
}
