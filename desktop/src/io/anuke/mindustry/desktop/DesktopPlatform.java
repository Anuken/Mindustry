package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.*;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.util.*;
import io.anuke.arc.util.serialization.Base64Coder;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.CrashSender;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.dialogs.FileChooser;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.net.NetworkInterface;
import java.util.Enumeration;

import static io.anuke.mindustry.Vars.*;

public class DesktopPlatform extends Platform{
    static boolean useDiscord = OS.is64Bit;
    final static String applicationId = "398246104468291591";
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;

        testMobile = Array.with(args).contains("-testMobile");

        if(useDiscord){
            try{
                DiscordEventHandlers handlers = new DiscordEventHandlers();
                DiscordRPC.INSTANCE.Discord_Initialize(applicationId, handlers, true, "");

                Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC.INSTANCE::Discord_Shutdown));
            }catch(Throwable t){
                useDiscord = false;
                Log.err("Failed to initialize discord.", t);
            }
        }
    }

    static void handleCrash(Throwable e){
        Consumer<Runnable> dialog = r -> new Thread(r).start();
        boolean badGPU = false;

        if(e.getMessage() != null && (e.getMessage().contains("Couldn't create window") || e.getMessage().contains("OpenGL 2.0 or higher"))){

            dialog.accept(() -> TinyFileDialogs.tinyfd_messageBox("oh no",
                    e.getMessage().contains("Couldn't create window") ? "A graphics initialization error has occured! Try to update your graphics drivers.\nReport this to the developer." :
                            "Your graphics card does not support OpenGL 2.0!\n" +
                                    "Try to update your graphics drivers.\n\n" +
                                    "(If that doesn't work, your computer just doesn't support Mindustry.)", "ok", "error", true));
            badGPU = true;
        }

        boolean fbgp = badGPU;

        CrashSender.send(e, file -> {
            if(!fbgp){
                dialog.accept(() -> TinyFileDialogs.tinyfd_messageBox("oh no", "A crash has occured. It has been saved in:\n" + file.getAbsolutePath(), "ok", "error", true));
            }
        });
    }

    @Override
    public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, Predicate<String> filetype){
        new FileChooser(text, file -> filetype.test(file.extension().toLowerCase()), open, cons).show();
    }

    @Override
    public void updateRPC(){

        if(!useDiscord) return;

        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            presence.state = state.rules.pvp ? "PvP" : state.rules.waves ? "Survival" : "Attack";
            if(world.getMap() == null){
                presence.details = "Unknown Map";
            }else if(!state.rules.waves){
                presence.details = Strings.capitalize(world.getMap().name());
            }else{
                presence.details = Strings.capitalize(world.getMap().name()) + " | Wave " + state.wave;
                presence.largeImageText = "Wave " + state.wave;
            }

            if(Net.active() && playerGroup.size() > 1){
                presence.state = (state.rules.pvp ? "PvP | " : "") + playerGroup.size() + " Players";
            }else if(state.rules.waves){
                presence.state = "Survival";
            }
        }else{
            if(ui.editor != null && ui.editor.isShown()){
                presence.state = "In Editor";
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

            if(str.equals("AAAAAAAAAOA=")) throw new RuntimeException("Bad UUID.");

            return str;
        }catch(Exception e){
            return super.getUUID();
        }
    }

    private boolean validAddress(byte[] bytes){
        if(bytes == null) return false;
        byte[] result = new byte[8];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        return !new String(Base64Coder.encode(result)).equals("AAAAAAAAAOA=");
    }
}
