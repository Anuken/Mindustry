package io.anuke.mindustry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.Version;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.impl.EffectEntity;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.OS;

import java.util.Locale;

public class Vars{
	public static final boolean testMobile = false;
	//shorthand for whether or not this is running on android or ios
	public static boolean mobile;
	public static boolean ios;
	public static boolean android;
	//shorthand for whether or not this is running on GWT
	public static boolean gwt;

	//respawn time in frames
	public static final float respawnduration = 60*4;
	//time between waves in frames (on normal mode)
	public static final float wavespace = 60*60*2;
	//waves can last no longer than 3 minutes, otherwise the next one spawns
	public static final float maxwavespace = 60*60*4f;

	public static final float coreBuildRange = 800f;
	//discord group URL
	public static final String discordURL = "https://discord.gg/BKADYds";

	public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";

	//directory for user-created map data
	public static FileHandle customMapDirectory;
	//save file directory
	public static FileHandle saveDirectory;
	public static String mapExtension = "mmap";
	public static String saveExtension = "msav";
	//scale of the font
	public static float fontScale;
	//camera zoom displayed on startup
	public static int baseCameraScale;
	//if true, player speed will be increased, massive amounts of resources will be given on start, and other debug options will be available
	public static boolean debug = false;
	public static boolean console = false;
	//whether the player can clip through walls
	public static boolean noclip = false;
	//whether turrets have infinite ammo (only with debug)
	public static boolean infiniteAmmo = true;
	//whether to show paths of enemies
	public static boolean showPaths = false;
	//if false, player is always hidden
	public static boolean showPlayer = true;
	//whether to hide ui, only on debug
	public static boolean showUI = true;
    //whether to show block debug
    public static boolean showBlockDebug = false;

    public static final int maxTextLength = 150;
    public static final int maxNameLength = 40;
    public static final int maxCharNameLength = 20;

	public static boolean headless = false;

	public static float controllerMin = 0.25f;

	public static float baseControllerSpeed = 11f;

	public static final int saveSlots = 64;

	public static final float itemSize = 5f;

	//only if smoothCamera
	public static boolean snapCamera = true;
	
	public static final int tilesize = 8;

	public static final Locale[] locales = {new Locale("en"), new Locale("fr"), new Locale("ru"), new Locale("uk", "UA"), new Locale("pl"),
			new Locale("de"), new Locale("pt", "BR"), new Locale("ko"), new Locale("in", "ID"), new Locale("ita"), new Locale("es")};

	public static final Color[] playerColors = {
		Color.valueOf("82759a"),
		Color.valueOf("c0c1c5"),
		Color.valueOf("fff0e7"),
		Color.valueOf("7d2953"),
		Color.valueOf("ff074e"),
		Color.valueOf("ff072a"),
		Color.valueOf("ff76a6"),
		Color.valueOf("a95238"),
		Color.valueOf("ffa108"),
		Color.valueOf("feeb2c"),
		Color.valueOf("ffcaa8"),
		Color.valueOf("008551"),
		Color.valueOf("00e339"),
		Color.valueOf("423c7b"),
		Color.valueOf("4b5ef1"),
		Color.valueOf("2cabfe"),
	};

	//server port
	public static final int port = 6567;
	public static final int webPort = 6568;

	public static GameState state;
	public static ThreadHandler threads;

	public static Control control;
	public static Logic logic;
	public static Renderer renderer;
	public static UI ui;
	public static World world;
	public static NetServer netServer;
	public static NetClient netClient;
	
	public static Player[] players = {};

	public static EntityGroup<Player> playerGroup;
	public static EntityGroup<TileEntity> tileGroup;
	public static EntityGroup<Bullet> bulletGroup;
	public static EntityGroup<Shield> shieldGroup;
	public static EntityGroup<EffectEntity> effectGroup;
	public static EntityGroup<DrawTrait> groundEffectGroup;
	public static EntityGroup<ItemDrop> itemGroup;

	public static EntityGroup<Puddle> puddleGroup;
	public static EntityGroup<Fire> fireGroup;
	public static EntityGroup<BaseUnit>[] unitGroups;

	public static void init(){
		Version.init();

		playerGroup = Entities.addGroup(Player.class).enableMapping();
		tileGroup = Entities.addGroup(TileEntity.class, false);
		bulletGroup = Entities.addGroup(Bullet.class).enableMapping();
		shieldGroup = Entities.addGroup(Shield.class, false);
		effectGroup = Entities.addGroup(EffectEntity.class, false);
		groundEffectGroup = Entities.addGroup(DrawTrait.class, false);
		puddleGroup = Entities.addGroup(Puddle.class, false).enableMapping();
		itemGroup = Entities.addGroup(ItemDrop.class).enableMapping();
		fireGroup = Entities.addGroup(Fire.class, false).enableMapping();
		unitGroups = new EntityGroup[Team.values().length];

		threads = new ThreadHandler(Platform.instance.getThreadProvider());

		for(Team team : Team.values()){
			unitGroups[team.ordinal()] = Entities.addGroup(BaseUnit.class).enableMapping();
		}

		mobile = Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS || testMobile;
		ios = Gdx.app.getType() == ApplicationType.iOS;
		android = Gdx.app.getType() == ApplicationType.Android;
		gwt = Gdx.app.getType() == ApplicationType.WebGL;

		if(!gwt) {
			customMapDirectory = OS.getAppDataDirectory("Mindustry").child("maps/");
			saveDirectory = OS.getAppDataDirectory("Mindustry").child("saves/");
		}

		fontScale = Math.max(Unit.dp.scl(1f)/2f, 0.5f);
		baseCameraScale = Math.round(Unit.dp.scl(4));
	}
}
