package io.anuke.mindustry.io;

import io.anuke.annotations.Annotations.ReadClass;
import io.anuke.annotations.Annotations.WriteClass;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.traits.ShooterTrait;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.net.Administration.TraceInfo;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import java.io.*;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

/** Class for specifying read/write methods for code generation. */
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
        if(unit.getGroup() == null){
            buffer.put((byte)-1);
            return;
        }
        buffer.put((byte)unit.getGroup().getID());
        buffer.putInt(unit.getID());
    }

    @ReadClass(Unit.class)
    public static Unit readUnit(ByteBuffer buffer){
        byte gid = buffer.get();
        if(gid == -1) return null;
        int id = buffer.getInt();
        return (Unit)entities.get(gid).getByID(id);
    }

    @WriteClass(ShooterTrait.class)
    public static void writeShooter(ByteBuffer buffer, ShooterTrait trait){
        buffer.put((byte)trait.getGroup().getID());
        buffer.putInt(trait.getID());
    }

    @ReadClass(ShooterTrait.class)
    public static ShooterTrait readShooter(ByteBuffer buffer){
        byte gid = buffer.get();
        int id = buffer.getInt();
        return (ShooterTrait)entities.get(gid).getByID(id);
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

    @WriteClass(BaseUnit.class)
    public static void writeBaseUnit(ByteBuffer buffer, BaseUnit unit){
        buffer.put((byte)unit.getTeam().ordinal());
        buffer.putInt(unit.getID());
    }

    @ReadClass(BaseUnit.class)
    public static BaseUnit readBaseUnit(ByteBuffer buffer){
        byte tid = buffer.get();
        int id = buffer.getInt();
        return unitGroups[tid].getByID(id);
    }

    @WriteClass(Tile.class)
    public static void writeTile(ByteBuffer buffer, Tile tile){
        buffer.putInt(tile == null ? Pos.get(-1, -1) : tile.pos());
    }

    @ReadClass(Tile.class)
    public static Tile readTile(ByteBuffer buffer){
        return world.tile(buffer.getInt());
    }

    @WriteClass(Block.class)
    public static void writeBlock(ByteBuffer buffer, Block block){
        buffer.putShort(block.id);
    }

    @ReadClass(Block.class)
    public static Block readBlock(ByteBuffer buffer){
        return content.block(buffer.getShort());
    }

    @WriteClass(BuildRequest[].class)
    public static void writeRequests(ByteBuffer buffer, BuildRequest[] requests){
        buffer.putShort((short)requests.length);
        for(BuildRequest request : requests){
            buffer.put(request.breaking ? (byte)1 : 0);
            buffer.putInt(Pos.get(request.x, request.y));
            if(!request.breaking){
                buffer.putShort(request.block.id);
                buffer.put((byte)request.rotation);
                buffer.put(request.hasConfig ? (byte)1 : 0);
                buffer.putInt(request.config);
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

            if(world.tile(position) == null){
                continue;
            }

            if(type == 1){ //remove
                currentRequest = new BuildRequest(Pos.x(position), Pos.y(position));
            }else{ //place
                short block = buffer.getShort();
                byte rotation = buffer.get();
                boolean hasConfig = buffer.get() == 1;
                int config = buffer.getInt();
                currentRequest = new BuildRequest(Pos.x(position), Pos.y(position), rotation, content.block(block));
                if(hasConfig){
                    currentRequest.configure(config);
                }
            }

            reqs[i] = (currentRequest);
        }

        return reqs;
    }

    @WriteClass(KickReason.class)
    public static void writeKick(ByteBuffer buffer, KickReason reason){
        buffer.put((byte)reason.ordinal());
    }

    @ReadClass(KickReason.class)
    public static KickReason readKick(ByteBuffer buffer){
        return KickReason.values()[buffer.get()];
    }

    @WriteClass(Rules.class)
    public static void writeRules(ByteBuffer buffer, Rules rules){
        String string = JsonIO.write(rules);
        byte[] bytes = string.getBytes(charset);
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        writeString(buffer, JsonIO.write(rules));
    }

    @ReadClass(Rules.class)
    public static Rules readRules(ByteBuffer buffer){
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(length);
        String string = new String(bytes, charset);
        return JsonIO.read(Rules.class, string);
    }

    @WriteClass(Team.class)
    public static void writeTeam(ByteBuffer buffer, Team reason){
        buffer.put((byte)reason.ordinal());
    }

    @ReadClass(Team.class)
    public static Team readTeam(ByteBuffer buffer){
        return Team.all[buffer.get()];
    }

    @WriteClass(UnitCommand.class)
    public static void writeUnitCommand(ByteBuffer buffer, UnitCommand reason){
        buffer.put((byte)reason.ordinal());
    }

    @ReadClass(UnitCommand.class)
    public static UnitCommand readUnitCommand(ByteBuffer buffer){
        return UnitCommand.all[buffer.get()];
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

    @WriteClass(UnitType.class)
    public static void writeUnitType(ByteBuffer buffer, UnitType effect){
        buffer.putShort(effect.id);
    }

    @ReadClass(UnitType.class)
    public static UnitType readUnitType(ByteBuffer buffer){
        return content.getByID(ContentType.unit, buffer.getShort());
    }

    @WriteClass(Color.class)
    public static void writeColor(ByteBuffer buffer, Color color){
        buffer.putInt(Color.rgba8888(color));
    }

    @ReadClass(Color.class)
    public static Color readColor(ByteBuffer buffer){
        return new Color(buffer.getInt());
    }

    @WriteClass(Mech.class)
    public static void writeMech(ByteBuffer buffer, Mech mech){
        buffer.put((byte)mech.id);
    }

    @ReadClass(Mech.class)
    public static Mech readMech(ByteBuffer buffer){
        return content.getByID(ContentType.mech, buffer.get());
    }

    @WriteClass(Liquid.class)
    public static void writeLiquid(ByteBuffer buffer, Liquid liquid){
        buffer.putShort(liquid == null ? -1 : liquid.id);
    }

    @ReadClass(Liquid.class)
    public static Liquid readLiquid(ByteBuffer buffer){
        short id = buffer.getShort();
        return id == -1 ? null : content.liquid(id);
    }

    @WriteClass(BulletType.class)
    public static void writeBulletType(ByteBuffer buffer, BulletType type){
        buffer.putShort(type.id);
    }

    @ReadClass(BulletType.class)
    public static BulletType readBulletType(ByteBuffer buffer){
        return content.getByID(ContentType.bullet, buffer.getShort());
    }

    @WriteClass(Item.class)
    public static void writeItem(ByteBuffer buffer, Item item){
        buffer.putShort(item == null ? -1 : item.id);
    }

    @ReadClass(Item.class)
    public static Item readItem(ByteBuffer buffer){
        short id = buffer.getShort();
        return id == -1 ? null : content.item(id);
    }

    @WriteClass(String.class)
    public static void writeString(ByteBuffer buffer, String string){
        if(string != null){
            byte[] bytes = string.getBytes(charset);
            buffer.putShort((short)bytes.length);
            buffer.put(bytes);
        }else{
            buffer.putShort((short)-1);
        }
    }

    @ReadClass(String.class)
    public static String readString(ByteBuffer buffer){
        short slength = buffer.getShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            buffer.get(bytes);
            return new String(bytes, charset);
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
    public static void writeTraceInfo(ByteBuffer buffer, TraceInfo trace){
        writeString(buffer, trace.ip);
        writeString(buffer, trace.uuid);
        buffer.put(trace.modded ? (byte)1 : 0);
        buffer.put(trace.mobile ? (byte)1 : 0);
    }

    @ReadClass(TraceInfo.class)
    public static TraceInfo readTraceInfo(ByteBuffer buffer){
        return new TraceInfo(readString(buffer), readString(buffer), buffer.get() == 1, buffer.get() == 1);
    }

    public static void writeStringData(DataOutput buffer, String string) throws IOException{
        if(string != null){
            byte[] bytes = string.getBytes(charset);
            buffer.writeShort((short)bytes.length);
            buffer.write(bytes);
        }else{
            buffer.writeShort((short)-1);
        }
    }

    public static String readStringData(DataInput buffer) throws IOException{
        short slength = buffer.readShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            buffer.readFully(bytes);
            return new String(bytes, charset);
        }else{
            return null;
        }
    }
}
