package mindustry.ui.dialogs;

import arc.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class GameOverDialog extends BaseDialog{
    private Team winner;

    public GameOverDialog(){
        super("@gameover");
        setFillParent(true);
        shown(this::rebuild);

        Events.on(ResetEvent.class, e -> hide());
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
        title.setText(state.isCampaign() ? Core.bundle.format("sector.lost", state.getSector().name()) : "@gameover");
        buttons.clear();
        cont.clear();

        buttons.margin(10);

        if(state.rules.pvp && winner != null){
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
                t.add(Core.bundle.format("stat.wave", state.stats.wavesLasted)).row();
                t.add(Core.bundle.format("stat.enemiesDestroyed", state.stats.enemyUnitsDestroyed)).row();
                t.add(Core.bundle.format("stat.built", state.stats.buildingsBuilt)).row();
                t.add(Core.bundle.format("stat.destroyed", state.stats.buildingsDestroyed)).row();
                t.add(Core.bundle.format("stat.deconstructed", state.stats.buildingsDeconstructed)).row();
                if(control.saves.getCurrent() != null){
                    t.add(Core.bundle.format("stat.playtime", control.saves.getCurrent().getPlayTime())).row();
                }
                if(state.isCampaign() && !state.stats.itemsDelivered.isEmpty()){
                    t.add("@stat.delivered").row();
                    for(Item item : content.items()){
                        if(state.stats.itemsDelivered.get(item, 0) > 0){
                            t.table(items -> {
                                items.add("    [lightgray]" + state.stats.itemsDelivered.get(item, 0));
                                items.image(item.uiIcon).size(8 * 3).pad(4);
                            }).left().row();
                        }
                    }
                }

                if(state.isCampaign() && net.client()){
                    t.add("@gameover.waiting").padTop(20f).row();
                }

            }).pad(12);

            if(state.isCampaign()){
                if(net.client()){
                    buttons.button("@gameover.disconnect", () -> {
                        logic.reset();
                        net.reset();
                        hide();
                        state.set(State.menu);
                    }).size(170f, 60f);
                }else{
                    buttons.button("@continue", () -> {
                        hide();
                        ui.planet.show();
                    }).size(170f, 60f);
                }
            }else{
                buttons.button("@menu", () -> {
                    hide();
                    logic.reset();
                }).size(140f, 60f);
            }
        }
    }
}
