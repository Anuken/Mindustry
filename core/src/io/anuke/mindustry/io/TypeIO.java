package io.anuke.mindustry.io;

import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Tile;


import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.playerGroup;
import static io.anuke.mindustry.Vars.world;

public class TypeIO {

    @WriteClass(Player.class)
    public static void writePlayer(ByteBuffer buffer, Player player){
        buffer.putInt(player.id);
    }

    @ReadClass(Player.class)
    public static Player readPlayer(ByteBuffer buffer){
        return playerGroup.getByID(buffer.getInt());
    }

    @WriteClass(Tile.class)
    public static void writeTile(ByteBuffer buffer, Tile tile){
        buffer.putInt(tile.packedPosition());
    }

    @ReadClass(Tile.class)
    public static Tile readTile(ByteBuffer buffer){
        return world.tile(buffer.getInt());
    }

    @WriteClass(String.class)
    public static void writeString(ByteBuffer buffer, String string){
        byte[] bytes = string.getBytes();
        buffer.putShort((short)bytes.length);
        buffer.put(bytes);
    }

    @ReadClass(String.class)
    public static String readString(ByteBuffer buffer){
        short length = buffer.getShort();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes);
    }
}
