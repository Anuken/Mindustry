package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import io.anuke.kryonet.DefaultThreadImpl;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.UCore;
import io.anuke.ucore.util.Strings;

import javax.swing.*;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import static io.anuke.mindustry.Vars.*;

public class DesktopPlatform extends Platform {
    final static boolean useDiscord = UCore.getPropertyNotNull("sun.arch.data.model").equals("64");
    final static String applicationId = "398246104468291591";
    final static DateFormat format = SimpleDateFormat.getDateTimeInstance();
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;

        if(useDiscord) {
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            DiscordRPC.INSTANCE.Discord_Initialize(applicationId, handlers, true, "");
        }
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
        JOptionPane.showMessageDialog(null, text);
    }

    @Override
    public String getLocaleName(Locale locale){
        return locale.getDisplayName(locale);
    }

    @Override
    public void updateRPC() {
        if(!useDiscord) return;

        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            presence.state = Strings.capitalize(state.mode.name()) + ", Solo";
            presence.details = Strings.capitalize(world.getMap().name) + " | Wave " + state.wave;
            presence.largeImageText = "Wave " + state.wave;

            if(Net.active()){
                presence.partyMax = 16;
                presence.partySize = playerGroup.size();
                presence.state = Strings.capitalize(state.mode.name());
            }
        }else{
            if(ui.editor != null && ui.editor.isShown()){
                presence.state = "In Editor";
            }else {
                presence.state = "In Menu";
            }
        }

        presence.largeImageKey = "logo";

        DiscordRPC.INSTANCE.Discord_UpdatePresence(presence);
    }

    @Override
    public void onGameExit() {
        if(useDiscord) DiscordRPC.INSTANCE.Discord_Shutdown();
    }

    @Override
    public boolean isDebug() {
        return args.length > 0 && args[0].equalsIgnoreCase("-debug");
    }

    @Override
    public ThreadProvider getThreadProvider() {
        return new DefaultThreadImpl();
    }

    @Override
    public byte[] getUUID() {
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            NetworkInterface out;
            for(out = e.nextElement(); out.getHardwareAddress() == null && e.hasMoreElements(); out = e.nextElement());

            byte[] bytes = out.getHardwareAddress();
            byte[] result = new byte[8];
            System.arraycopy(bytes, 0, result, 0, bytes.length);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
