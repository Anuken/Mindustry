package io.anuke.mindustry.net;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.Player;

/**Class for storing all packets.*/
public class Packets {

    public static class Connect {
        public int id;
        public String addressTCP;
    }

    public static class Disconnect {
        public int id;
        public String addressTCP;
    }

    public static class WorldData extends Streamable{

    }

    public static class EntityDataPacket{
        public Player[] players;
        public int playerid;
    }

    public static class SyncPacket{

    }
}
