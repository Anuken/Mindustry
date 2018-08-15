package io.anuke.mindustry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.Puddle;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.EffectEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.OS;
import io.anuke.ucore.util.Translator;

import java.util.Locale;

public class Vars{
    //respawn time in frames
    public static final float respawnduration = 60 * 4;
    //time between waves in frames (on normal mode)
    public static final float wavespace = 60 * 60 * 1.5f;
    //set ridiculously high for now
    public static final float coreBuildRange = 800999f;

    public static final float enemyCoreBuildRange = 400f;
    //discord group URL
    public static final String discordURL = "https://discord.gg/BKADYds";
    public static final String releasesURL = "https://api.github.com/repos/Anuken/Mindustry/releases";
    public static final int maxTextLength = 150;
    public static final int maxNameLength = 40;
    public static final float itemSize = 5f;
    public static final int tilesize = 8;
    public static final int sectorSize = 300;
    public static final int mapPadding = 3;
    public static final int invalidSector = Integer.MAX_VALUE;
    public static Locale[] locales;
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
    public static boolean testMobile;
    //shorthand for whether or not this is running on android or ios
    public static boolean mobile;
    public static boolean ios;
    public static boolean android;
    //shorthand for whether or not this is running on GWT
    public static boolean gwt;
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
    public static boolean showFog = true;
    public static boolean headless = false;
    public static float controllerMin = 0.25f;
    public static float baseControllerSpeed = 11f;
    //only if smoothCamera
    public static boolean snapCamera = true;
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
    public static EntityGroup<EffectEntity> effectGroup;
    public static EntityGroup<DrawTrait> groundEffectGroup;
    public static EntityGroup<ItemDrop> itemGroup;

    public static EntityGroup<Puddle> puddleGroup;
    public static EntityGroup<Fire> fireGroup;
    public static EntityGroup<BaseUnit>[] unitGroups;

    public static final Translator[] tmptr = new Translator[]{new Translator(), new Translator(), new Translator(), new Translator()};

    public static void init(){

        //load locales
        String[] stra = new Json().fromJson(String[].class, Gdx.files.internal("locales.json"));
        locales = new Locale[stra.length];
        for(int i = 0; i < locales.length; i++){
            String code = stra[i];
            if(code.contains("_")){
                locales[i] = new Locale(code.split("_")[0], code.split("_")[1]);
            }else{
                locales[i] = new Locale(code);
            }
        }

        Version.init();

        playerGroup = Entities.addGroup(Player.class).enableMapping();
        tileGroup = Entities.addGroup(TileEntity.class, false);
        bulletGroup = Entities.addGroup(Bullet.class).enableMapping();
        effectGroup = Entities.addGroup(EffectEntity.class, false);
        groundEffectGroup = Entities.addGroup(DrawTrait.class, false);
        puddleGroup = Entities.addGroup(Puddle.class, false).enableMapping();
        itemGroup = Entities.addGroup(ItemDrop.class).enableMapping();
        fireGroup = Entities.addGroup(Fire.class, false).enableMapping();
        unitGroups = new EntityGroup[Team.all.length];

        for(Team team : Team.all){
            unitGroups[team.ordinal()] = Entities.addGroup(BaseUnit.class).enableMapping();
        }

        for(EntityGroup<?> group : Entities.getAllGroups()){
            group.setRemoveListener(entity -> {
                if(entity instanceof SyncTrait && Net.client()){
                    netClient.addRemovedEntity((entity).getID());
                }
            });
        }

        threads = new ThreadHandler(Platform.instance.getThreadProvider());

        mobile = Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS || testMobile;
        ios = Gdx.app.getType() == ApplicationType.iOS;
        android = Gdx.app.getType() == ApplicationType.Android;
        gwt = Gdx.app.getType() == ApplicationType.WebGL;

        if(!gwt){
            customMapDirectory = OS.getAppDataDirectory("Mindustry").child("maps/");
            saveDirectory = OS.getAppDataDirectory("Mindustry").child("saves/");
        }

        fontScale = Math.max(Unit.dp.scl(1f) / 2f, 0.5f);
        baseCameraScale = Math.round(Unit.dp.scl(4));
    }
}
