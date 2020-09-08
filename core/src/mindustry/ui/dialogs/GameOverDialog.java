package mindustry.ui.dialogs;

import arc.*;
import mindustry.game.EventType.*;
import mindustry.game.Stats.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class GameOverDialog extends BaseDialog{
    private Team winner;

    public GameOverDialog(){
        super("@gameover");
        setFillParent(true);
        shown(this::rebuild);
    }

    public void show(Team winner){
        this.winner = winner;
        show();
        if(winner == player.team()){
            Events.fire(new WinEvent());
        }else{
            Events.fire(new LoseEvent());
        }
    }

    void rebuild(){
        title.setText(state.launched ? "@launch.title" : "@gameover");
        buttons.clear();
        cont.clear();

        buttons.margin(10);

        if(state.rules.pvp){
            cont.add(Core.bundle.format("gameover.pvp", winner.localized())).pad(6);
            buttons.button("@menu", () -> {
                hide();
                logic.reset();
            }).size(130f, 60f);
        }else{
            if(control.isHighScore()){
                cont.add("@highscore").pad(6);
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
                if(control.saves.getCurrent() != null){
                    t.add(Core.bundle.format("stat.playtime", control.saves.getCurrent().getPlayTime()));
                    t.row();
                }
                if(state.isCampaign() && !state.stats.itemsDelivered.isEmpty()){
                    t.add("@stat.delivered");
                    t.row();
                    for(Item item : content.items()){
                        if(state.stats.itemsDelivered.get(item, 0) > 0){
                            t.table(items -> {
                                items.add("    [lightgray]" + state.stats.itemsDelivered.get(item, 0));
                                items.image(item.icon(Cicon.small)).size(8 * 3).pad(4);
                            }).left();
                            t.row();
                        }
                    }
                }

                if(state.hasSector()){
                    RankResult result = state.stats.calculateRank(state.getSector(), state.launched);
                    t.add(Core.bundle.format("stat.rank", result.rank + result.modifier));
                    t.row();
                }
            }).pad(12);

            if(state.isCampaign()){
                buttons.button("@continue", () -> {
                    hide();
                    logic.reset();
                    ui.planet.show();
                }).size(130f, 60f);
            }else{
                buttons.button("@menu", () -> {
                    hide();
                    logic.reset();
                }).size(130f, 60f);
            }
        }
    }
}
