package io.anuke.mindustry.io;

import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Tile;


import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.playerGroup;
import static io.anuke.mindustry.Vars.world;

/**Class for specifying read/write methods for code generation.*/
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

    @WriteClass(KickReason.class)
    public static void writeKick(ByteBuffer buffer, KickReason reason){
        buffer.put((byte)reason.ordinal());
    }

    @ReadClass(KickReason.class)
    public static KickReason readKick(ByteBuffer buffer){
        return KickReason.values()[buffer.get()];
    }

    @WriteClass(Weapon.class)
    public static void writeWeapon(ByteBuffer buffer, Weapon weapon){
        buffer.put(weapon.id);
    }

    @ReadClass(Weapon.class)
    public static Weapon readWeapon(ByteBuffer buffer){
        return Upgrade.getByID(buffer.get());
    }

    @WriteClass(String.class)
    public static void writeString(ByteBuffer buffer, String string){
        if(string != null) {
            byte[] bytes = string.getBytes();
            buffer.putShort((short) bytes.length);
            buffer.put(bytes);
        }else{
            buffer.putShort((short)-1);
        }
    }

    @ReadClass(String.class)
    public static String readString(ByteBuffer buffer){
        short length = buffer.getShort();
        if(length != -1) {
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes);
        }else{
            return null;
        }
    }

    @WriteClass(byte[].class)
    public static void writeBytes(ByteBuffer buffer, byte[] bytes){
        buffer.putShort((short)bytes.length);
        buffer.put(bytes);
    }

    @ReadClass(byte[].class)
    public static byte[] readBytes(ByteBuffer buffer){
        short length = buffer.getShort();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }
}
