package mindustry;

import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.ai.*;
import mindustry.async.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.editor.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.mod.*;
import mindustry.net.*;
import mindustry.service.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import static arc.Core.*;

public class Vars implements Loadable{
    /** Whether the game failed to launch last time. */
    public static boolean failedToLaunch = false;
    /** Whether to load locales.*/
    public static boolean loadLocales = true;
    /** Whether the logger is loaded. */
    public static boolean loadedLogger = false, loadedFileLogger = false;
    /** Whether to enable various experimental features (e.g. spawn positions for spawn groups) TODO change */
    public static boolean experimental = true;
    /** Name of current Steam player. */
    public static String steamPlayerName = "";
    /** Default accessible content types used for player-selectable icons. */
    public static final ContentType[] defaultContentIcons = {ContentType.item, ContentType.liquid, ContentType.block, ContentType.unit};
    /** Default rule environment. */
    public static final int defaultEnv = Env.terrestrial | Env.spores | Env.groundOil | Env.groundWater | Env.oxygen;
    /** Wall darkness radius. */
    public static final int darkRadius = 4;
    /** Maximum extra padding around deployment schematics. */
    public static final int maxLoadoutSchematicPad = 4;
    /** All schematic base64 starts with this string.*/
    public static final String schematicBaseStart ="bXNjaA";
    /** IO buffer size. */
    public static final int bufferSize = 8192;
    /** global charset, since Android doesn't support the Charsets class */
    public static final Charset charset = Charset.forName("UTF-8");
    /** main application name, capitalized */
    public static final String appName = "Mindustry";
    /** Github API URL. */
    public static final String ghApi = "https://api.github.com";
    /** URL for discord invite. */
    public static final String discordURL = "https://discord.gg/mindustry";
    /** URL the links to the wiki's modding guide.*/
    public static final String modGuideURL = "https://mindustrygame.github.io/wiki/modding/1-modding/";
    /** URL to the JSON file containing all the BE servers. Only queried in BE. */
    public static final String serverJsonBeURL = "https://raw.githubusercontent.com/Anuken/Mindustry/master/servers_be.json";
    /** URL to the JSON file containing all the stable servers.  */
    //TODO merge with v6 list upon release
    public static final String serverJsonURL = "https://raw.githubusercontent.com/Anuken/Mindustry/master/servers_v7.json";
    /** URL of the github issue report template.*/
    public static final String reportIssueURL = "https://github.com/Anuken/Mindustry/issues/new?labels=bug&template=bug_report.md";
    /** list of built-in servers.*/
    public static final Seq<ServerGroup> defaultServers = Seq.with();
    /** maximum size of any block, do not change unless you know what you're doing */
    public static final int maxBlockSize = 16;
    /** maximum distance between mine and core that supports automatic transferring */
    public static final float mineTransferRange = 220f;
    /** max chat message length */
    public static final int maxTextLength = 150;
    /** max player name length in bytes */
    public static final int maxNameLength = 40;
    /** displayed item size when ingame. */
    public static final float itemSize = 5f;
    /** units outside this bound will die instantly */
    public static final float finalWorldBounds = 250;
    /** default range for building */
    public static final float buildingRange = 220f;
    /** range for moving items */
    public static final float itemTransferRange = 220f;
    /** range for moving items for logic units */
    public static final float logicItemTransferRange = 45f;
    /** duration of time between turns in ticks */
    public static final float turnDuration = 2 * Time.toMinutes;
    /** chance of an invasion per turn, 1 = 100% */
    public static final float baseInvasionChance = 1f / 100f;
    /** how many minutes have to pass before invasions in a *captured* sector start */
    public static final float invasionGracePeriod = 20;
    /** min armor fraction damage; e.g. 0.05 = at least 5% damage */
    public static final float minArmorDamage = 0.1f;
    /** land/launch animation duration */
    public static final float coreLandDuration = 160f;
    /** size of tiles in units */
    public static final int tilesize = 8;
    /** size of one tile payload (^2) */
    public static final float tilePayload = tilesize * tilesize;
    /** icon sizes for UI */
    public static final float iconXLarge = 8*6f, iconLarge = 8*5f, iconMed = 8*4f, iconSmall = 8*3f;
    /** for map generator dialog */
    public static boolean updateEditorOnChange = false;
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
    /** maximum TCP packet size */
    public static final int maxTcpSize = 900;
    /** default server port */
    public static final int port = 6567;
    /** multicast discovery port.*/
    public static final int multicastPort = 20151;
    /** Maximum char length of mod subtitles in browser/viewer. */
    public static final int maxModSubtitleLength = 40;
    /** multicast group for discovery.*/
    public static final String multicastGroup = "227.2.7.7";
    /** whether the graphical game client has loaded */
    public static boolean clientLoaded = false;
    /** max GL texture size */
    public static int maxTextureSize = 2048;
    /** Maximum schematic size.*/
    public static int maxSchematicSize = 32;
    /** Whether to show sector info upon landing. */
    public static boolean showSectorLandInfo = true;
    /** Whether to check for memory use before taking screenshots. */
    public static boolean checkScreenshotMemory = true;
    /** Whether to prompt the user to confirm exiting. */
    public static boolean confirmExit = true;
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
    /** whether to clear sector saves when landing */
    public static boolean clearSectors = false;
    /** whether any light rendering is enabled */
    public static boolean enableLight = true;
    /** Whether to draw shadows of blocks at map edges and static blocks.
     * Do not change unless you know exactly what you are doing.*/
    public static boolean enableDarkness = true;
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
    /** file used to store launch ID */
    public static Fi launchIDFile;
    /** empty map, indicates no current map */
    public static Map emptyMap;
    /** empty tile for payloads */
    public static Tile emptyTile;
    /** map file extension */
    public static final String mapExtension = "msav";
    /** save file extension */
    public static final String saveExtension = "msav";
    /** schematic file extension */
    public static final String schematicExtension = "msch";
    /** path to the java executable */
    public static String javaPath;

