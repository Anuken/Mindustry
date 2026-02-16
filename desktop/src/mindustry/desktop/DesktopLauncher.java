package mindustry.desktop;

import arc.*;
import arc.Files.*;
import arc.backend.sdl.*;
import arc.backend.sdl.jni.*;
import arc.discord.*;
import arc.discord.DiscordRPC.*;
import arc.filedialogs.*;
import arc.files.*;
import arc.func.*;
import arc.math.*;
import arc.profiling.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.serialization.*;
import com.codedisaster.steamworks.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.desktop.steam.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.Mods.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.service.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;

import java.io.*;

import static mindustry.Vars.*;

public class DesktopLauncher extends ClientLauncher{
    public final static long discordID = 610508934456934412L;
    public final String[] args;

    boolean useDiscord = !OS.hasProp("nodiscord"), loadError = false;
    Throwable steamError;

    public static void main(String[] arg){
        try{
            Vars.loadLogger();

            check32Bit();

            checkJavaVersion();

            new SdlApplication(new DesktopLauncher(arg), new SdlConfig(){{
                title = "Mindustry";
                maximized = true;
                coreProfile = true;
                width = 900;
                height = 700;

                //on Windows, Intel drivers might be buggy with OpenGL 3.x, so only use 2.x. See https://github.com/Anuken/Mindustry/issues/11041
                if(IntelGpuCheck.wasIntel()){
                    allowGl30 = false;
                    coreProfile = false;
                    glVersions = new int[][]{{2, 1}, {2, 0}};
                }else if(OS.isMac){
                    //MacOS supports 4.1 at most
                    glVersions = new int[][]{{4, 1}, {3, 2}, {2, 1}, {2, 0}};
                }else{
                    //try essentially every OpenGL version
                    glVersions = new int[][]{{4, 6}, {4, 5}, {4, 4}, {4, 1}, {3, 3}, {3, 2}, {3, 1}, {2, 1}, {2, 0}};
                }

                for(int i = 0; i < arg.length; i++){
                    if(arg[i].charAt(0) == '-'){
                        String name = arg[i].substring(1);
                        switch(name){
                            case "width" -> width = Strings.parseInt(arg[i + 1], width);
                            case "height" -> height = Strings.parseInt(arg[i + 1], height);
                            case "gl" -> {
                                String str = arg[i + 1];
                                if(str.contains(".")){
                                    String[] split = str.split("\\.");
                                    if(split.length == 2 && Strings.canParsePositiveInt(split[0]) && Strings.canParsePositiveInt(split[1])){
                                        glVersions = new int[][]{{Strings.parseInt(split[0]), Strings.parseInt(split[1])}};
                                        allowGl30 = true; //when a version is explicitly specified always allow GL 3
                                        break;
                                    }
                                }
                                Log.err("Invalid GL version format string: '@'. GL version must be of the form <major>.<minor>", str);
                            }
                            case "coreGl" -> coreProfile = true;
                            case "compatibilityGl" -> coreProfile = false;
                            case "antialias" -> samples = 16;
                            case "debug" -> Log.level = LogLevel.debug;
                            case "maximized" -> maximized = Boolean.parseBoolean(arg[i + 1]);
                            case "testMobile" -> testMobile = true;
                            case "gltrace" -> {
                                Events.on(ClientCreateEvent.class, e -> {
                                    var profiler = new GLProfiler(Core.graphics);
                                    profiler.enable();
                                    Core.app.addListener(new ApplicationListener(){
                                        @Override
                                        public void update(){
                                            profiler.reset();
                                        }
                                    });
                                });
                            }
                        }
                    }
                }
                setWindowIcon(FileType.internal, "icons/icon_64.png");
            }});
        }catch(Throwable e){
            handleCrash(e);
        }
    }

    static void checkJavaVersion(){
        if(OS.javaVersionNumber < 17){
            //this is technically a lie: Java 25 isn't actually required (17 is), but I want people to get the highest available version they can.
            //Java 25 *might* be required in the future for FFM bindings.
            ErrorDialog.show("Java 25 is required to run Mindustry. Your version: " + OS.javaVersionNumber + "\n" +
            "\n" +
            "Please uninstall your current Java version, and download Java 25.\n" +
            "\n" +
            "It is recommended to download Java from adoptium.net.\n" +
            "Do not download from java.com, as that will give you Java 8 by default.");
        }
    }

