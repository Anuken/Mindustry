package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Tutorial;
import io.anuke.mindustry.game.UpgradeInventory;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.input.AndroidInput;
import io.anuke.mindustry.input.DefaultKeybinds;
import io.anuke.mindustry.input.DesktopInput;
import io.anuke.mindustry.input.InputHandler;
import io.anuke.mindustry.io.Saves;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.*;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Atlas;
import io.anuke.ucore.util.InputProxy;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

/**Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any game-critical state.
 * This class is not created in the headless server.*/
public class Control extends Module{
	private UpgradeInventory upgrades = new UpgradeInventory();
	private Tutorial tutorial = new Tutorial();
	private boolean hiscore = false;

	private boolean wasPaused = false;

	private Saves saves;

	private float respawntime;
	private InputHandler input;

    private InputProxy proxy;
    private float controlx, controly;
    private boolean controlling;
    private Throwable error;

	public Control(){
		saves = new Saves();

		Inputs.useControllers(!gwt);

		Gdx.input.setCatchBackKey(true);

		if(mobile){
			input = new AndroidInput();
		}else{
			input = new DesktopInput();
		}

        proxy = new InputProxy(Gdx.input){
            @Override
            public int getY() {
                return controlling ? (int)controly : input.getY();
            }

            @Override
            public int getX() {
                return controlling ? (int)controlx : input.getX();
            }

			@Override
			public int getY(int pointer) {
				return pointer == 0 ? getY() : super.getY(pointer);
			}

			@Override
			public int getX(int pointer) {
				return pointer == 0 ? getX() : super.getX(pointer);
			}
        };

		Inputs.addProcessor(input);

		Effects.setShakeFalloff(10000f);

		Core.atlas = new Atlas("sprites.atlas");

		for(Item item : Item.getAllItems()){
			item.init();
		}

		Sounds.load("shoot.mp3", "place.mp3", "explosion.mp3", "enemyshoot.mp3",
				"corexplode.mp3", "break.mp3", "spawn.mp3", "flame.mp3", "die.mp3",
				"respawn.mp3", "purchase.mp3", "flame2.mp3", "bigshot.mp3", "laser.mp3", "lasershot.mp3",
				"ping.mp3", "tesla.mp3", "waveend.mp3", "railgun.mp3", "blast.mp3", "bang2.mp3");

		Sounds.setFalloff(9000f);

        Musics.load("1.mp3", "2.mp3", "3.mp3", "4.mp3", "5.mp3", "6.mp3");

        DefaultKeybinds.load();

		for(int i = 0; i < saveSlots; i ++){
			Settings.defaults("save-" + i + "-autosave", !gwt);
			Settings.defaults("save-" + i + "-name", "untitled");
			Settings.defaults("save-" + i + "-data", "empty");
		}

		Settings.defaultList(
			"ip", "localhost",
			"port", port+"",
			"name", mobile || gwt ? "player" : UCore.getProperty("user.name"),
			"servers", "",
			"color", Color.rgba8888(playerColors[8]),
			"lastVersion", "3.2",
			"lastBuild", 0
		);

		KeyBinds.load();

		for(Map map : world.maps().list()){
			Settings.defaults("hiscore" + map.name, 0);
		}

		player = new Player();
		player.name = Settings.getString("name");
		player.isAndroid = mobile;
		player.color.set(Settings.getInt("color"));
		player.isLocal = true;

		saves.load();

		Events.on(StateChangeEvent.class, (from, to) -> {
			if((from == State.playing && to == State.menu) || (from == State.menu && to != State.menu)){
				Timers.runTask(5f, Platform.instance::updateRPC);
			}
		});

		Events.on(PlayEvent.class, () -> {
			renderer.clearTiles();

			player.set(world.getSpawnX(), world.getSpawnY());

			Core.camera.position.set(player.x, player.y, 0);

			ui.hudfrag.updateItems();

			state.set(State.playing);
		});

		Events.on(ResetEvent.class, () -> {
			upgrades.reset();
			player.weaponLeft = player.weaponRight = Weapon.blaster;

			player.add();
			player.heal();

			respawntime = -1;
			hiscore = false;

			ui.hudfrag.updateItems();
			ui.hudfrag.updateWeapons();
			ui.hudfrag.fadeRespawn(false);
		});

		Events.on(WaveEvent.class, () -> {
			Sounds.play("spawn");

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
			Sounds.play("corexplode");
			for(int i = 0; i < 16; i ++){
				Timers.run(i*2, ()-> Effects.effect(Fx.explosion, world.getCore().worldx()+Mathf.range(40), world.getCore().worldy()+Mathf.range(40)));
			}
			Effects.effect(Fx.coreexplosion, world.getCore().worldx(), world.getCore().worldy());

			ui.restart.show();

			Timers.runTask(30f, () -> state.set(State.menu));
		});
	}

