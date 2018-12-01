package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Color;
import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.traits.CarriableTrait;
import io.anuke.mindustry.entities.traits.CarryTrait;
import io.anuke.mindustry.entities.traits.ShooterTrait;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.Entities;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.anuke.mindustry.Vars.*;

/** Class for specifying read/write methods for code generation.*/
@SuppressWarnings("unused")
public class TypeIO{

    @WriteClass(Player.class)
    public static void writePlayer(ByteBuffer buffer, Player player){
        if(player == null){
            buffer.putInt(-1);
        }else{
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
        buffer.put((byte) unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(Unit.class)
    public static Unit readUnit(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (Unit) Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(ShooterTrait.class)
    public static void writeShooter(ByteBuffer buffer, ShooterTrait trait){
        buffer.put((byte) trait.getGroup().getID());
        buffer.putInt(trait.getID());
    }

    @ReadClass(ShooterTrait.class)
    public static ShooterTrait readShooter(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (ShooterTrait) Entities.getGroup(gid).getByID(id);
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
            buffer.put((byte) -1);
            return;
        }
        buffer.put((byte) unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(CarriableTrait.class)
    public static CarriableTrait readCarriable(ByteBuffer buffer){
        byte gid = buffer.get();
        if(gid == -1){
            return null;
        }
        int id = buffer.getInt();
        return (CarriableTrait) Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(CarryTrait.class)
    public static void writeCarry(ByteBuffer buffer, CarryTrait unit){
        if(unit == null || unit.getGroup() == null){
            buffer.put((byte) -1);
            return;
        }
        buffer.put((byte) unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(CarryTrait.class)
    public static CarryTrait readCarry(ByteBuffer buffer){
        byte gid = buffer.get();
        if(gid == -1){
            return null;
        }
        int id = buffer.getInt();
        return (CarryTrait) Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(BaseUnit.class)
    public static void writeBaseUnit(ByteBuffer buffer, BaseUnit unit){
        buffer.put((byte) unitGroups[unit.getTeam().ordinal()].getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(BaseUnit.class)
    public static BaseUnit writeBaseUnit(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (BaseUnit) Entities.getGroup(gid).getByID(id);
    }

    @WriteClass(Tile.class)
    public static void writeTile(ByteBuffer buffer, Tile tile){
        buffer.putInt(tile == null ? -1 : tile.packedPosition());
    }

    @ReadClass(Tile.class)
    public static Tile readTile(ByteBuffer buffer){
        int position = buffer.getInt();
        return position == -1 ? null : world.tile(position);
    }

    @WriteClass(Block.class)
    public static void writeBlock(ByteBuffer buffer, Block block){
        buffer.put(block.id);
    }

    @ReadClass(Block.class)
    public static Block readBlock(ByteBuffer buffer){
        return content.block(buffer.get());
    }

    @WriteClass(BuildRequest[].class)
    public static void writeRequests(ByteBuffer buffer, BuildRequest[] requests){
        buffer.putShort((short)requests.length);
        for(BuildRequest request : requests){
            buffer.put(request.breaking ? (byte) 1 : 0);
            buffer.putInt(world.toPacked(request.x, request.y));
            if(!request.breaking){
                buffer.put(request.recipe.id);
                buffer.put((byte) request.rotation);
            }
        }
    }

    @ReadClass(BuildRequest[].class)
    public static BuildRequest[] readRequests(ByteBuffer buffer){
        short reqamount = buffer.getShort();
        BuildRequest[] reqs = new BuildRequest[reqamount];
        for(int i = 0; i < reqamount; i++){
            byte type = buffer.get();
            int position = buffer.getInt();
            BuildRequest currentRequest;

            if(type == 1){ //remove
                currentRequest = new BuildRequest(position % world.width(), position / world.width());
            }else{ //place
                byte recipe = buffer.get();
                byte rotation = buffer.get();
                currentRequest = new BuildRequest(position % world.width(), position / world.width(), rotation, content.recipe(recipe));
            }

            reqs[i] = (currentRequest);
        }

        return reqs;
    }

    @WriteClass(KickReason.class)
    public static void writeKick(ByteBuffer buffer, KickReason reason){
        buffer.put((byte) reason.ordinal());
    }

    @ReadClass(KickReason.class)
    public static KickReason readKick(ByteBuffer buffer){
        return KickReason.values()[buffer.get()];
    }

    @WriteClass(Team.class)
    public static void writeTeam(ByteBuffer buffer, Team reason){
        buffer.put((byte) reason.ordinal());
    }

    @ReadClass(Team.class)
    public static Team readTeam(ByteBuffer buffer){
        return Team.all[buffer.get()];
    }

    @WriteClass(AdminAction.class)
    public static void writeAction(ByteBuffer buffer, AdminAction reason){
        buffer.put((byte) reason.ordinal());
    }

    @ReadClass(AdminAction.class)
    public static AdminAction readAction(ByteBuffer buffer){
        return AdminAction.values()[buffer.get()];
    }

    @WriteClass(UnitCommand.class)
    public static void writeCommand(ByteBuffer buffer, UnitCommand reason){
        buffer.put((byte) reason.ordinal());
    }

    @ReadClass(UnitCommand.class)
    public static UnitCommand readCommand(ByteBuffer buffer){
        return UnitCommand.values()[buffer.get()];
    }

    @WriteClass(Effect.class)
    public static void writeEffect(ByteBuffer buffer, Effect effect){
        buffer.putShort((short) effect.id);
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
        return content.getByID(ContentType.weapon, buffer.get());
    }

    @WriteClass(Mech.class)
    public static void writeMech(ByteBuffer buffer, Mech mech){
        buffer.put(mech.id);
    }

    @ReadClass(Mech.class)
    public static Mech readMech(ByteBuffer buffer){
        return content.getByID(ContentType.mech, buffer.get());
    }

    @WriteClass(Liquid.class)
    public static void writeLiquid(ByteBuffer buffer, Liquid liquid){
        buffer.put(liquid.id);
    }

    @ReadClass(Liquid.class)
    public static Liquid readLiquid(ByteBuffer buffer){
        return content.liquid(buffer.get());
    }

    @WriteClass(AmmoType.class)
    public static void writeAmmo(ByteBuffer buffer, AmmoType type){
        buffer.put(type.id);
    }

    @ReadClass(AmmoType.class)
    public static AmmoType readAmmo(ByteBuffer buffer){
        return content.getByID(ContentType.weapon, buffer.get());
    }

    @WriteClass(BulletType.class)
    public static void writeBulletType(ByteBuffer buffer, BulletType type){
        buffer.put(type.id);
    }

    @ReadClass(BulletType.class)
    public static BulletType readBulletType(ByteBuffer buffer){
        return content.getByID(ContentType.bullet, buffer.get());
    }

    @WriteClass(Item.class)
    public static void writeItem(ByteBuffer buffer, Item item){
        buffer.put(item == null ? -1 : item.id);
    }

    @ReadClass(Item.class)
    public static Item readItem(ByteBuffer buffer){
        byte id = buffer.get();
        return id == -1 ? null : content.item(id);
    }

    @WriteClass(Recipe.class)
    public static void writeRecipe(ByteBuffer buffer, Recipe recipe){
        buffer.put(recipe.id);
    }

    @ReadClass(Recipe.class)
    public static Recipe readRecipe(ByteBuffer buffer){
        return content.recipe(buffer.get());
    }

    @WriteClass(String.class)
    public static void writeString(ByteBuffer buffer, String string){
        if(string != null){
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) bytes.length);
            buffer.put(bytes);
        }else{
            buffer.putShort((short) -1);
        }
    }

    @ReadClass(String.class)
    public static String readString(ByteBuffer buffer){
        short slength = buffer.getShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            buffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }else{
            return null;
        }
    }

    @WriteClass(byte[].class)
    public static void writeBytes(ByteBuffer buffer, byte[] bytes){
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
    }

    @ReadClass(byte[].class)
    public static byte[] readBytes(ByteBuffer buffer){
        short length = buffer.getShort();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public static void writeStringData(DataOutput buffer, String string) throws IOException{
        if(string != null){
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            buffer.writeShort((short) bytes.length);
            buffer.write(bytes);
        }else{
            buffer.writeShort((short) -1);
        }
    }

    public static String readStringData(DataInput buffer) throws IOException{
        short slength = buffer.readShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            buffer.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }else{
            return null;
        }
    }
}
