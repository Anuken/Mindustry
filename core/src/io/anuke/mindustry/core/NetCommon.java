package io.anuke.mindustry.core;

import io.anuke.mindustry.entities.Player;
import io.anuke.ucore.modules.Module;

import static io.anuke.mindustry.Vars.playerGroup;

public class NetCommon extends Module {

    public void sendMessage(String message){
        //TODO implement
    }

    public String colorizeName(int id, String name){
        Player player = playerGroup.getByID(id);
        if(name == null || player == null) return null;
        return "[#" + player.color.toString().toUpperCase() + "]" + name;
    }
}