    /** list of all locales that can be switched to */
    public static Locale[] locales;

    //the main executor will only have at most [cores] number of threads active
    public static ExecutorService mainExecutor = Threads.executor("Main Executor", OS.cores);

    public static FileTree tree = new FileTree();
    public static Net net;
    public static ContentLoader content;
    public static GameState state;
    public static EntityCollisions collisions;
    public static Waves waves;
    public static Platform platform = new Platform(){};
    public static Mods mods;
    public static Schematics schematics;
    public static BeControl becontrol;
    public static AsyncCore asyncCore;
    public static BaseRegistry bases;
    public static GlobalVars logicVars;
    public static MapEditor editor;
    public static GameService service = new GameService();

    public static Universe universe;
    public static World world;
    public static Maps maps;
    public static WaveSpawner spawner;
    public static BlockIndexer indexer;
    public static Pathfinder pathfinder;
    public static ControlPathfinder controlPath;
    public static FogControl fogControl;

    public static Control control;
    public static Logic logic;
    public static Renderer renderer;
    public static UI ui;
    public static NetServer netServer;
    public static NetClient netClient;

    public static Player player;

    @Override
    public void loadAsync(){
        loadSettings();
        init();
    }

    public static void init(){
        Groups.init();

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

            Arrays.sort(locales, Structs.comparing(LanguageDialog::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            locales = Seq.with(locales).add(new Locale("router")).toArray(Locale.class);
        }

        Version.init();
        CacheLayer.init();

        dataDirectory = settings.getDataDirectory();
        screenshotDirectory = dataDirectory.child("screenshots/");
        customMapDirectory = dataDirectory.child("maps/");
        mapPreviewDirectory = dataDirectory.child("previews/");
        saveDirectory = dataDirectory.child("saves/");
        tmpDirectory = dataDirectory.child("tmp/");
        modDirectory = dataDirectory.child("mods/");
        schematicDirectory = dataDirectory.child("schematics/");
        bebuildDirectory = dataDirectory.child("be_builds/");
        emptyMap = new Map(new StringMap());

        if(tree == null) tree = new FileTree();
        if(mods == null) mods = new Mods();

        content = new ContentLoader();
        waves = new Waves();
        collisions = new EntityCollisions();
        world = new World();
        universe = new Universe();
        becontrol = new BeControl();
        asyncCore = new AsyncCore();
        if(!headless) editor = new MapEditor();

        maps = new Maps();
        spawner = new WaveSpawner();
        indexer = new BlockIndexer();
        pathfinder = new Pathfinder();
        controlPath = new ControlPathfinder();
        fogControl = new FogControl();
        bases = new BaseRegistry();
        logicVars = new GlobalVars();
        javaPath =
            new Fi(OS.prop("java.home")).child("bin/java").exists() ? new Fi(OS.prop("java.home")).child("bin/java").absolutePath() :
            Core.files.local("jre/bin/java").exists() ? Core.files.local("jre/bin/java").absolutePath() :
            "java";

        state = new GameState();

        mobile = Core.app.isMobile() || testMobile;
        ios = Core.app.isIOS();
        android = Core.app.isAndroid();

        modDirectory.mkdirs();

        Events.on(ContentInitEvent.class, e -> {
            emptyTile = new Tile(Short.MAX_VALUE - 20, Short.MAX_VALUE - 20);
        });

        mods.load();
        maps.load();
    }

    /** Checks if a launch failure occurred.
     * If this is the case, failedToLaunch is set to true. */
    public static void checkLaunch(){
        settings.setAppName(appName);
        launchIDFile = settings.getDataDirectory().child("launchid.dat");

        if(launchIDFile.exists()){
            failedToLaunch = true;
        }else{
            failedToLaunch = false;
            launchIDFile.writeString("go away");
        }
    }

    /** Cleans up after a successful launch. */
    public static void finishLaunch(){
        if(launchIDFile != null){
            launchIDFile.delete();
        }
    }

    public static void loadLogger(){
        if(loadedLogger) return;

        String[] tags = {"[green][D][]", "[royal][I][]", "[yellow][W][]", "[scarlet][E][]", ""};
        String[] stags = {"&lc&fb[D]", "&lb&fb[I]", "&ly&fb[W]", "&lr&fb[E]", ""};

        Seq<String> logBuffer = new Seq<>();
        Log.logger = (level, text) -> {
            synchronized(logBuffer){
                String result = text;
                String rawText = Log.format(stags[level.ordinal()] + "&fr " + text);
                System.out.println(rawText);

                result = tags[level.ordinal()] + " " + result;

                if(!headless && (ui == null || ui.consolefrag == null)){
                    logBuffer.add(result);
                }else if(!headless){
                    if(!OS.isWindows){
                        for(String code : ColorCodes.values){
                            result = result.replace(code, "");
                        }
                    }

                    ui.consolefrag.addMessage(Log.removeColors(result));
                }
            }
        };

        Events.on(ClientLoadEvent.class, e -> logBuffer.each(ui.consolefrag::addMessage));

        loadedLogger = true;
    }

    public static void loadFileLogger(){
        if(loadedFileLogger) return;

        settings.setAppName(appName);

        try{
            Writer writer = settings.getDataDirectory().child("last_log.txt").writer(false);
            LogHandler log = Log.logger;
            Log.logger = (level, text) -> {
                log.log(level, text);

                try{
                    writer.write("[" + Character.toUpperCase(level.name().charAt(0)) + "] " + Log.removeColors(text) + "\n");
                    writer.flush();
                }catch(IOException e){
                    e.printStackTrace();
                    //ignore it
                }
            };
        }catch(Exception e){
            //handle log file not being found
            Log.err(e);
        }

        loadedFileLogger = true;
    }

    public static void loadSettings(){
        settings.setJson(JsonIO.json);
        settings.setAppName(appName);

        if(steam || (Version.modifier != null && Version.modifier.contains("steam"))){
            settings.setDataDirectory(Core.files.local("saves/"));
        }

        settings.defaults("locale", "default", "blocksync", true);
        keybinds.setDefaults(Binding.values());
        settings.setAutosave(false);
        settings.load();

        Scl.setProduct(settings.getInt("uiscale", 100) / 100f);

        if(!loadLocales) return;

        try{
            //try loading external bundle
            Fi handle = Core.files.local("bundle");

            Locale locale = Locale.ENGLISH;
            Core.bundle = I18NBundle.createBundle(handle, locale);

            Log.info("NOTE: external translation bundle has been loaded.");

            if(!headless){
                Time.run(10f, () -> ui.showInfo("Note: You have successfully loaded an external translation bundle.\n[accent]" + handle.absolutePath()));
            }
        }catch(Throwable e){
            //no external bundle found

            Fi handle = Core.files.internal("bundles/bundle");
            Locale locale;
            String loc = settings.getString("locale");
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

            //router
            if(locale.toString().equals("router")){
                bundle.debug("router");
            }
        }
    }
}
