package io.anuke.mindustry.desktop;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import io.anuke.kryonet.DefaultThreadImpl;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.util.Strings;

import javax.swing.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static io.anuke.mindustry.Vars.*;

public class DesktopPlatform extends Platform {
    DateFormat format = SimpleDateFormat.getDateTimeInstance();
    DiscordRPC lib = DiscordRPC.INSTANCE;
    String[] args;

    public DesktopPlatform(String[] args){
        this.args = args;
        String applicationId = "398246104468291591";
        DiscordEventHandlers handlers = new DiscordEventHandlers();

        lib.Discord_Initialize(applicationId, handlers, true, "");
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
        DiscordRichPresence presence = new DiscordRichPresence();

        if(!state.is(State.menu)){
            presence.state = Strings.capitalize(state.mode.name()) + ", Solo";
            presence.details = Strings.capitalize(world.getMap().name) + " | Wave " + state.wave;
            presence.largeImageText = "Wave " + state.wave;
            if(Net.active() ){
                presence.partyMax = 16;
                presence.partySize = Net.getConnections().size;
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

        lib.Discord_UpdatePresence(presence);
    }

    @Override
    public void onGameExit() {
        lib.Discord_Shutdown();
    }

    @Override
    public boolean isDebug() {
        return args.length > 0 && args[0].equalsIgnoreCase("-debug");
    }

    @Override
    public ThreadProvider getThreadProvider() {
        return new DefaultThreadImpl();
    }
}
