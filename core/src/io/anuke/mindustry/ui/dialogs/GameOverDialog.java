package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Stats.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.Cicon;

import static io.anuke.mindustry.Vars.*;

public class GameOverDialog extends FloatingDialog{
    private Team winner;

    public GameOverDialog(){
        super("$gameover");
        setFillParent(true);
        shown(this::rebuild);
    }

    public void show(Team winner){
        this.winner = winner;
        show();
        if(winner == player.getTeam()){
            Events.fire(new WinEvent());
        }else{
            Events.fire(new LoseEvent());
        }
    }

    void rebuild(){
        title.setText(state.launched ? "$launch.title" : "$gameover");
        buttons.clear();
        cont.clear();

        buttons.margin(10);

        if(state.rules.pvp){
            cont.add(Core.bundle.format("gameover.pvp", winner.localized())).pad(6);
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

            cont.pane(t -> {
                t.margin(13f);
                t.left().defaults().left();
                t.add(Core.bundle.format("stat.wave", state.stats.wavesLasted));
                t.row();
                t.add(Core.bundle.format("stat.enemiesDestroyed", state.stats.enemyUnitsDestroyed));
                t.row();
                t.add(Core.bundle.format("stat.built", state.stats.buildingsBuilt));
                t.row();
                t.add(Core.bundle.format("stat.destroyed", state.stats.buildingsDestroyed));
                t.row();
                t.add(Core.bundle.format("stat.deconstructed", state.stats.buildingsDeconstructed));
                t.row();
                if(world.isZone() && !state.stats.itemsDelivered.isEmpty()){
                    t.add("$stat.delivered");
                    t.row();
                    for(Item item : content.items()){
                        if(state.stats.itemsDelivered.get(item, 0) > 0){
                            t.table(items -> {
                                items.add("    [LIGHT_GRAY]" + state.stats.itemsDelivered.get(item, 0));
                                items.addImage(item.icon(Cicon.small)).size(8 * 3).pad(4);
                            }).left();
                            t.row();
                        }
                    }
                }

                if(world.isZone()){
                    RankResult result = state.stats.calculateRank(world.getZone(), state.launched);
                    t.add(Core.bundle.format("stat.rank", result.rank + result.modifier));
                    t.row();
                }
            }).pad(12);

            if(world.isZone()){
                buttons.addButton("$continue", () -> {
                    hide();
                    state.set(State.menu);
                    logic.reset();
                    ui.deploy.show();
                }).size(130f, 60f);
            }else{
                buttons.addButton("$menu", () -> {
                    hide();
                    state.set(State.menu);
                    logic.reset();
                }).size(130f, 60f);
            }
        }
    }
}
