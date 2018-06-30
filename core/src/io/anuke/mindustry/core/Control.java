package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.ContentDatabase;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.DefaultKeybinds;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.io.Map;
import io.anuke.mindustry.io.Saves;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Atlas;

import static io.anuke.mindustry.Vars.*;

/**Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.*/
public class Control extends Module{
	/**Minimum period of time between the same sound being played.*/
	private static final long minSoundPeriod = 100;

	private boolean hiscore = false;
	private boolean wasPaused = false;
	private Saves saves;
	private ContentDatabase db;
	private InputHandler[] inputs = {};
	private ObjectMap<Sound, Long> soundMap = new ObjectMap<>();

    private Throwable error;
    private Input gdxInput;

	public Control(){

		saves = new Saves();
		db = new ContentDatabase();

		Inputs.useControllers(!gwt);

		Gdx.input.setCatchBackKey(true);

		Effects.setShakeFalloff(10000f);

		ContentLoader.initialize(Content::init);
		Core.atlas = new Atlas("sprites.atlas");
		Core.atlas.setErrorRegion("error");
		ContentLoader.initialize(Content::load);

		db.load();

		gdxInput = Gdx.input;

		Sounds.load("shoot.mp3", "place.mp3", "explosion.mp3", "enemyshoot.mp3",
				"corexplode.mp3", "break.mp3", "spawn.mp3", "flame.mp3", "die.mp3",
				"respawn.mp3", "purchase.mp3", "flame2.mp3", "bigshot.mp3", "laser.mp3", "lasershot.mp3",
				"ping.mp3", "tesla.mp3", "waveend.mp3", "railgun.mp3", "blast.mp3", "bang2.mp3");

		Sounds.setFalloff(9000f);
		Sounds.setPlayer((sound, volume) -> {
			long time = TimeUtils.millis();
			long value = soundMap.get(sound, 0L);

			if(TimeUtils.timeSinceMillis(value) >= minSoundPeriod){
				threads.run(() -> sound.play(volume));
				soundMap.put(sound, time);
			}
		});

        Musics.load("1.mp3", "2.mp3", "3.mp3", "4.mp3", "5.mp3", "6.mp3");

        DefaultKeybinds.load();

		Settings.defaultList(
			"ip", "localhost",
			"port", port+"",
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

		Events.on(StateChangeEvent.class, (from, to) -> {
			if((from == State.playing && to == State.menu) || (from == State.menu && to != State.menu)){
				Timers.runTask(5f, Platform.instance::updateRPC);
			}
		});

		Events.on(PlayEvent.class, () -> {
		    for(Player player : players){
                player.add();
            }

			state.set(State.playing);
		});

		Events.on(ResetEvent.class, () -> {
		    for(Player player : players){
		        player.reset();
            }

			hiscore = false;

			saves.resetSave();
		});

		Events.on(WaveEvent.class, () -> {

			int last = Settings.getInt("hiscore" + world.getMap().name, 0);

			if(state.wave > last && !state.mode.infiniteResources && !state.mode.disableWaveTimer){
				Settings.putInt("hiscore" + world.getMap().name, state.wave);
				Settings.save();
				hiscore = true;
			}

			Platform.instance.updateRPC();
		});

		Events.on(GameOverEvent.class, () -> {
			Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);

			//TODO game over effect
			ui.restart.show();

			Timers.runTask(30f, () -> state.set(State.menu));
		});

		Events.on(WorldLoadEvent.class, () -> threads.runGraphics(() -> Events.fire(WorldLoadGraphicsEvent.class)));
	}

	public void addPlayer(int index){
	    if(players.length < index + 1){
	        Player[] old = players;
	        players = new Player[index + 1];
            System.arraycopy(old, 0, players, 0, old.length);

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
            input = new AndroidInput(player);
        }else{
            input = new DesktopInput(player);
        }

        inputs[index] = input;
        Inputs.addProcessor(input);
    }

