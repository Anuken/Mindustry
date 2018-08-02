package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.IntFormat;
import io.anuke.mindustry.ui.Minimap;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.actions.Actions;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class HudFragment extends Fragment{
    public final BlocksFragment blockfrag = new BlocksFragment();

    private ImageButton menu, flip;
    private Table wavetable;
    private Table infolabel;
    private Table lastUnlockTable;
    private Table lastUnlockLayout;
    private boolean shown = true;
    private float dsize = 58;
    private float isize = 40;

    public void build(Group parent){

        //menu at top left
        parent.fill(cont -> {

            cont.top().left().visible(() -> !state.is(State.menu));

            cont.table(select -> {
                select.left();
                select.defaults().size(dsize).left();

                menu = select.addImageButton("icon-menu", isize, ui.paused::show).get();
                flip = select.addImageButton("icon-arrow-up", isize, this::toggleMenus).get();

                select.update(() -> {
                    if(Inputs.keyTap("toggle_menus") && !ui.chatfrag.chatOpen()){
                        toggleMenus();
                    }
                });

                select.addImageButton("icon-pause", isize, () -> {
                    if(Net.active()){
                        ui.listfrag.toggle();
                    }else{
                        state.set(state.is(State.paused) ? State.playing : State.paused);
                    }
                }).update(i -> {
                    if(Net.active()){
                        i.getStyle().imageUp = Core.skin.getDrawable("icon-players");
                    }else{
                        i.setDisabled(Net.active());
                        i.getStyle().imageUp = Core.skin.getDrawable(state.is(State.paused) ? "icon-play" : "icon-pause");
                    }
                }).get();

                select.addImageButton("icon-settings", isize, () -> {
                    if(Net.active() && mobile){
                        if(ui.chatfrag.chatOpen()){
                            ui.chatfrag.hide();
                        }else{
                            ui.chatfrag.toggle();
                        }
                    }else{
                        ui.settings.show();
                    }
                }).update(i -> {
                    if(Net.active() && mobile){
                        i.getStyle().imageUp = Core.skin.getDrawable("icon-chat");
                    }else{
                        i.getStyle().imageUp = Core.skin.getDrawable("icon-settings");
                    }
                }).get();
            });

            cont.row();

            Table waves = cont.table(this::addWaveTable).touchable(Touchable.enabled).fillX().height(66f).get();

            cont.row();

            //fps display
            infolabel = cont.table(t -> {
                IntFormat fps = new IntFormat("text.fps");
                IntFormat tps = new IntFormat("text.tps");
                IntFormat ping = new IntFormat("text.ping");
                t.label(() -> fps.get(Gdx.graphics.getFramesPerSecond())).padRight(10);
                t.label(() -> tps.get(threads.getTPS())).visible(() -> threads.isEnabled());
                t.row();
                if(Net.hasClient()){
                    t.label(() -> ping.get(Net.getPing())).visible(() -> Net.client() && !gwt).colspan(2);
                }
            }).size(-1).visible(() -> Settings.getBool("fps")).update(t -> {
                t.setTranslation(0, state.mode.disableWaves ? waves.getHeight() : 0);
            }).get();

            //make wave box appear below rest of menu
            cont.swapActor(wavetable, menu.getParent());
        });

        //minimap
        parent.fill(t -> t.top().right().add(new Minimap())
            .visible(() -> !state.is(State.menu) && Settings.getBool("minimap")));

        //paused table
        parent.fill(t -> {
            t.top().visible(() -> state.is(State.paused) && !Net.active());
            t.table("pane", top -> top.add("[orange]< " + Bundles.get("text.paused") + " >").pad(6).get().setFontScale(fontScale * 1.5f));
        });

        //'saving' indicator
        parent.fill(t -> {
            t.bottom().visible(() -> !state.is(State.menu) && control.getSaves().isSaving());
            t.add("$text.saveload");
        });

        blockfrag.build(Core.scene.getRoot());
    }

    /**
     * Show unlock notification for a new recipe.
     */
    public void showUnlock(Recipe recipe){
        blockfrag.rebuild();

        //if there's currently no unlock notification...
        if(lastUnlockTable == null){
            Table table = new Table("button");
            table.update(() -> {
                if(state.is(State.menu)){
                    table.remove();
                    lastUnlockLayout = null;
                    lastUnlockTable = null;
                }
            });
            table.margin(12);

            Table in = new Table();

            //create texture stack for displaying
            Stack stack = new Stack();
            for(TextureRegion region : recipe.result.getCompactIcon()){
                Image image = new Image(region);
                image.setScaling(Scaling.fit);
                stack.add(image);
            }

            in.add(stack).size(48f).pad(2);

            //add to table
            table.add(in).padRight(8);
            table.add("$text.unlocked");
            table.pack();

            //create container table which will align and move
            Table container = Core.scene.table();
            container.top().add(table);
            container.setTranslation(0, table.getPrefHeight());
            container.actions(Actions.translateBy(0, -table.getPrefHeight(), 1f, Interpolation.fade), Actions.delay(4f),
                    //nesting actions() calls is necessary so the right prefHeight() is used
                    Actions.run(() -> container.actions(Actions.translateBy(0, table.getPrefHeight(), 1f, Interpolation.fade), Actions.run(() -> {
                        lastUnlockTable = null;
                        lastUnlockLayout = null;
                    }), Actions.removeActor())));

            lastUnlockTable = container;
            lastUnlockLayout = in;
        }else{
            //max column size
            int col = 3;
            //max amount of elements minus extra 'plus'
            int cap = col * col - 1;

            //get old elements
            Array<Element> elements = new Array<>(lastUnlockLayout.getChildren());
            int esize = elements.size;

            //...if it's already reached the cap, ignore everything
            if(esize > cap) return;

            //get size of each element
            float size = 48f / Math.min(elements.size + 1, col);

            //correct plurals if needed
            if(esize == 1){
                ((Label) lastUnlockLayout.getParent().find(e -> e instanceof Label)).setText("$text.unlocked.plural");
            }

            lastUnlockLayout.clearChildren();
            lastUnlockLayout.defaults().size(size).pad(2);

            for(int i = 0; i < esize && i <= cap; i++){
                lastUnlockLayout.add(elements.get(i));

                if(i % col == col - 1){
                    lastUnlockLayout.row();
                }
            }

            //if there's space, add it
            if(esize < cap){

                Stack stack = new Stack();
                for(TextureRegion region : recipe.result.getCompactIcon()){
                    Image image = new Image(region);
                    image.setScaling(Scaling.fit);
                    stack.add(image);
                }

                lastUnlockLayout.add(stack);
            }else{ //else, add a specific icon to denote no more space
                lastUnlockLayout.addImage("icon-add");
            }

            lastUnlockLayout.pack();
        }
    }

    private void toggleMenus(){
        wavetable.clearActions();
        infolabel.clearActions();

        float dur = 0.3f;
        Interpolation in = Interpolation.pow3Out;

        flip.getStyle().imageUp = Core.skin.getDrawable(shown ? "icon-arrow-down" : "icon-arrow-up");

        if(shown){
            shown = false;
            blockfrag.toggle(dur, in);
            wavetable.actions(Actions.translateBy(0, (wavetable.getHeight() + dsize) - wavetable.getTranslation().y, dur, in));
            infolabel.actions(Actions.translateBy(0, (wavetable.getHeight()) - wavetable.getTranslation().y, dur, in));
        }else{
            shown = true;
            blockfrag.toggle(dur, in);
            wavetable.actions(Actions.translateBy(0, -wavetable.getTranslation().y, dur, in));
            infolabel.actions(Actions.translateBy(0, -infolabel.getTranslation().y, dur, in));
        }
    }

    private String getEnemiesRemaining(){
        int enemies = unitGroups[Team.red.ordinal()].size();
        if(enemies == 1){
            return Bundles.format("text.enemies.single", enemies);
        }else{
            return Bundles.format("text.enemies", enemies);
        }
    }

    private void addWaveTable(Table table){
        wavetable = table;
        float uheight = 66f;

        IntFormat wavef = new IntFormat("text.wave");
        IntFormat timef = new IntFormat("text.wave.waiting");

        table.background("button");
        table.left().table(text -> {
            text.left();
            text.label(() -> wavef.get(state.wave)).left().get().setFontScale(fontScale * 1.5f);
            text.row();
            text.label(() -> unitGroups[Team.red.ordinal()].size() > 0 && state.mode.disableWaveTimer ?
                getEnemiesRemaining() : (state.mode.disableWaveTimer) ? "$text.waiting" :
                timef.get((int) (state.wavetime / 60f))).minWidth(126).left();
        });

        table.add().growX();
        table.visible(() -> !state.mode.disableWaves);

        playButton(uheight);
    }

    private void playButton(float uheight){
        wavetable.addImageButton("icon-play", 30f, () -> {
            if(Net.client() && players[0].isAdmin){
                Call.onAdminRequest(players[0], AdminAction.wave);
            }else{
                state.wavetime = 0f;
            }
        }).height(uheight).fillX().right().padTop(-8f).padBottom(-12f).padLeft(-15).padRight(-10).width(40f).update(l -> {
            boolean vis = state.mode.disableWaveTimer && ((Net.server() || players[0].isAdmin) || !Net.active());
            boolean paused = state.is(State.paused) || !vis;

            l.getStyle().imageUp = Core.skin.getDrawable(vis ? "icon-play" : "clear");
            l.setTouchable(!paused ? Touchable.enabled : Touchable.disabled);
        }).visible(() -> state.mode.disableWaveTimer && ((Net.server() || players[0].isAdmin) || !Net.active()) && unitGroups[Team.red.ordinal()].size() == 0);
    }
}
