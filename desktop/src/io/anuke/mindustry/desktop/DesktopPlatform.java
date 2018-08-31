package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.kryonet.DefaultThreadImpl;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.ui.dialogs.FileChooser;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.OS;
import io.anuke.ucore.util.Strings;

import java.io.File;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import static io.anuke.mindustry.Vars.*;

public class DesktopPlatform extends Platform{
    final static boolean useDiscord = OS.is64Bit;
    final static String applicationId = "398246104468291591";
    final static DateFormat format = SimpleDateFormat.getDateTimeInstance();
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;

        Vars.testMobile = isDebug() && Array.with(args).contains("-testMobile", false);

        if(useDiscord){
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            DiscordRPC.INSTANCE.Discord_Initialize(applicationId, handlers, true, "");
        }
    }

    @Override
    public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, String filter){
        new FileChooser(text, file -> file.extension().equalsIgnoreCase(filter), open, cons).show();
    }

    @Override
    public String format(Date date){
        return format.format(date);
    }

    @Override
    public String format(int number){
        return NumberFormat.getIntegerInstance().format(number);
    }

    @Override
    public void showError(String text){

    }

    @Override
    public String getLocaleName(Locale locale){
        return locale.getDisplayName(locale);
    }

    @Override
    public void updateRPC(){

        if(!useDiscord) return;

        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            presence.state = Strings.capitalize(state.mode.name());
            if(state.mode == GameMode.noWaves){
                presence.details = Strings.capitalize(world.getMap().name);
            }else{
                presence.details = Strings.capitalize(world.getMap().name) + " | Wave " + state.wave;
                presence.largeImageText = "Wave " + state.wave;
            }

            if(state.mode != GameMode.noWaves){
                presence.state = Strings.capitalize(state.mode.name());
            }else{
                presence.state = unitGroups[players[0].getTeam().ordinal()].size() == 1 ? "1 Unit Active" :
                (unitGroups[players[0].getTeam().ordinal()].size() + " Units Active");
            }

            if(Net.active()){
                presence.partyMax = 16;
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
    public void onGameExit(){
        if(useDiscord) DiscordRPC.INSTANCE.Discord_Shutdown();
    }

    @Override
    public boolean isDebug(){
        //honestly I'm just putting this ridiculous """anti-debug""" mess here to see if anyone bothers solving it without editing source
        boolean eq = args.length > 0 && args[0].equals(("-debug_12312333_" + System.getProperty("os.arch") + "nice" + (int)(Math.sin(System.getProperty("user.dir").hashCode()) * 100) + Thread.currentThread().getStackTrace()[1].toString()).hashCode() + "") && new File("../../desktop/build/").exists();
        if(eq) Log.info("--DEBUG MODE ACTIVE--");
        return eq;
    }

    @Override
    public ThreadProvider getThreadProvider(){
        return new DefaultThreadImpl();
    }

    @Override
    public String getUUID(){
        try{
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            NetworkInterface out;
            for(out = e.nextElement(); (out.getHardwareAddress() == null || !validAddress(out.getHardwareAddress())) && e.hasMoreElements(); out = e.nextElement()) ;

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
