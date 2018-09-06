package io.anuke.mindustry.net;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.annotations.Annotations.Variant;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.maxTextLength;
import static io.anuke.mindustry.Vars.playerGroup;

public class NetEvents{

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void sendMessage(Player player, String message){
        if(message.length() > maxTextLength){
            throw new ValidateException(player, "Player has sent a message above the text limit.");
        }

        Log.info("&y{0}: &lb{1}", (player == null || player.name == null ? "" : player.name), message);

        if(Vars.ui != null){
            Vars.ui.chatfrag.addMessage(message, player == null ? null : colorizeName(player.id, player.name));
        }
    }

    @Remote(called = Loc.server, variants = Variant.both, forward = true)
    public static void sendMessage(String message){
        if(Vars.ui != null){
            Vars.ui.chatfrag.addMessage(message, null);
        }
    }

    private static String colorizeName(int id, String name){
        Player player = playerGroup.getByID(id);
        if(name == null || player == null) return null;
        return "[#" + player.color.toString().toUpperCase() + "]" + name;
    }
}
