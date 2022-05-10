package mindustry.ui.dialogs;

import arc.*;
import arc.flabel.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class GameOverDialog extends BaseDialog{
    private Team winner;
    private boolean hudShown;

    public GameOverDialog(){
        super("@gameover");
        setFillParent(true);

        titleTable.remove();

        shown(() -> {
            hudShown = ui.hudfrag.shown;
            ui.hudfrag.shown = false;
            rebuild();
        });

        hidden(() -> ui.hudfrag.shown = hudShown);

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
        buttons.clear();
        cont.clear();

        buttons.margin(10);

        cont.table(t -> {
            if(state.rules.pvp && winner != null){
                t.add(Core.bundle.format("gameover.pvp", winner.localized())).center().pad(6);
            }else{
                t.add(state.isCampaign() ? Core.bundle.format("sector.lost", state.getSector().name()) : "@gameover").center().pad(6);
            }
            t.row();

            if(control.isHighScore()){
                t.add("@highscore").pad(6);
                t.row();
            }

            t.pane(p -> {
                p.margin(13f);
                p.left().defaults().left();
                p.setBackground(Styles.black3);

                p.table(stats -> {
                    if(state.rules.waves) addStat(stats, Core.bundle.get("stats.wave"), state.stats.wavesLasted, 0f);
                    addStat(stats, Core.bundle.get("stats.unitsCreated"), state.stats.unitsCreated, 0.05f);
                    addStat(stats, Core.bundle.get("stats.enemiesDestroyed"), state.stats.enemyUnitsDestroyed, 0.1f);
                    addStat(stats, Core.bundle.get("stats.built"), state.stats.buildingsBuilt, 0.15f);
                    addStat(stats, Core.bundle.get("stats.destroyed"), state.stats.buildingsDestroyed, 0.2f);
                    addStat(stats, Core.bundle.get("stats.deconstructed"), state.stats.buildingsDeconstructed, 0.25f);
                }).top().grow().row();

                if(control.saves.getCurrent() != null){
                    p.table(pt -> {
                        pt.add(new FLabel(Core.bundle.get("stats.playtime"))).left().pad(5).growX();
                        pt.add(new FLabel("[accent]" + control.saves.getCurrent().getPlayTime())).right().pad(5);
                    }).growX();
                }
            }).grow().pad(12).top();
        }).center().minWidth(370).maxSize(600, 550).grow();

        if(state.isCampaign() && net.client()){
            cont.row();
            cont.add("@gameover.waiting").padTop(20f).row();
        }

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
                if(!ui.paused.checkPlaytest()){
                    logic.reset();
                }
            }).size(140f, 60f);
        }
    }

    private void addStat(Table parent, String stat, int value, float delay){
        parent.add(new StatLabel(stat, value, delay)).top().pad(5).growX().height(50).row();
    }

    private static class StatLabel extends Table {
        private float progress = 0;

        public StatLabel(String stat, int value, float delay){
            setTransform(true);
            setClip(true);
            setBackground(Tex.whiteui);
            setColor(Pal.accent);
            margin(2f);

            FLabel statLabel = new FLabel(stat);
            statLabel.setStyle(Styles.outlineLabel);
            statLabel.setWrap(true);
            statLabel.pause();

            Label valueLabel = new Label("", Styles.outlineLabel);
            valueLabel.setAlignment(Align.right);

            add(statLabel).left().growX().padLeft(5);
            add(valueLabel).right().growX().padRight(5);

            actions(
                Actions.scaleTo(0, 1),
                Actions.delay(delay),
                Actions.parallel(
                    Actions.scaleTo(1, 1, 0.3f, Interp.pow3Out),
                    Actions.color(Pal.darkestGray, 0.3f, Interp.pow3Out),
                    Actions.sequence(
                        Actions.delay(0.3f),
                        Actions.run(() -> {
                            valueLabel.update(() -> {
                                progress = Math.min(1, progress + (Time.delta / 60));
                                valueLabel.setText("" + (int)Mathf.lerp(0, value, value < 10 ? progress : Interp.slowFast.apply(progress)));
                            });
                            statLabel.resume();
                        })
                    )
                )
            );
        }
    }
}
