package io.anuke.mindustry.net;

import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Tile;

public class NetEvents {

    @Remote(unreliable = true, one = true, all = false)
    public static void callClientMethod(int something, Player player, String str, boolean bool){
        System.out.println("Called " + something + " ? " + bool);
    }

    @Remote(local = false)
    public static void someOtherMethod(Tile tile){
        System.out.println("Called with tile " + tile);
    }
}
