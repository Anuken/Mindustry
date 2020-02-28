package mindustry.plugin.spiderchat;

import arc.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.plugin.*;

import java.util.regex.*;

public class SpiderChat extends Plugin implements ApplicationListener{

    public static Player server = new Player(){{
        name = "Server";
        color = Color.lightGray;
        isServer = true;
        team = Team.derelict;
    }};

    public void message(Player player, String raw){
        Call.sendMessage(player.prefix() + "[orange]> [white]" + raw);
    }

    public void connected(Player player){
        player.icon = Iconc.download;
        message(player, "connected");
//        message(server, Strings.format("[#{0}]{1} [white]connected", player.color, player.name));
//        String msg = player.getTeam().color() + Iconc.download + " connected " + "[#" + player.color + "]" + player.name;
//        Call.sendMessage(msg);
    }

    public void disconnected(Player player){
        player.icon = Iconc.upload;
        message(player, "disconnected");
    }

    public void kicked(Player player){
        player.icon = Iconc.warning;
        message(player, "kicked");
    }

    public void banned(Player player){
        player.icon = Iconc.block;
        message(player, "banned");
    }

    public String colorcase(String string, Color color){
        Pattern p = Pattern.compile("[A-Z]+");
        Matcher m = p.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "[#" + color + "]" + m.group() + "[]");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
