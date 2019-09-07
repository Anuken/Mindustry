package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.*;
import io.anuke.arc.backends.sdl.jni.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.ui.dialogs.*;

import java.net.*;
import java.util.*;

import static io.anuke.mindustry.Vars.*;


public class DesktopPlatform extends ClientLauncher{
    static boolean useDiscord = OS.is64Bit;
    final static String applicationId = "610508934456934412";
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;

        testMobile = Array.with(args).contains("-testMobile");

        if(useDiscord){
            try{
                DiscordEventHandlers handlers = new DiscordEventHandlers();
                DiscordRPC.INSTANCE.Discord_Initialize(applicationId, handlers, true, "1127400");
                Log.info("Initialized Discord rich presence.");

                Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC.INSTANCE::Discord_Shutdown));
            }catch(Throwable t){
                useDiscord = false;
                Log.err("Failed to initialize discord.", t);
            }
        }
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
            if(!fbgp){
                dialog.accept(() -> message("A crash has occured. It has been saved in:\n" + file.getAbsolutePath() + "\n" + (e.getMessage() == null ? "" : "\n" + e.getMessage())));
            }
        });
    }

    @Override
    public void updateRPC(){
        if(!useDiscord) return;

        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            String map = world.getMap() == null ? "Unknown Map" : world.isZone() ? world.getZone().localizedName : Strings.capitalize(world.getMap().name());
            String mode = state.rules.pvp ? "PvP" : state.rules.attackMode ? "Attack" : "Survival";
            String players =  Net.active() && playerGroup.size() > 1 ? " | " + playerGroup.size() + " Players" : "";

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
