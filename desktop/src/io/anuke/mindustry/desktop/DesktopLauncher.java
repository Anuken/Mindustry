package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.*;
import com.codedisaster.steamworks.*;
import io.anuke.arc.*;
import io.anuke.arc.Files.*;
import io.anuke.arc.backends.sdl.*;
import io.anuke.arc.backends.sdl.jni.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
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
import io.anuke.mindustry.desktop.steam.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.ui.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

import static io.anuke.mindustry.Vars.*;


public class DesktopLauncher extends ClientLauncher{
    public final static String discordID = "610508934456934412";

    boolean useDiscord = OS.is64Bit, showConsole = OS.getPropertyNotNull("user.name").equals("anuke");

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
            if(showConsole){
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
                            if(Core.input.keyTap(KeyCode.BACKTICK)){
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
                });
            }

            try{
                try{
                    SteamAPI.loadLibraries();
                }catch(Throwable t){
                    Log.err(t);
                    fallbackSteam();
                }

                if(!SteamAPI.init()){
                    Log.err("Steam client not running.");
                }else{
                    initSteam(args);
                    Vars.steam = true;
                }
            }catch(Throwable e){
                steam = false;
                Log.err("Failed to load Steam native libraries.");
                Log.err(e);
            }
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
            Log.err(e);
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
        Consumer<Runnable> dialog = Runnable::run;
        boolean badGPU = false;

        if(e.getMessage() != null && (e.getMessage().contains("Couldn't create window") || e.getMessage().contains("OpenGL 2.0 or higher"))){

            dialog.accept(() -> message(
                    e.getMessage().contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers:\n" + e.getMessage() :
                            "Your graphics card does not support OpenGL 2.0!\n" +
                                    "Try to update your graphics drivers.\n\n" +
                                    "(If that doesn't work, your computer just doesn't support Mindustry.)"));
            badGPU = true;
        }

        boolean fbgp = badGPU;

        CrashSender.send(e, file -> {
            Throwable cause = Strings.getFinalCause(e);
            if(!fbgp){
                dialog.accept(() -> message("A crash has occured. It has been saved in:\n" + file.getAbsolutePath() + "\n" + cause.getClass().getSimpleName().replace("Exception", "") + (cause.getMessage() == null ? "" : ":\n" + cause.getMessage())));
            }
        });
    }

    @Override
    public Array<FileHandle> getExternalMaps(){
        return !steam ? super.getExternalMaps() : SVars.workshop.getMapFiles();
    }

    @Override
    public Array<FileHandle> getExternalMods(){
        return !steam ? super.getExternalMods() : SVars.workshop.getModFiles();
    }

    @Override
    public void viewMapListing(Map map){
        viewListing(map.file.parent().name());
    }

    @Override
    public void viewListing(String mapid){
        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + mapid);
    }

    @Override
    public void viewMapListingInfo(Map map){
        SVars.workshop.viewMapListingInfo(map);
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
    public void publishMap(Map map){
        SVars.workshop.publishMap(map);
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
