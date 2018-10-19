package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Saves;
import io.anuke.mindustry.game.Unlocks;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.input.DefaultKeybinds;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.input.MobileInput;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityQuery;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Atlas;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control extends Module{
    /** Minimum period of time between the same sound being played.*/
    private static final long minSoundPeriod = 100;

    public final Saves saves;
    public final Unlocks unlocks;

    private boolean hiscore = false;
    private boolean wasPaused = false;
    private InputHandler[] inputs = {};
    private ObjectMap<Sound, Long> soundMap = new ObjectMap<>();
    private Throwable error;

    public Control(){

        saves = new Saves();
        unlocks = new Unlocks();

        Inputs.useControllers(!gwt);

        Gdx.input.setCatchBackKey(true);

        Effects.setShakeFalloff(10000f);

        content.initialize(Content::init);
        Core.atlas = new Atlas("sprites.atlas");
        Core.atlas.setErrorRegion("error");
        content.initialize(Content::load);

        unlocks.load();

        Sounds.setFalloff(9000f);
        Sounds.setPlayer((sound, volume) -> {
            long time = TimeUtils.millis();
            long value = soundMap.get(sound, 0L);

            if(TimeUtils.timeSinceMillis(value) >= minSoundPeriod){
                threads.runGraphics(() -> sound.play(volume));
                soundMap.put(sound, time);
            }
        });

        DefaultKeybinds.load();

        Settings.defaultList(
            "ip", "localhost",
            "color-0", Color.rgba8888(playerColors[8]),
            "color-1", Color.rgba8888(playerColors[11]),
            "color-2", Color.rgba8888(playerColors[13]),
            "color-3", Color.rgba8888(playerColors[9]),
            "name", "player",
            "lastBuild", 0
        );

        KeyBinds.load();

        addPlayer(0);

        saves.load();

        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Timers.runTask(5f, Platform.instance::updateRPC);
            }
        });

        Events.on(PlayEvent.class, event -> {
            for(Player player : players){
                player.add();
            }

            state.set(State.playing);

            if(world.getSector() == null && !Settings.getBool("custom-warning-for-real-1", false)){
                threads.runGraphics(() -> ui.showInfo("$mode.custom.warning", () ->
                    ui.showInfo("$mode.custom.warning.read", () -> {
                        Settings.putBool("custom-warning-for-real-1", true);
                        Settings.save();
                    })));

            }
        });

        Events.on(WorldLoadGraphicsEvent.class, event -> {
            if(mobile){
                Core.camera.position.set(players[0].x, players[0].y, 0);
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

            int last = Settings.getInt("hiscore" + world.getMap().name, 0);

            if(state.wave > last && !state.mode.infiniteResources && !state.mode.disableWaveTimer && world.getSector() == null){
                Settings.putInt("hiscore" + world.getMap().name, state.wave);
                Settings.save();
                hiscore = true;
            }

            Platform.instance.updateRPC();
        });

        Events.on(GameOverEvent.class, event -> {
            //delete saves for game-over sectors
            if(world.getSector() != null && world.getSector().hasSave()){
                world.getSector().getSave().delete();
            }

            threads.runGraphics(() -> {
                Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
                //the restart dialog can show info for any number of scenarios
                Call.onGameOver(event.winner);
            });
        });

        //autohost for pvp sectors
        Events.on(WorldLoadEvent.class, event -> {
            if(state.mode.isPvp && !Net.active()){
                try{
                    Net.host(port);
                    players[0].isAdmin = true;
                }catch(IOException e){
                    ui.showError(Bundles.format("text.server.error", Strings.parseException(e, false)));
                    threads.runDelay(() -> state.set(State.menu));
                }
            }
        });

        Events.on(WorldLoadEvent.class, event -> threads.runGraphics(() -> Events.fire(new WorldLoadGraphicsEvent())));

        Events.on(TileChangeEvent.class, event -> {
            if(event.tile.getTeam() == players[0].getTeam() && Recipe.getByResult(event.tile.block()) != null){
                unlocks.handleContentUsed(Recipe.getByResult(event.tile.block()));
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
        player.name = Settings.getString("name");
        player.mech = mobile ? Mechs.starterMobile : Mechs.starterDesktop;
        player.color.set(Settings.getInt("color-" + index));
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
        Inputs.addProcessor(input);
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

    public void setError(Throwable error){
        this.error = error;
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

    private void checkUnlockableBlocks(){
        TileEntity entity = players[0].getClosestCore();

        if(entity == null) return;

        entity.items.forEach((item, amount) -> unlocks.unlockContent(item));

        if(players[0].inventory.hasItem()){
            unlocks.unlockContent(players[0].inventory.getItem().item);
        }

        outer:
        for(int i = 0; i < content.recipes().size; i ++){
            Recipe recipe = content.recipes().get(i);
            if(!recipe.hidden && recipe.requirements != null){
                for(ItemStack stack : recipe.requirements){
                    if(!entity.items.has(stack.item, Math.min((int) (stack.amount * unlockResourceScaling), 2000))) continue outer;
                }

                if(unlocks.unlockContent(recipe)){
                    ui.hudfrag.showUnlock(recipe);
                }
            }
        }
    }

    @Override
    public void dispose(){
        Platform.instance.onGameExit();
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

        if(!Settings.getBool("4.0-warning-2", false)){

            Timers.run(5f, () -> {
                FloatingDialog dialog = new FloatingDialog("[orange]WARNING![]");
                dialog.buttons().addButton("$text.ok", () -> {
                    dialog.hide();
                    Settings.putBool("4.0-warning-2", true);
                    Settings.save();
                }).size(100f, 60f);
                dialog.content().add("Reminder: The beta version you are about to play is very unstable, and is [accent]not representative of the final 4.0 release.[]\n\n " +
                        "\nThere is currently[scarlet] no sound implemented[]; this is intentional.\n" +
                        "All current art and UI is temporary, and will be re-drawn before release. " +
                        "\n\n[accent]Saves and maps may be corrupted without warning between updates.").wrap().width(400f);
                dialog.show();
            });
        }
    }

    /** Called from main logic thread.*/
    public void runUpdateLogic(){
        if(!state.is(State.menu)){
            renderer.minimap.updateUnitArray();
        }
    }

    @Override
    public void update(){

        if(error != null){
            throw new RuntimeException(error);
        }

        saves.update();

        for(InputHandler inputHandler : inputs){
            inputHandler.updateController();
        }

        if(!state.is(State.menu)){
            for(InputHandler input : inputs){
                input.update();
            }

            //auto-update rpc every 5 seconds
            if(Timers.get("rpcUpdate", 60 * 5)){
                Platform.instance.updateRPC();
            }

            //check unlocks every 2 seconds
            if(!state.mode.infiniteResources && Timers.get("timerCheckUnlock", 120)){
                checkUnlockableBlocks();

                //save if the unlocks changed
                if(unlocks.isDirty()){
                    unlocks.save();
                }
            }

            if(Inputs.keyTap("pause") && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.is(State.playing) ? State.paused : State.playing);
            }

            if(Inputs.keyTap("menu")){
                if(state.is(State.paused)){
                    ui.paused.hide();
                    state.set(State.playing);
                }else if(!ui.restart.isShown()){
                    if(ui.chatfrag.chatOpen()){
                        ui.chatfrag.hide();
                    }else{
                        ui.paused.show();
                        state.set(State.paused);
                    }
                }
            }

            if(!state.is(State.paused) || Net.active()){
                Entities.update(effectGroup);
                Entities.update(groundEffectGroup);
            }
        }else{
            if(!state.is(State.paused) || Net.active()){
                Timers.update();
            }
        }
    }
}
