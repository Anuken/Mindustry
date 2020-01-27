package mindustry;

import arc.Application.*;
import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.ai.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.effect.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.maps.*;
import mindustry.mod.*;
import mindustry.net.Net;
import mindustry.net.*;
import mindustry.world.blocks.defense.ForceProjector.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import static arc.Core.settings;

public class Vars implements Loadable{
    /** Whether to load locales.*/
    public static boolean loadLocales = true;
    /** Whether the logger is loaded. */
    public static boolean loadedLogger = false, loadedFileLogger = false;
    /** Maximum schematic size.*/
    public static final int maxSchematicSize = 32;
    /** All schematic base64 starts with this string.*/
    public static final String schematicBaseStart ="bXNjaAB";
    /** IO buffer size. */
    public static final int bufferSize = 8192;
    /** global charset, since Android doesn't support the Charsets class */
    public static final Charset charset = Charset.forName("UTF-8");
    /** main application name, capitalized */
    public static final String appName = "Mindustry";
    /** URL for itch.io donations. */
    public static final String donationURL = "https://anuke.itch.io/mindustry/purchase";
    /** URL for discord invite. */
    public static final String discordURL = "https://discord.gg/mindustry";
    /** URL for sending crash reports to */
    public static final String crashReportURL = "http://192.99.169.18/report";
    /** URL the links to the wiki's modding guide.*/
    public static final String modGuideURL = "https://mindustrygame.github.io/wiki/modding/";
    /** URL to the JSON file containing all the global, public servers. Not queried in BE. */
    public static final String serverJsonURL = "https://raw.githubusercontent.com/Anuken/Mindustry/master/servers.json";
    /** URL to the JSON file containing all the BE servers. Only queried in BE. */
    public static final String serverJsonBeURL = "https://raw.githubusercontent.com/Anuken/Mindustry/master/servers_be.json";
    /** URL of the github issue report template.*/
    public static final String reportIssueURL = "https://github.com/Anuken/Mindustry/issues/new?template=bug_report.md";
    /** list of built-in servers.*/
    public static final Array<String> defaultServers = Array.with();
    /** maximum distance between mine and core that supports automatic transferring */
    public static final float mineTransferRange = 220f;
    /** whether to enable editing of units in the editor */
    public static final boolean enableUnitEditing = false;
    /** max chat message length */
    public static final int maxTextLength = 150;
    /** max player name length in bytes */
    public static final int maxNameLength = 40;
    /** displayed item size when ingame, TODO remove. */
    public static final float itemSize = 5f;
    /** extra padding around the world; units outside this bound will begin to self-destruct. */
    public static final float worldBounds = 100f;
    /** units outside of this bound will simply die instantly */
    public static final float finalWorldBounds = worldBounds + 500;
    /** ticks spent out of bound until self destruct. */
    public static final float boundsCountdown = 60 * 7;
    /** for map generator dialog */
    public static boolean updateEditorOnChange = false;
    /** size of tiles in units */
    public static final int tilesize = 8;
    /** all choosable player colors in join/host dialog */
    public static final Color[] playerColors = {
        Color.valueOf("82759a"),
        Color.valueOf("c0c1c5"),
        Color.valueOf("ffffff"),
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
    /** default server port */
    public static final int port = 6567;
    /** multicast discovery port.*/
    public static final int multicastPort = 20151;
    /** multicast group for discovery.*/
    public static final String multicastGroup = "227.2.7.7";
    /** if true, UI is not drawn */
    public static boolean disableUI;
    /** if true, game is set up in mobile mode, even on desktop. used for debugging */
    public static boolean testMobile;
    /** whether the game is running on a mobile device */
    public static boolean mobile;
    /** whether the game is running on an iOS device */
    public static boolean ios;
    /** whether the game is running on an Android device */
    public static boolean android;
    /** whether the game is running on a headless server */
    public static boolean headless;
    /** whether steam is enabled for this game */
    public static boolean steam;
    /** whether typing into the console is enabled - developers only */
    public static boolean enableConsole = false;
    /** application data directory, equivalent to {@link Settings#getDataDirectory()} */
    public static Fi dataDirectory;
    /** data subdirectory used for screenshots */
    public static Fi screenshotDirectory;
    /** data subdirectory used for custom maps */
    public static Fi customMapDirectory;
    /** data subdirectory used for custom map previews */
    public static Fi mapPreviewDirectory;
    /** tmp subdirectory for map conversion */
    public static Fi tmpDirectory;
    /** data subdirectory used for saves */
    public static Fi saveDirectory;
    /** data subdirectory used for mods */
    public static Fi modDirectory;
    /** data subdirectory used for schematics */
    public static Fi schematicDirectory;
    /** data subdirectory used for bleeding edge build versions */
    public static Fi bebuildDirectory;
    /** map file extension */
    public static final String mapExtension = "msav";
    /** save file extension */
    public static final String saveExtension = "msav";
    /** schematic file extension */
    public static final String schematicExtension = "msch";

    /** list of all locales that can be switched to */
    public static Locale[] locales;

    public static FileTree tree;
    public static Net net;
    public static ContentLoader content;
    public static GameState state;
    public static GlobalData data;
    public static EntityCollisions collisions;
    public static DefaultWaves defaultWaves;
    public static LoopControl loops;
    public static Platform platform = new Platform(){};
    public static Mods mods;
    public static Schematics schematics = new Schematics();
    public static BeControl becontrol;

    public static World world;
    public static Maps maps;
    public static WaveSpawner spawner;
    public static BlockIndexer indexer;
    public static Pathfinder pathfinder;

    public static Control control;
    public static Logic logic;
    public static Renderer renderer;
    public static UI ui;
    public static NetServer netServer;
    public static NetClient netClient;

    public static Entities entities;
    public static EntityGroup<Player> playerGroup;
    public static EntityGroup<TileEntity> tileGroup;
    public static EntityGroup<Bullet> bulletGroup;
    public static EntityGroup<EffectEntity> effectGroup;
    public static EntityGroup<DrawTrait> groundEffectGroup;
    public static EntityGroup<ShieldEntity> shieldGroup;
    public static EntityGroup<Puddle> puddleGroup;
    public static EntityGroup<Fire> fireGroup;
    public static EntityGroup<BaseUnit> unitGroup;

    public static Player player;

    @Override
    public void loadAsync(){
        loadSettings();
        init();
    }

    public static void init(){
        Serialization.init();
        DefaultSerializers.typeMappings.put("mindustry.type.ContentType", "mindustry.ctype.ContentType");

        if(loadLocales){
            //load locales
            String[] stra = Core.files.internal("locales").readString().split("\n");
            locales = new Locale[stra.length];
            for(int i = 0; i < locales.length; i++){
                String code = stra[i];
                if(code.contains("_")){
                    locales[i] = new Locale(code.split("_")[0], code.split("_")[1]);
                }else{
                    locales[i] = new Locale(code);
                }
            }

            Arrays.sort(locales, Structs.comparing(l -> l.getDisplayName(l), String.CASE_INSENSITIVE_ORDER));
        }

        Version.init();

        if(tree == null) tree = new FileTree();
        if(mods == null) mods = new Mods();

        content = new ContentLoader();
        loops = new LoopControl();
        defaultWaves = new DefaultWaves();
        collisions = new EntityCollisions();
        world = new World();
        becontrol = new BeControl();

        maps = new Maps();
        spawner = new WaveSpawner();
        indexer = new BlockIndexer();
        pathfinder = new Pathfinder();

        entities = new Entities();
        playerGroup = entities.add(Player.class).enableMapping();
        tileGroup = entities.add(TileEntity.class, false);
        bulletGroup = entities.add(Bullet.class).enableMapping();
        effectGroup = entities.add(EffectEntity.class, false);
        groundEffectGroup = entities.add(DrawTrait.class, false);
        puddleGroup = entities.add(Puddle.class).enableMapping();
        shieldGroup = entities.add(ShieldEntity.class, false);
        fireGroup = entities.add(Fire.class).enableMapping();
        unitGroup = entities.add(BaseUnit.class).enableMapping();

        for(EntityGroup<?> group : entities.all()){
            group.setRemoveListener(entity -> {
                if(entity instanceof SyncTrait && net.client()){
                    netClient.addRemovedEntity((entity).getID());
                }
            });
        }

        state = new GameState();
        data = new GlobalData();

        mobile = Core.app.getType() == ApplicationType.Android || Core.app.getType() == ApplicationType.iOS || testMobile;
        ios = Core.app.getType() == ApplicationType.iOS;
        android = Core.app.getType() == ApplicationType.Android;

        dataDirectory = Core.settings.getDataDirectory();
        screenshotDirectory = dataDirectory.child("screenshots/");
        customMapDirectory = dataDirectory.child("maps/");
        mapPreviewDirectory = dataDirectory.child("previews/");
        saveDirectory = dataDirectory.child("saves/");
        tmpDirectory = dataDirectory.child("tmp/");
        modDirectory = dataDirectory.child("mods/");
        schematicDirectory = dataDirectory.child("schematics/");
        bebuildDirectory = dataDirectory.child("be_builds/");

        modDirectory.mkdirs();

        mods.load();
        maps.load();
    }

    public static void loadLogger(){
        if(loadedLogger) return;

        String[] tags = {"[green][D][]", "[royal][I][]", "[yellow][W][]", "[scarlet][E][]", ""};
        String[] stags = {"&lc&fb[D]", "&lg&fb[I]", "&ly&fb[W]", "&lr&fb[E]", ""};

        Array<String> logBuffer = new Array<>();
        Log.setLogger((level, text) -> {
            String result = text;
            String rawText = Log.format(stags[level.ordinal()] + "&fr " + text);
            System.out.println(rawText);

            result = tags[level.ordinal()] + " " + result;

            if(!headless && (ui == null || ui.scriptfrag == null)){
                logBuffer.add(result);
            }else if(!headless){
                ui.scriptfrag.addMessage(result);
            }
        });

        Events.on(ClientLoadEvent.class, e -> logBuffer.each(ui.scriptfrag::addMessage));

        loadedLogger = true;
    }

    public static void loadFileLogger(){
        if(loadedFileLogger) return;

        Core.settings.setAppName(appName);

        Writer writer = settings.getDataDirectory().child("last_log.txt").writer(false);
        LogHandler log = Log.getLogger();
        Log.setLogger(((level, text) -> {
            log.log(level, text);

            try{
                writer.write("[" + Character.toUpperCase(level.name().charAt(0)) +"] " + Log.removeCodes(text) + "\n");
                writer.flush();
            }catch(IOException e){
                e.printStackTrace();
                //ignore it
            }
        }));

        loadedFileLogger = true;
    }

    public static void loadSettings(){
        Core.settings.setAppName(appName);

        if(steam || (Version.modifier != null && Version.modifier.contains("steam"))){
            Core.settings.setDataDirectory(Core.files.local("saves/"));
        }

        Core.settings.defaults("locale", "default", "blocksync", true);
        Core.keybinds.setDefaults(Binding.values());
        Core.settings.load();

        Scl.setProduct(settings.getInt("uiscale", 100) / 100f);

        if(!loadLocales) return;

        try{
            //try loading external bundle
            Fi handle = Core.files.local("bundle");

            Locale locale = Locale.ENGLISH;
            Core.bundle = I18NBundle.createBundle(handle, locale);

            Log.info("NOTE: external translation bundle has been loaded.");

            if(!headless){
                Time.run(10f, () -> ui.showInfo("Note: You have successfully loaded an external translation bundle."));
            }
        }catch(Throwable e){
            //no external bundle found

            Fi handle = Core.files.internal("bundles/bundle");
            Locale locale;
            String loc = Core.settings.getString("locale");
            if(loc.equals("default")){
                locale = Locale.getDefault();
            }else{
                Locale lastLocale;
                if(loc.contains("_")){
                    String[] split = loc.split("_");
                    lastLocale = new Locale(split[0], split[1]);
                }else{
                    lastLocale = new Locale(loc);
                }

                locale = lastLocale;
            }

            Locale.setDefault(locale);
            Core.bundle = I18NBundle.createBundle(handle, locale);
        }
    }
}