	//FIXME figure out what's causing this problem in the first place
	public void triggerInputUpdate(){
		Gdx.input = proxy;
	}

	public void setError(Throwable error){
		this.error = error;
	}

	public UpgradeInventory upgrades() {
		return upgrades;
	}

	public Saves getSaves(){
		return saves;
	}

	public boolean showCursor(){
		return controlling;
	}

	public InputHandler input(){
		return input;
	}

	public void playMap(Map map){
		ui.loadfrag.show();
		saves.resetSave();

		Timers.runTask(10, () -> {
			logic.reset();
			world.loadMap(map);
			logic.play();
		});

		Timers.runTask(18, () -> ui.loadfrag.hide());
	}

	public boolean isHighScore(){
		return hiscore;
	}

	public float getRespawnTime(){
		return respawntime;
	}

	public void setRespawnTime(float respawntime){
		this.respawntime = respawntime;
	}

	public Tutorial tutorial(){
		return tutorial;
	}

	@Override
	public void dispose(){
		Platform.instance.onGameExit();
		Net.dispose();
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
		Timers.run(1f, Musics::shuffleAll);

		Entities.initPhysics();
		Entities.collisions().setCollider(tilesize, world::solid);

		Platform.instance.updateRPC();
	}

	@Override
	public void update(){

		if(error != null){
			throw new RuntimeException(error);
		}

		Gdx.input = proxy;

        if(Inputs.keyTap("console")){
			console = !console;
		}

        if(KeyBinds.getSection("default").device.type == DeviceType.controller){
            if(Inputs.keyTap("select")){
                Inputs.getProcessor().touchDown(Gdx.input.getX(), Gdx.input.getY(), 0, Buttons.LEFT);
            }

            if(Inputs.keyRelease("select")){
				Inputs.getProcessor().touchUp(Gdx.input.getX(), Gdx.input.getY(), 0, Buttons.LEFT);
            }

            float xa = Inputs.getAxis("cursor_x");
            float ya = Inputs.getAxis("cursor_y");

            if(Math.abs(xa) > controllerMin || Math.abs(ya) > controllerMin) {
            	float scl = Settings.getInt("sensitivity")/100f * Unit.dp.scl(1f);
                controlx += xa*baseControllerSpeed*scl;
                controly -= ya*baseControllerSpeed*scl;
                controlling = true;

                Gdx.input.setCursorCatched(true);

				Inputs.getProcessor().touchDragged(Gdx.input.getX(), Gdx.input.getY(), 0);
            }

            controlx = Mathf.clamp(controlx, 0, Gdx.graphics.getWidth());
            controly = Mathf.clamp(controly, 0, Gdx.graphics.getHeight());

            if(Gdx.input.getDeltaX() > 1 || Gdx.input.getDeltaY() > 1) {
				controlling = false;
				Gdx.input.setCursorCatched(false);
			}
        }else{
            controlling = false;
			Gdx.input.setCursorCatched(false);
        }

        if(!controlling){
            controlx = Gdx.input.getX();
            controly = Gdx.input.getY();
        }

        saves.update();

		if(state.inventory.isUpdated() && (Timers.get("updateItems", 8) || state.is(State.paused))){
			ui.hudfrag.updateItems();
			state.inventory.setUpdated(false);
		}

		if(!state.is(State.menu)){
			input.update();

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

				if(respawntime > 0){

					respawntime -= delta();

					if(respawntime <= 0){
						player.set(world.getSpawnX(), world.getSpawnY());
						player.heal();
						player.add();
						Effects.sound("respawn");
						ui.hudfrag.fadeRespawn(false);
					}
				}

				if(tutorial.active()){
					tutorial.update();
				}
			}
		}else{
			if(!state.is(State.paused) || Net.active()){
				Timers.update();
			}
		}
	}
}
