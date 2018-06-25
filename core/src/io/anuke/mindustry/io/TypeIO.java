package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.CarriableTrait;
import io.anuke.mindustry.entities.traits.CarryTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.net.TraceInfo;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.Entities;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

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
    public static Unit readUnit(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (Unit)Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(Bullet.class)
    public static void writeBullet(ByteBuffer buffer, Bullet bullet){
        buffer.putInt(bullet.getID());
    }

    @ReadClass(Bullet.class)
    public static Bullet readBullet(ByteBuffer buffer){
        int id = buffer.getInt();
        return bulletGroup.getByID(id);
    }

    @WriteClass(CarriableTrait.class)
    public static void writeCarriable(ByteBuffer buffer, CarriableTrait unit){
        if(unit == null){
            buffer.put((byte)-1);
            return;
        }
        buffer.put((byte)unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(CarriableTrait.class)
    public static CarriableTrait readCarriable(ByteBuffer buffer){
        byte gid = buffer.get();
        if(gid == -1){
            return null;
        }
        int id = buffer.getInt();
        return (CarriableTrait)Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(CarryTrait.class)
    public static void writeCarry(ByteBuffer buffer, CarryTrait unit){
        buffer.put((byte)unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(CarryTrait.class)
    public static CarryTrait readCarry(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (CarryTrait)Entities.getGroup(gid).getByID(id);
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

    @WriteClass(AdminAction.class)
    public static void writeAction(ByteBuffer buffer, AdminAction reason){
        buffer.put((byte)reason.ordinal());
    }

    @ReadClass(AdminAction.class)
    public static AdminAction readAction(ByteBuffer buffer){
        return AdminAction.values()[buffer.get()];
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

    @WriteClass(Mech.class)
    public static void writeMech(ByteBuffer buffer, Mech mech){
        buffer.put(mech.id);
    }

    @ReadClass(Mech.class)
    public static Mech readMech(ByteBuffer buffer){
        return Upgrade.getByID(buffer.get());
    }

    @WriteClass(Liquid.class)
    public static void writeLiquid(ByteBuffer buffer, Liquid liquid){
        buffer.put((byte)liquid.id);
    }

    @ReadClass(Liquid.class)
    public static Liquid readLiquid(ByteBuffer buffer){
        return Liquid.getByID(buffer.get());
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

    @WriteClass(TraceInfo.class)
    public static void writeTrace(ByteBuffer buffer, TraceInfo info){
        buffer.putInt(info.playerid);
        buffer.putShort((short)info.ip.getBytes().length);
        buffer.put(info.ip.getBytes());
        buffer.put(info.modclient ? (byte)1 : 0);
        buffer.put(info.android ? (byte)1 : 0);

        buffer.putInt(info.totalBlocksBroken);
        buffer.putInt(info.structureBlocksBroken);
        buffer.putInt(info.lastBlockBroken.id);

        buffer.putInt(info.totalBlocksPlaced);
        buffer.putInt(info.lastBlockPlaced.id);
        buffer.put(Base64Coder.decode(info.uuid));
    }

    @ReadClass(TraceInfo.class)
    public static TraceInfo readTrace(ByteBuffer buffer){
        int id = buffer.getInt();
        short iplen = buffer.getShort();
        byte[] ipb = new byte[iplen];
        buffer.get(ipb);

        TraceInfo info = new TraceInfo(new String(ipb));

        info.playerid = id;
        info.modclient = buffer.get() == 1;
        info.android = buffer.get() == 1;
        info.totalBlocksBroken = buffer.getInt();
        info.structureBlocksBroken = buffer.getInt();
        info.lastBlockBroken = Block.getByID(buffer.getInt());
        info.totalBlocksPlaced = buffer.getInt();
        info.lastBlockPlaced = Block.getByID(buffer.getInt());
        byte[] uuid = new byte[8];
        buffer.get(uuid);

        info.uuid = new String(Base64Coder.encode(uuid));
        return info;
    }
}