    static void check32Bit(){
        if(OS.isWindows && !OS.is64Bit){
            try{
                Version.init();
            }catch(Throwable ignored){
            }

            boolean steam = Version.modifier != null && Version.modifier.contains("steam");
            String versionWarning = "";

            if(steam){
                versionWarning = "\n\nIf you are unable to upgrade, consider switching to the legacy v7 branch on Steam, which is the last release that supported 32-bit windows:\n(properties -> betas -> select version-7.0 in the drop-down box).";
            }else if(OS.javaVersion.equals("1.8.0_151-1-ojdkbuild")){ //version string of JVM packaged with the 32-bit version of the game on itch/steam
                versionWarning = "\n\nMake sure you have downloaded the 64-bit version of the game, not the 32-bit one.";
            }else if(OS.javaVersionNumber < 25){
                //technically, java 25 isn't required yet, but it might be in the future, so tell users to get that one
                versionWarning = "\n\nYour current Java version is: " + OS.javaVersionNumber + ". To run the game, upgrade to Java 25 on a 64-bit machine.";
            }

            ErrorDialog.show("You are running a 32-bit installation of Windows and/or a 32-bit JVM. 32-bit windows is no longer supported." + versionWarning);
        }
    }

    public DesktopLauncher(String[] args){
        this.args = args;

        Version.init();
        boolean useSteam = Version.modifier.contains("steam");

        if(useDiscord){
            Threads.daemon(() -> {
                try{
                    DiscordRPC.connect(discordID);
                    Log.info("Initialized Discord rich presence.");
                    Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC::close));
                }catch(NoDiscordClientException none){
                    //don't log if no client is found
                    useDiscord = false;
                }catch(Throwable t){
                    useDiscord = false;
                    Log.warn("Failed to initialize Discord RPC - you are likely using a JVM <16.");
                }
            });
        }

        if(useSteam){

            Events.on(ClientLoadEvent.class, event -> {
                if(steamError != null){
                    Core.app.post(() -> Core.app.post(() -> Core.app.post(() -> {
                        ui.showErrorMessage(Core.bundle.format("steam.error", (steamError.getMessage() == null) ? steamError.getClass().getSimpleName() : steamError.getClass().getSimpleName() + ": " + steamError.getMessage()));
                    })));
                }
            });

            try{
                SteamAPI.loadLibraries();

                if(!SteamAPI.init()){
                    loadError = true;
                    Log.err("Steam client not running.");
                }else{
                    initSteam(args);
                    Vars.steam = true;
                }

                if(SteamAPI.restartAppIfNecessary(SVars.steamID)){
                    System.exit(0);
                }
            }catch(Throwable e){
                steam = false;
                Log.err("Failed to load Steam native libraries.");
                logSteamError(e);
            }
        }
    }

    void logSteamError(Throwable e){
        steamError = e;
        loadError = true;
        Log.err(e);
        try(OutputStream s = new FileOutputStream("steam-error-log-" + System.nanoTime() + ".txt")){
            String log = Strings.neatError(e);
            s.write(log.getBytes());
        }catch(Exception e2){
            Log.err(e2);
        }
    }

    void initSteam(String[] args){
        SVars.net = new SNet(new ArcNetProvider());
        SVars.stats = new SStats();
        SVars.workshop = new SWorkshop();
        SVars.user = new SUser();
        boolean[] isShutdown = {false};

        service = new GameService(){

            @Override
            public boolean enabled(){
                return true;
            }

            @Override
            public void completeAchievement(String name){
                SVars.stats.stats.setAchievement(name);
                SVars.stats.stats.storeStats();
            }

            @Override
            public void clearAchievement(String name){
                SVars.stats.stats.clearAchievement(name);
                SVars.stats.stats.storeStats();
            }

            @Override
            public boolean isAchieved(String name){
                return SVars.stats.stats.isAchieved(name, false);
            }

            @Override
            public int getStat(String name, int def){
                return SVars.stats.stats.getStatI(name, def);
            }

            @Override
            public void setStat(String name, int amount){
                SVars.stats.stats.setStatI(name, amount);
            }

            @Override
            public void storeStats(){
                SVars.stats.onUpdate();
            }
        };

        Events.on(ClientLoadEvent.class, event -> {
            Core.settings.defaults("name", SVars.net.friends.getPersonaName());
            if(player.name.isEmpty()){
                player.name = SVars.net.friends.getPersonaName();
                Core.settings.put("name", player.name);
            }
            steamPlayerName = SVars.net.friends.getPersonaName();
            //update callbacks
            Core.app.addListener(new ApplicationListener(){
                @Override
                public void update(){
                    if(SteamAPI.isSteamRunning()){
                        SteamAPI.runCallbacks();
                    }
                }
            });

            Core.app.post(() -> {
                if(args.length >= 2 && args[0].equals("+connect_lobby")){
                    try{
                        long id = Long.parseLong(args[1]);
                        ui.join.connect("steam:" + id, port);
                    }catch(Exception e){
                        Log.err("Failed to parse steam lobby ID: @", e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        });

        Events.on(DisposeEvent.class, event -> {
            SteamAPI.shutdown();
            isShutdown[0] = true;
        });

        //steam shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if(!isShutdown[0]){
                SteamAPI.shutdown();
            }
        }));
    }

    static void handleCrash(Throwable e){
        boolean badGPU = false;
        String finalMessage = Strings.getFinalMessage(e);
        String total = Strings.getCauses(e).toString();

        if(total.contains("Couldn't create window") || total.contains("OpenGL 2.0 or higher") || total.toLowerCase().contains("pixel format") || total.contains("GLEW")|| total.contains("unsupported combination of formats")){

            message(
                total.contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers:\n" + finalMessage :
                            "Your graphics card does not support the right OpenGL features.\n" +
                                    "Try to update your graphics drivers. If this doesn't work, your computer may not support Mindustry.\n\n" +
                                    "Full message: " + finalMessage);
            badGPU = true;
        }

        boolean fbgp = badGPU;

        LoadedMod cause = CrashHandler.getModCause(e);
        String causeString = cause == null ? (Structs.contains(e.getStackTrace(), st -> st.getClassName().contains("rhino.gen.")) ? "A mod or script has caused Mindustry to crash.\nConsider disabling your mods if the issue persists.\n" : "Mindustry has crashed.") :
            "'" + cause.meta.displayName + "' (" + cause.name + ") has caused Mindustry to crash.\nConsider disabling this mod if issues persist.\n";

        CrashHandler.handle(e, file -> {
            Throwable fc = Strings.getFinalCause(e);
            if(!fbgp){
                message(causeString + "\nThe logs have been saved in:\n" + file.getAbsolutePath() + "\n" + fc.getClass().getSimpleName().replace("Exception", "") + (fc.getMessage() == null ? "" : ":\n" + fc.getMessage()));
            }
        });
    }

    @Override
    public void showFileChooser(boolean open, String title, String extension, Cons<Fi> cons){
        showNativeFileChooser(title, open, cons, extension);
    }

    @Override
    public void showMultiFileChooser(Cons<Fi> cons, String... extensions){
        showNativeFileChooser("@open", true, cons, extensions);
    }

    void showNativeFileChooser(String title, boolean open, Cons<Fi> cons, String... shownExtensions){
        String formatted = (title.startsWith("@") ? Core.bundle.get(title.substring(1)) : title).replaceAll("\"", "'");

        //this should never happen unless someone is being dumb with the parameters
        String[] ext = shownExtensions == null || shownExtensions.length == 0 ? new String[]{""} : shownExtensions;

        //native file dialog
        Threads.daemon(() -> {
            try{
                FileDialogs.loadNatives();

                String result;
                String[] patterns = new String[ext.length];
                for(int i = 0; i < ext.length; i++){
                    patterns[i] = "*." + ext[i];
                }

                //on MacOS, .msav is not properly recognized until I put garbage into the array?
                if(patterns.length == 1 && OS.isMac && open){
                    patterns = new String[]{"", "*." + ext[0]};
                }

                if(open){
                    result = FileDialogs.openFileDialog(formatted, FileChooser.getLastDirectory().absolutePath(), patterns, "." + ext[0] + " files", false);
                }else{
                    result = FileDialogs.saveFileDialog(formatted, FileChooser.getLastDirectory().child("file." + ext[0]).absolutePath(), patterns, "." + ext[0] + " files");
                }

                if(result == null) return;

                if(result.length() > 1 && result.contains("\n")){
                    result = result.split("\n")[0];
                }

                //cancelled selection, ignore result
                if(result.isEmpty() || result.equals("\n")) return;
                if(result.endsWith("\n")) result = result.substring(0, result.length() - 1);
                if(result.contains("\n")) throw new IOException("invalid input: \"" + result + "\"");

                Fi file = Core.files.absolute(result);
                Core.app.post(() -> {
                    FileChooser.setLastDirectory(file.isDirectory() ? file : file.parent());

                    if(!open){
                        cons.get(file.parent().child(file.nameWithoutExtension() + "." + ext[0]));
                    }else{
                        cons.get(file);
                    }
                });
            }catch(Throwable error){
                Log.err("Failure to execute native file chooser", error);
                Core.app.post(() -> {
                    if(ext.length > 1){
                        showMultiFileChooser(cons, ext);
                    }else{
                        Platform.defaultFileDialog(open, formatted, ext[0], cons);
                    }
                });
            }
        });
    }

    @Override
    public Seq<Fi> getWorkshopContent(Class<? extends Publishable> type){
        return !steam ? super.getWorkshopContent(type) : SVars.workshop.getWorkshopFiles(type);
    }

    @Override
    public void viewListing(Publishable pub){
        SVars.workshop.viewListing(pub);
    }

    @Override
    public void viewListingID(String id){
        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + id);
    }

    @Override
    public NetProvider getNet(){
        return steam ? SVars.net : new ArcNetProvider();
    }

    @Override
    public void openWorkshop(){
        SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/app/1127400/workshop/");
    }

    @Override
    public void publish(Publishable pub){
        SVars.workshop.publish(pub);
    }

    @Override
    public void inviteFriends(){
        SVars.net.showFriendInvites();
    }

    @Override
    public void updateLobby(){
        if(SVars.net != null){
            SVars.net.updateLobby();
        }
    }

    @Override
    public void updateRPC(){
        //if we're using neither discord nor steam, do no work
        if(!useDiscord && !steam) return;

        //common elements they each share
        boolean inGame = state.isGame();
        String gameMapWithWave = "Unknown Map";
        String gameMode = "";
        String gamePlayersSuffix = "";
        String uiState = "";

        if(inGame){
            gameMapWithWave = Strings.capitalize(Strings.stripColors(state.map.name()));

            if(state.rules.waves){
                gameMapWithWave += " | Wave " + state.wave;
            }
            gameMode = state.rules.pvp ? "PvP" : state.rules.attackMode ? "Attack" : state.rules.infiniteResources ? "Sandbox" : "Survival";
            if(net.active() && Groups.player.size() > 1){
                gamePlayersSuffix = " | " + Groups.player.size() + " Players";
            }
        }else{
            if(ui.editor != null && ui.editor.isShown()){
                uiState = "In Editor";
            }else if(ui.planet != null && ui.planet.isShown()){
                uiState = "In Launch Selection";
            }else{
                uiState = "In Menu";
            }
        }

        if(useDiscord){
            RichPresence presence = new RichPresence();

            if(inGame){
                presence.state = gameMode + gamePlayersSuffix;
                presence.details = gameMapWithWave;
                if(state.rules.waves){
                    presence.largeImageText = "Wave " + state.wave;
                }
            }else{
                presence.state = uiState;
            }

            presence.largeImageKey = "logo";

            try{
                DiscordRPC.send(presence);
            }catch(Exception ignored){}
        }

        if(steam){
            //Steam mostly just expects us to give it a nice string, but it apparently expects "steam_display" to always be a loc token, so I've uploaded this one which just passes through 'steam_status' raw.
            SVars.net.friends.setRichPresence("steam_display", "#steam_status_raw");

            if(inGame){
                SVars.net.friends.setRichPresence("steam_status", gameMapWithWave);
            }else{
                SVars.net.friends.setRichPresence("steam_status", uiState);
            }
        }
    }

    @Override
    public String getUUID(){
        if(steam){
            try{
                byte[] result = new byte[8];
                new Rand(SVars.user.user.getSteamID().getAccountID()).nextBytes(result);
                return new String(Base64Coder.encode(result));
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        return super.getUUID();
    }

    private static void message(String message){
        SDL.SDL_ShowSimpleMessageBox(SDL.SDL_MESSAGEBOX_ERROR, "oh no", message);
    }
}
