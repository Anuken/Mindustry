package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Color;
import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.Entities;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.playerGroup;
import static io.anuke.mindustry.Vars.world;

/**Class for specifying read/write methods for code generation.*/
public class TypeIO {

    @WriteClass(Player.class)
    public static void writePlayer(ByteBuffer buffer, Player player){
        if(player == null){
            buffer.putInt(-1);
        }else {
            buffer.putInt(player.id);
        }
    }

    @ReadClass(Player.class)
    public static Player readPlayer(ByteBuffer buffer){
        int id = buffer.getInt();
        return id == -1 ? null : playerGroup.getByID(id);
    }

    @WriteClass(Unit.class)
    public static void writeUnit(ByteBuffer buffer, Unit unit){
        buffer.put((byte)unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(Unit.class)
    public static Unit writeUnit(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (Unit)Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(BaseUnit.class)
    public static void writeBaseUnit(ByteBuffer buffer, BaseUnit unit){
        buffer.put((byte)unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(BaseUnit.class)
    public static BaseUnit writeBaseUnit(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (BaseUnit)Entities.getGroup(gid).getByID(id);
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

    @WriteClass(Team.class)
    public static void writeTeam(ByteBuffer buffer, Team reason){
        buffer.put((byte)reason.ordinal());
    }

    @ReadClass(Team.class)
    public static Team readTeam(ByteBuffer buffer){
        return Team.values()[buffer.get()];
    }

    @WriteClass(Effect.class)
    public static void writeEffect(ByteBuffer buffer, Effect effect){
        buffer.putShort((short)effect.id);
    }

    @ReadClass(Effect.class)
    public static Effect readEffect(ByteBuffer buffer){
        return Effects.getEffect(buffer.getShort());
    }

    @WriteClass(Color.class)
    public static void writeColor(ByteBuffer buffer, Color color){
        buffer.putInt(Color.rgba8888(color));
    }

    @ReadClass(Color.class)
    public static Color readColor(ByteBuffer buffer){
        return new Color(buffer.getInt());
    }

    @WriteClass(Weapon.class)
    public static void writeWeapon(ByteBuffer buffer, Weapon weapon){
        buffer.put(weapon.id);
    }

    @ReadClass(Weapon.class)
    public static Weapon readWeapon(ByteBuffer buffer){
        return Upgrade.getByID(buffer.get());
    }

    @WriteClass(AmmoType.class)
    public static void writeAmmo(ByteBuffer buffer, AmmoType type){
        buffer.put(type.id);
    }

    @ReadClass(AmmoType.class)
    public static AmmoType readAmmo(ByteBuffer buffer){
        return AmmoType.getByID(buffer.get());
    }

    @WriteClass(BulletType.class)
    public static void writeBulletType(ByteBuffer buffer, BulletType type){
        buffer.put((byte)type.id);
    }

    @ReadClass(BulletType.class)
    public static BulletType readBulletType(ByteBuffer buffer){
        return BulletType.getByID(buffer.get());
    }

    @WriteClass(Item.class)
    public static void writeItem(ByteBuffer buffer, Item item){
        buffer.put((byte)item.id);
    }

    @ReadClass(Item.class)
    public static Item readItem(ByteBuffer buffer){
        return Item.getByID(buffer.get());
    }

    @WriteClass(Recipe.class)
    public static void writeRecipe(ByteBuffer buffer, Recipe recipe){
        buffer.put((byte)recipe.id);
    }

    @ReadClass(Recipe.class)
    public static Recipe readRecipe(ByteBuffer buffer){
        return Recipe.getByID(buffer.get());
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