    public void removePlayer(){
	    players[players.length-1].remove();
	    inputs[inputs.length-1].remove();

        Player[] old = players;
        players = new Player[players.length - 1];
        System.arraycopy(old, 0, players, 0, players.length);

        InputHandler[] oldi = inputs;
        inputs = new InputHandler[inputs.length - 1];
        System.arraycopy(oldi, 0, inputs, 0, inputs.length);
    }

	public ContentDatabase database() {
		return db;
	}

	public Input gdxInput(){
	    return gdxInput;
    }

	public void setError(Throwable error){
		this.error = error;
	}

	public Saves getSaves(){
		return saves;
	}

	public InputHandler input(int index){
		return inputs[index];
	}

	public void triggerUpdateInput(){
	    //Gdx.input = proxy;
    }

	public void playMap(Map map){
		ui.loadfrag.show();

		Timers.run(5f, () ->
			threads.run(() -> {
				logic.reset();
				world.loadMap(map);
				logic.play();

				Gdx.app.postRunnable(ui.loadfrag::hide);
			}));
	}

	public boolean isHighScore(){
		return hiscore;
	}

	private void checkUnlockableBlocks(){
		TileEntity entity = players[0].getClosestCore();

		if(entity == null) return;

		for (int i = 0; i < entity.items.items.length; i++) {
			if(entity.items.items[i] <= 0) continue;
			Item item = Item.getByID(i);
			control.database().unlockContent(item);
		}

		if(players[0].inventory.hasItem()){
			control.database().unlockContent(players[0].inventory.getItem().item);
		}

		for(int i = 0 ; i < Recipe.all().size; i ++){
			Recipe recipe = Recipe.all().get(i);
			if(!recipe.debugOnly && entity.items.hasItems(recipe.requirements)){
				if(control.database().unlockContent(recipe)){
					ui.hudfrag.showUnlock(recipe);
				}
			}
		}
	}

	@Override
	public void dispose(){
		Platform.instance.onGameExit();
		Net.dispose();
		ui.editor.dispose();
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
		EntityPhysics.initPhysics();

		Platform.instance.updateRPC();

		if(!Settings.has("4.0-warning")){
			Settings.putBool("4.0-warning", true);

			Timers.runTask(5f, () -> {
				FloatingDialog dialog = new FloatingDialog("[orange]WARNING![]");
				dialog.buttons().addButton("$text.ok", dialog::hide).size(100f, 60f);
				dialog.content().add("The beta version you are about to play should be considered very unstable, and is [accent]not representative of the final 4.0 release.[]\n\n " +
						"A large portion of content is still unimplemented. \nAll current art and UI is temporary, and will be re-drawn before release. " +
						"\n\n[accent]Saves and maps may be corrupted without warning between updates.[] You have been warned!").wrap().width(500f);
				dialog.show();

			});
		}
	}

	/**Called from main logic thread.*/
	public void runUpdateLogic(){
		if(!state.is(State.menu)) {
			renderer.minimap().updateUnitArray();
		}
	}

	@Override
	public void update(){

		if(error != null){
			throw new RuntimeException(error);
		}

        if(Inputs.keyTap("console")){
			console = !console;
		}

        saves.update();

		triggerUpdateInput();

		for(InputHandler inputHandler : inputs){
			inputHandler.updateController();
		}

		if(!state.is(State.menu)){
		    for(InputHandler input : inputs){
		        input.update();
            }

            //check unlocks every 2 seconds
			if(!state.mode.infiniteResources && !state.mode.disableWaveTimer && Timers.get("timerCheckUnlock", 120)){
				checkUnlockableBlocks();

				//save if the db changed, but don't save unlocks
				if(db.isDirty() && !debug){
					db.save();
				}
			}

			if(Inputs.keyTap("pause") && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.is(State.playing) ? State.paused : State.playing);
			}

			if(Inputs.keyTap("menu")){
				if(state.is(State.paused)){
					ui.paused.hide();
                    state.set(State.playing);
				}else if (!ui.restart.isShown()){
					if(ui.chatfrag.chatOpen()) {
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
