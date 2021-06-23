package mindustry.desktop;

import arc.*;
import arc.Files.*;
import arc.backend.sdl.*;
import arc.backend.sdl.jni.*;
import arc.discord.*;
import arc.discord.DiscordRPC.*;
import arc.files.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import com.codedisaster.steamworks.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.desktop.steam.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.service.*;
import mindustry.type.*;

import java.io.*;

import static mindustry.Vars.*;

public class DesktopLauncher extends ClientLauncher{
    public final static long discordID = 610508934456934412L;
    boolean useDiscord = !OS.hasProp("nodiscord"), loadError = false;
    Throwable steamError;

    public static void main(String[] arg){
        try{
            Vars.loadLogger();
            new SdlApplication(new DesktopLauncher(arg), new SdlConfig(){{
                title = "Mindustry";
                maximized = true;
                width = 900;
                height = 700;
                //enable gl3 with command-line argument
                if(Structs.contains(arg, "-gl3")){
                    gl30 = true;
                }
                setWindowIcon(FileType.internal, "icons/icon_64.png");
            }});
        }catch(Throwable e){
            handleCrash(e);
        }
    }

    public DesktopLauncher(String[] args){
        Version.init();
        boolean useSteam = Version.modifier.contains("steam");
        testMobile = Seq.with(args).contains("-testMobile");

        if(useDiscord){
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
        }

        if(useSteam){
            //delete leftover dlls
            for(Fi other : new Fi(".").parent().list()){
                if(other.name().contains("steam") && (other.extension().equals("dll") || other.extension().equals("so") || other.extension().equals("dylib"))){
                    other.delete();
                }
            }

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
            }catch(NullPointerException ignored){
                steam = false;
                Log.info("Running in offline mode.");
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
        Cons<Runnable> dialog = Runnable::run;
        boolean badGPU = false;
        String finalMessage = Strings.getFinalMessage(e);
        String total = Strings.getCauses(e).toString();

        if(total.contains("Couldn't create window") || total.contains("OpenGL 2.0 or higher") || total.toLowerCase().contains("pixel format") || total.contains("GLEW")|| total.contains("unsupported combination of formats")){

            dialog.get(() -> message(
                total.contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers:\n" + finalMessage :
                            "Your graphics card does not support the right OpenGL features.\n" +
                                    "Try to update your graphics drivers. If this doesn't work, your computer may not support Mindustry.\n\n" +
                                    "Full message: " + finalMessage));
            badGPU = true;
        }

        boolean fbgp = badGPU;

        CrashSender.send(e, file -> {
            Throwable fc = Strings.getFinalCause(e);
            if(!fbgp){
                dialog.get(() -> message("A crash has occured. It has been saved in:\n" + file.getAbsolutePath() + "\n" + fc.getClass().getSimpleName().replace("Exception", "") + (fc.getMessage() == null ? "" : ":\n" + fc.getMessage())));
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
            gameMode = state.rules.pvp ? "PvP" : state.rules.attackMode ? "Attack" : "Survival";
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

    private boolean validAddress(byte[] bytes){
        if(bytes == null) return false;
        byte[] result = new byte[8];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        return !new String(Base64Coder.encode(result)).equals("AAAAAAAAAOA=") && !new String(Base64Coder.encode(result)).equals("AAAAAAAAAAA=");
    }
}
