package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.*;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.util.OS;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.serialization.Base64Coder;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.dialogs.FileChooser;

import java.net.NetworkInterface;
import java.util.Enumeration;

import static io.anuke.mindustry.Vars.*;

public class DesktopPlatform extends Platform{
    final static boolean useDiscord = OS.is64Bit;
    final static String applicationId = "398246104468291591";
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;

        testMobile = Array.with(args).contains("-testMobile");

        if(useDiscord){
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            DiscordRPC.INSTANCE.Discord_Initialize(applicationId, handlers, true, "");

            Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC.INSTANCE::Discord_Shutdown));
        }
    }

    @Override
    public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, String filter){
        new FileChooser(text, file -> file.extension().equalsIgnoreCase(filter), open, cons).show();
    }

    @Override
    public void updateRPC(){

        if(!useDiscord) return;

        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            presence.state = state.rules.waves ? "Survival" : "Attack";
            if(world.getMap() == null){
                presence.details = "Unknown Map";
            }else if(!state.rules.waves){
                presence.details = Strings.capitalize(world.getMap().name());
            }else{
                presence.details = Strings.capitalize(world.getMap().name()) + " | Wave " + state.wave;
                presence.largeImageText = "Wave " + state.wave;
            }

            presence.state = unitGroups[player.getTeam().ordinal()].size() == 1 ? "1 Unit Active" :
            (unitGroups[player.getTeam().ordinal()].size() + " Units Active");

            if(Net.active()){
                presence.partyMax = 100;
                presence.partySize = playerGroup.size();
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
            for(out = e.nextElement(); (out.getHardwareAddress() == null || !validAddress(out.getHardwareAddress())) && e.hasMoreElements(); out = e.nextElement())
                ;

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
