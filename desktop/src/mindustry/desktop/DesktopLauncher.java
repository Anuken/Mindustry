package mindustry.desktop;

import arc.*;
import arc.Files.*;
import arc.backend.sdl.*;
import arc.files.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import club.minnced.discord.rpc.*;
import com.codedisaster.steamworks.*;
import io.anuke.arc.backends.sdl.jni.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.core.*;
import mindustry.desktop.steam.*;
import mindustry.game.EventType.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.type.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import static mindustry.Vars.*;

public class DesktopLauncher extends ClientLauncher{
    public final static String discordID = "610508934456934412";

    boolean useDiscord = OS.is64Bit, loadError = false;
    Throwable steamError;

    static{
        if(!Charset.forName("US-ASCII").newEncoder().canEncode(System.getProperty("user.name", ""))){
            System.setProperty("com.codedisaster.steamworks.SharedLibraryExtractPath", new File("").getAbsolutePath());
        }
    }

    public static void main(String[] arg){
        try{
            Vars.loadLogger();
            new SdlApplication(new DesktopLauncher(arg), new SdlConfig(){{
                title = "Mindustry";
                maximized = true;
                depth = 0;
                stencil = 0;
                width = 900;
                height = 700;
                setWindowIcon(FileType.internal, "icons/icon_64.png");
            }});
        }catch(Throwable e){
            handleCrash(e);
        }
    }

    public DesktopLauncher(String[] args){
        Version.init();
        boolean useSteam = Version.modifier.contains("steam");
        testMobile = Array.with(args).contains("-testMobile");

        if(useDiscord){
            try{
                DiscordEventHandlers handlers = new DiscordEventHandlers();
                DiscordRPC.INSTANCE.Discord_Initialize(discordID, handlers, true, "1127400");
                Log.info("Initialized Discord rich presence.");

                Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC.INSTANCE::Discord_Shutdown));
            }catch(Throwable t){
                useDiscord = false;
                Log.err("Failed to initialize discord.", t);
            }
        }

        if(useSteam){
            //delete leftover dlls
            Fi file = new Fi(".");
            for(Fi other : file.parent().list()){
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
        try(OutputStream s = new FileOutputStream(new File("steam-error-log-" + System.nanoTime() + ".txt"))){
            String log = Strings.parseException(e, true);
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

        Events.on(ClientLoadEvent.class, event -> {
            player.name = SVars.net.friends.getPersonaName();
            Core.settings.defaults("name", SVars.net.friends.getPersonaName());
            Core.settings.put("name", player.name);
            Core.settings.save();
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
                        Log.err("Failed to parse steam lobby ID: {0}", e.getMessage());
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

        if(e.getMessage() != null && (e.getMessage().contains("Couldn't create window") || e.getMessage().contains("OpenGL 2.0 or higher") || e.getMessage().toLowerCase().contains("pixel format"))){

            dialog.get(() -> message(
                    e.getMessage().contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers:\n" + e.getMessage() :
                            "Your graphics card does not support OpenGL 2.0!\n" +
                                    "Try to update your graphics drivers.\n\n" +
                                    "(If that doesn't work, your computer just doesn't support Mindustry.)"));
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
    public Array<Fi> getWorkshopContent(Class<? extends Publishable> type){
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
        SVars.net.updateLobby();
    }

    @Override
    public void updateRPC(){
        if(!useDiscord) return;

        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            String map = world.getMap() == null ? "Unknown Map" : world.isZone() ? world.getZone().localizedName : Strings.capitalize(world.getMap().name());
            String mode = state.rules.pvp ? "PvP" : state.rules.attackMode ? "Attack" : "Survival";
            String players =  net.active() && playerGroup.size() > 1 ? " | " + playerGroup.size() + " Players" : "";

            presence.state = mode + players;

            if(!state.rules.waves){
                presence.details = map;
            }else{
                presence.details = map + " | Wave " + state.wave;
                presence.largeImageText = "Wave " + state.wave;
            }
        }else{
            if(ui.editor != null && ui.editor.isShown()){
                presence.state = "In Editor";
            }else if(ui.deploy != null && ui.deploy.isShown()){
                presence.state = "In Launch Selection";
            }else{
                presence.state = "In Menu";
            }
        }

        presence.largeImageKey = "logo";

        DiscordRPC.INSTANCE.Discord_UpdatePresence(presence);
    }

    @Override
    public String getUUID(){
        if(steam){
            try{
                byte[] result = new byte[8];
                new RandomXS128(SVars.user.user.getSteamID().getAccountID()).nextBytes(result);
                return new String(Base64Coder.encode(result));
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        try{
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            NetworkInterface out;
            for(out = e.nextElement(); (out.getHardwareAddress() == null || !validAddress(out.getHardwareAddress())) && e.hasMoreElements(); out = e.nextElement());

            byte[] bytes = out.getHardwareAddress();
            byte[] result = new byte[8];
            System.arraycopy(bytes, 0, result, 0, bytes.length);

            String str = new String(Base64Coder.encode(result));

            if(str.equals("AAAAAAAAAOA=") || str.equals("AAAAAAAAAAA=")) throw new RuntimeException("Bad UUID.");

            return str;
        }catch(Exception e){
            return super.getUUID();
        }
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
