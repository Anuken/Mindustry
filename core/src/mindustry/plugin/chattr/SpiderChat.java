package mindustry.plugin.chattr;

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
        color = Color.scarlet;
        isServer = true;
        team = Team.derelict;
    }};

    public void message(Player player, String raw){
        Call.sendMessage(player.prefix() + "[orange]> [white]" + raw);
    }

    public void connected(Player player){
        message(server, Strings.format("{0} [white]has connected", player.name));
    }

    public void disconnected(Player player){
        message(server, Strings.format("{0} [white]has disconnected", player.name));
    }

    public void kicked(Player player){
        message(server, Strings.format("{0} [white]has been kicked", player.name));
    }

    public void banned(Player player){
        message(server, Strings.format("{0} [white]has been banned", player.name));
    }

    public String colorcase(String string, Color color){
        Log.info(string);
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
