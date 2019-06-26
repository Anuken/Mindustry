package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.function.BooleanProvider;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Stats.RankResult;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Item.Icon;

import static io.anuke.mindustry.Vars.*;

public class GameOverDialog extends FloatingDialog{
    private Team winner;
    private Team eliminated;
    private String mapName;
    private String author;
    private int time;

    public GameOverDialog(){
        super("$gameover");
        setFillParent(true);
        shown(this::rebuild);
    }

    void resetVars(){
        if(isShown()){
            hide();
        }
        this.winner = null;
        this.eliminated = null;
        this.mapName = null;
        this.author = null;
        this.time = -1;
    }

    public void showEliminated(Team eliminated){
        resetVars();
        this.eliminated = eliminated;
        show();
    }

    public void show(Team winner){
        resetVars();
        this.winner = winner;
        show();
    }

    public void showRemote(Team winner, String mapName, String author, int time){
        resetVars();
        this.winner = winner;
        this.mapName = mapName;
        if(!author.equals("")){
            this.author = author;
        }
        this.time = time;
        show();
    }

    void rebuild(){
        title.setText(state.launched ? "$launch.title" : "$gameover");
        buttons.clear();
        cont.clear();

        buttons.margin(10);

        if(state.rules.pvp){
            if(eliminated == null){
                cont.add(Core.bundle.format("gameover.pvp", winner.localized())).pad(6);
            }else{
                cont.add(Core.bundle.format("gameover.eliminated")).pad(6);
            }
            cont.row();
            cont.add(Core.bundle.format("gameover.place", (winner != null && winner == player.getTeam()) ? 1 : state.stats.rankPlace)).pad(6);
        }

        StringBuilder builder = new StringBuilder();
        if(mapName != null){
            builder.append(Core.bundle.format("gameover.nextmap", mapName));
            if(author != null){
                builder.append(" ");
                builder.append(Core.bundle.format("gameover.mapby", author));
            }
            builder.append("\n");
        }
        if(time != -1){
            builder.append(Core.bundle.format("gameover.nextgame", time));
            builder.append("\n");
        }
        cont.add(builder.toString());
        cont.row();

        if(state.rules.waves){
            if(control.isHighScore()){
                cont.add("$highscore").pad(6);
                cont.row();
            }
        }

        cont.pane(t -> {
            t.margin(13f);
            t.left().defaults().left();

            stat(t, "stat.wave", state.stats.wavesLasted, () -> state.rules.waves);
            stat(t, "stat.enemiesDestroyed", state.stats.enemyUnitsDestroyed);
            stat(t, "stat.built", state.stats.buildingsBuilt);
            stat(t, "stat.destroyed", state.stats.buildingsDestroyed);
            stat(t, "stat.deconstructed", state.stats.buildingsDeconstructed);
            stat(t, "stat.friendly", state.stats.teamDeaths);
            stat(t, "stat.enemies", state.stats.enemyDeaths, () -> state.rules.pvp);


            if(world.isZone() && !state.stats.itemsDelivered.isEmpty()){
                t.add("$stat.delivered");
                t.row();
                for(Item item : content.items()){
                    if(state.stats.itemsDelivered.get(item, 0) > 0){
                        t.table(items -> {
                            items.add("    [LIGHT_GRAY]" + state.stats.itemsDelivered.get(item, 0));
                            items.addImage(item.icon(Icon.medium)).size(8 * 3).pad(4);
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
        if(mapName != null){
            buttons.addButton("$continue", () ->
                hide()
            ).size(130f, 60f);
        }
    }

    void stat(Table t, String s, int val){
        stat(t, s, val, () -> true);
    }

    void stat(Table t, String s, int val, BooleanProvider cond){
        if(cond.get()){
            t.add(Core.bundle.format(s, val));
            t.row();
        }
    }
}
