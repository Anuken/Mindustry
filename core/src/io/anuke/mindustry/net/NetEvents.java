package io.anuke.mindustry.net;

import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;

public class NetEvents {

    @Remote(one = true, all = false, unreliable = true)
    public static void onSnapshot(byte[] snapshot, int snapshotID){

    }

    @Remote(unreliable = true, server = false)
    public static void onRecievedSnapshot(Player player, int snapshotID){
        Net.getConnection(player.clientid).lastSnapshotID = snapshotID;
    }
}
