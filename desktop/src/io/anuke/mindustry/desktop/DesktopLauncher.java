package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.*;
import com.codedisaster.steamworks.*;
import io.anuke.arc.*;
import io.anuke.arc.Files.*;
import io.anuke.arc.backends.sdl.*;
import io.anuke.arc.backends.sdl.jni.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.func.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.Log.*;
import io.anuke.arc.util.io.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.core.Version;
import io.anuke.mindustry.desktop.steam.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import static io.anuke.mindustry.Vars.*;

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
            new SdlApplication(new DesktopLauncher(arg), new SdlConfig(){{
                title = "Mindustry";
                maximized = true;
                depth = 0;
                stencil = 0;
                width = 900;
                height = 700;
                setWindowIcon(FileType.Internal, "icons/icon_64.png");
            }});
        }catch(Throwable e){
            handleCrash(e);
        }
    }

    public DesktopLauncher(String[] args){
        Log.setUseColors(false);
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
            FileHandle file = new FileHandle(".");
            for(FileHandle other : file.parent().list()){
                if(other.name().contains("steam") && (other.extension().equals("dll") || other.extension().equals("so") || other.extension().equals("dylib"))){
                    other.delete();
                }
            }

            StringBuilder base = new StringBuilder();
            Log.setLogger(new LogHandler(){
                  @Override
                  public void print(String text, Object... args){
                      String out = Log.format(text, false, args);

                      base.append(out).append("\n");
                  }
            });

            Events.on(ClientLoadEvent.class, event -> {
                Label[] label = {null};
                boolean[] visible = {false};
                Core.scene.table(t -> {
                    t.touchable(Touchable.disabled);
                    t.top().left();
                    t.update(() -> {
                        if(Core.input.keyTap(KeyCode.BACKTICK) && (loadError || System.getProperty("user.name").equals("anuke") || Version.modifier.contains("beta"))){
                            visible[0] = !visible[0];
                        }

                        t.toFront();
                    });
                    t.table(Styles.black3, f -> label[0] = f.add("").get()).visible(() -> visible[0]);
                    label[0].getText().append(base);
                });

                Log.setLogger(new LogHandler(){
                    @Override
                    public void print(String text, Object... args){
                        super.print(text, args);
                        String out = Log.format(text, false, args);

                        int maxlen = 2048;

                        if(label[0].getText().length() > maxlen){
                            label[0].setText(label[0].getText().substring(label[0].getText().length() - maxlen));
                        }

                        label[0].getText().append(out).append("\n");
                        label[0].invalidateHierarchy();
                    }
                });

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
        try(OutputStream s = new FileOutputStream(new File("steam-error-log-" + System.nanoTime() + ".txt"))){
            String log = Strings.parseException(e, true);
            s.write(log.getBytes());
        }catch(Exception e2){
            Log.err(e2);
        }
    }

    void fallbackSteam(){
        try{
            String name = "steam_api";
            if(OS.isMac || OS.isLinux) name = "lib" + name;
            if(OS.isWindows && OS.is64Bit) name += "64";
            name += (OS.isLinux ? ".so" : OS.isMac ? ".dylib" : ".dll");
            Streams.copyStream(getClass().getResourceAsStream(name), new FileOutputStream(name));
            System.loadLibrary(new File(name).getAbsolutePath());
        }catch(Throwable e){
            logSteamError(e);
        }
    }

    void initSteam(String[] args){
        SVars.net = new SNet(new ArcNetImpl());
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
            Array<Throwable> causes = Strings.getCauses(e);
            Throwable fc = causes.find(t -> t instanceof ModLoadException);
            if(fc == null) fc = Strings.getFinalCause(e);
            Throwable cause = fc;
            if(!fbgp){
                dialog.get(() -> message("A crash has occured. It has been saved in:\n" + file.getAbsolutePath() + "\n" + cause.getClass().getSimpleName().replace("Exception", "") + (cause.getMessage() == null ? "" : ":\n" + cause.getMessage())));
            }
        });
    }

    @Override
    public Array<FileHandle> getWorkshopContent(Class<? extends Publishable> type){
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
        return steam ? SVars.net : new ArcNetImpl();
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
