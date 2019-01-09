package io.anuke.mindustry.core;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.entities.Effects;
import io.anuke.arc.entities.EntityQuery;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureAtlas;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.util.Interval;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Saves;
import io.anuke.mindustry.game.GlobalData;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.input.Binding;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.MobileInput;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

import java.io.IOException;

import static io.anuke.arc.Core.scene;
import static io.anuke.mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener{
    public final Saves saves;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false;
    private InputHandler[] inputs = {};

    public Control(){
        saves = new Saves();
        data = new GlobalData();

        Core.input.setCatch(KeyCode.BACK, true);

        Effects.setShakeFalloff(10000f);

        content.initialize(Content::init);
        Core.atlas = new TextureAtlas("sprites/sprites.atlas");
        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();
        content.initialize(Content::load);

        if(Core.atlas.getTextures().size != 1){
            throw new IllegalStateException("Atlas must be exactly one texture. " +
            "If more textures are used, the map editor will not display them correctly.");
        }

        data.load();

        Core.settings.setAppName(appName);
        Core.settings.defaults(
            "ip", "localhost",
            "color-0", Color.rgba8888(playerColors[8]),
            "color-1", Color.rgba8888(playerColors[11]),
            "color-2", Color.rgba8888(playerColors[13]),
            "color-3", Color.rgba8888(playerColors[9]),
            "name", "",
            "lastBuild", 0
        );

        addPlayer(0);

        saves.load();

        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Time.runTask(5f, Platform.instance::updateRPC);
            }
        });

        Events.on(PlayEvent.class, event -> {
            for(Player player : players){
                player.add();
            }

            state.set(State.playing);
        });

        Events.on(WorldLoadEvent.class, event -> {
            if(mobile){
                Core.app.post(() -> Core.camera.position.set(players[0]));
            }
        });

        Events.on(ResetEvent.class, event -> {
            for(Player player : players){
                player.reset();
            }

            hiscore = false;

            saves.resetSave();
        });

        Events.on(WaveEvent.class, event -> {

            int last = Core.settings.getInt("hiscore" + world.getMap().name, 0);

            if(state.wave > last && !state.mode.infiniteResources && !state.mode.disableWaveTimer){
                Core.settings.put("hiscore" + world.getMap().name, state.wave);
                Core.settings.save();
                hiscore = true;
            }

            Platform.instance.updateRPC();
        });

        Events.on(GameOverEvent.class, event -> {
            Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.onGameOver(event.winner);
        });

        //autohost for pvp sectors
        Events.on(WorldLoadEvent.class, event -> {
            if(state.mode.isPvp && !Net.active()){
                try{
                    Net.host(port);
                    players[0].isAdmin = true;
                }catch(IOException e){
                    ui.showError(Core.bundle.format("server.error", Strings.parseException(e, false)));
                    Core.app.post(() -> state.set(State.menu));
                }
            }
        });
    }

    public void addPlayer(int index){
        if(players.length != index + 1){
            Player[] old = players;
            players = new Player[index + 1];
            System.arraycopy(old, 0, players, 0, old.length);
        }

        if(inputs.length != index + 1){
            InputHandler[] oldi = inputs;
            inputs = new InputHandler[index + 1];
            System.arraycopy(oldi, 0, inputs, 0, oldi.length);
        }

        Player setTo = (index == 0 ? null : players[0]);

        Player player = new Player();
        player.name = Core.settings.getString("name");
        player.mech = mobile ? Mechs.starterMobile : Mechs.starterDesktop;
        player.color.set(Core.settings.getInt("color-" + index));
        player.isLocal = true;
        player.playerIndex = index;
        player.isMobile = mobile;
        players[index] = player;

        if(setTo != null){
            player.set(setTo.x, setTo.y);
        }

        if(!state.is(State.menu)){
            player.add();
        }

        InputHandler input;

        if(mobile){
            input = new MobileInput(player);
        }else{
            input = new DesktopInput(player);
        }

        inputs[index] = input;
        Core.input.addProcessor(input);
    }

    public void removePlayer(){
        players[players.length - 1].remove();
        inputs[inputs.length - 1].remove();

        Player[] old = players;
        players = new Player[players.length - 1];
        System.arraycopy(old, 0, players, 0, players.length);

        InputHandler[] oldi = inputs;
        inputs = new InputHandler[inputs.length - 1];
        System.arraycopy(oldi, 0, inputs, 0, inputs.length);
    }

    public InputHandler input(int index){
        return inputs[index];
    }

    public void playMap(Map map){
        ui.loadLogic(() -> {
            logic.reset();
            world.loadMap(map);
            logic.play();
        });
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        content.dispose();
        Net.dispose();
        ui.editor.dispose();
        inputs = new InputHandler[]{};
        players = new Player[]{};
    }

    @Override
    public void pause(){
        wasPaused = state.is(State.paused);
        if(state.is(State.playing)) state.set(State.paused);
    }

    @Override
    public void resume(){
        if(state.is(State.paused) && !wasPaused){
            state.set(State.playing);
        }
    }

    @Override
    public void init(){
        EntityQuery.init();

        Platform.instance.updateRPC();

        if(!Core.settings.getBool("4.0-warning-2", false)){

            Time.run(5f, () -> {
                FloatingDialog dialog = new FloatingDialog("WARNING!");
                dialog.buttons.addButton("$ok", () -> {
                    dialog.hide();
                    Core.settings.put("4.0-warning-2", true);
                    Core.settings.save();
                }).size(100f, 60f);
                dialog.cont.add("Reminder: The alpha version you are about to play is very unstable, and is [accent]not representative of the final 4.0 release.[]\n\n " +
                        "\nThere is currently[scarlet] no sound implemented[]; this is intentional.\n" +
                        "All current art and UI is temporary, and will be re-drawn before release. " +
                        "\n\n[accent]Saves and maps may be corrupted without warning between updates.").wrap().width(400f);
                dialog.show();
            });
        }
    }

    @Override
    public void update(){

        saves.update();

        for(InputHandler inputHandler : inputs){
            inputHandler.updateController();
        }

        if(!state.is(State.menu)){
            for(InputHandler input : inputs){
                input.update();
            }

            //autosave global data every second if it's modified
            if(timer.get(1, 60)){
                data.checkSave();
            }

            //auto-update rpc every 5 seconds
            if(timer.get(60 * 5)){
                Platform.instance.updateRPC();
            }

            if(Core.input.keyTap(Binding.pause) && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.is(State.playing) ? State.paused : State.playing);
            }

            if(Core.input.keyTap(Binding.menu) && !ui.restart.isShown()){
                if(ui.chatfrag.chatOpen()){
                    ui.chatfrag.hide();
                }else if(!ui.paused.isShown() && !scene.hasDialog()){
                    ui.paused.show();
                    state.set(State.paused);
                }
            }

            if(!mobile && Core.input.keyTap(Binding.screenshot) && !(scene.getKeyboardFocus() instanceof TextField) && !ui.chatfrag.chatOpen()){
                renderer.takeMapScreenshot();
            }

        }else{
            if(!state.isPaused()){
                Time.update();
            }
        }
    }
}
