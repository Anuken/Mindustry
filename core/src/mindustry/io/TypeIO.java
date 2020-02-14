package mindustry.io;

import arc.graphics.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.world.*;

import java.io.*;
import java.nio.*;

import static mindustry.Vars.*;

/** Class for specifying read/write methods for code generation. */
@SuppressWarnings("unused")
@TypeIOHandler
public class TypeIO{

    public static void writeEntity(Writes write, Entityc entity){
        write.i(entity == null ? -1 : entity.id());
    }

    public static <T extends Entityc> T readEntity(Reads read){
        return (T)Groups.all.getByID(read.i());
    }

    public static void writeTile(Writes write, Tile tile){
        write.i(tile == null ? Pos.get(-1, -1) : tile.pos());
    }

    public static Tile readTile(Reads read){
        return world.tile(read.i());
    }

    public static void writeBlock(Writes write, Block block){
        write.s(block.id);
    }

    public static Block readBlock(Reads read){
        return content.block(read.s());
    }

    public static void writeRequests(Writes write, BuildRequest[] requests){
        write.s((short)requests.length);
        for(BuildRequest request : requests){
            write.b(request.breaking ? (byte)1 : 0);
            write.i(Pos.get(request.x, request.y));
            if(!request.breaking){
                write.s(request.block.id);
                write.b((byte)request.rotation);
                write.b(request.hasConfig ? (byte)1 : 0);
                write.i(request.config);
            }
        }
    }

    public static BuildRequest[] readRequests(Reads read){
        short reqamount = read.s();
        BuildRequest[] reqs = new BuildRequest[reqamount];
        for(int i = 0; i < reqamount; i++){
            byte type = read.b();
            int position = read.i();
            BuildRequest currentRequest;

            if(world.tile(position) == null){
                continue;
            }

            if(type == 1){ //remove
                currentRequest = new BuildRequest(Pos.x(position), Pos.y(position));
            }else{ //place
                short block = read.s();
                byte rotation = read.b();
                boolean hasConfig = read.b() == 1;
                int config = read.i();
                currentRequest = new BuildRequest(Pos.x(position), Pos.y(position), rotation, content.block(block));
                if(hasConfig){
                    currentRequest.configure(config);
                }
            }

            reqs[i] = (currentRequest);
        }

        return reqs;
    }

    public static void writeKick(Writes write, KickReason reason){
        write.b((byte)reason.ordinal());
    }

    public static KickReason readKick(Reads read){
        return KickReason.values()[read.b()];
    }

    public static void writeRules(Writes write, Rules rules){
        String string = JsonIO.write(rules);
        byte[] bytes = string.getBytes(charset);
        write.i(bytes.length);
        write.b(bytes);
    }

    public static Rules readRules(Reads read){
        int length = read.i();
        String string = new String(read.b(new byte[length]), charset);
        return JsonIO.read(Rules.class, string);
    }

    public static void writeTeam(Writes write, Team reason){
        write.b((byte) (int)reason.id);
    }

    public static Team readTeam(Reads read){
        return Team.get(read.b());
    }

    public static void writeUnitCommand(Writes write, UnitCommand reason){
        write.b((byte)reason.ordinal());
    }

    public static UnitCommand readUnitCommand(Reads read){
        return UnitCommand.all[read.b()];
    }

    public static void writeAction(Writes write, AdminAction reason){
        write.b((byte)reason.ordinal());
    }

    public static AdminAction readAction(Reads read){
        return AdminAction.values()[read.b()];
    }

    public static void writeUnitDef(Writes write, UnitType effect){
        write.s(effect.id);
    }

    public static UnitType readUnitDef(Reads read){
        return content.getByID(ContentType.unit, read.s());
    }

    public static void writeColor(Writes write, Color color){
        write.i(Color.rgba8888(color));
    }

    public static Color readColor(Reads read){
        return new Color(read.i());
    }

    public static void writeLiquid(Writes write, Liquid liquid){
        write.s(liquid == null ? -1 : liquid.id);
    }

    public static Liquid readLiquid(Reads read){
        short id = read.s();
        return id == -1 ? null : content.liquid(id);
    }

    public static void writeBulletType(Writes write, BulletType type){
        write.s(type.id);
    }

    public static BulletType readBulletType(Reads read){
        return content.getByID(ContentType.bullet, read.s());
    }

    public static void writeItem(Writes write, Item item){
        write.s(item == null ? -1 : item.id);
    }

    public static Item readItem(Reads read){
        short id = read.s();
        return id == -1 ? null : content.item(id);
    }

    public static void writeString(Writes write, String string){
        if(string != null){
            byte[] bytes = string.getBytes(charset);
            write.s((short)bytes.length);
            write.b(bytes);
        }else{
            write.s((short)-1);
        }
    }

    public static String readString(Reads read){
        short slength = read.s();
        if(slength != -1){
            return new String(read.b(new byte[slength]), charset);
        }else{
            return null;
        }
    }

    public static void writeString(ByteBuffer write, String string){
        if(string != null){
            byte[] bytes = string.getBytes(charset);
            write.putShort((short)bytes.length);
            write.put(bytes);
        }else{
            write.putShort((short)-1);
        }
    }

    public static String readString(ByteBuffer read){
        short slength = read.getShort();
        if(slength != -1){
            byte[] bytes = new byte[slength];
            read.get(bytes);
            return new String(bytes, charset);
        }else{
            return null;
        }
    }

    public static void writeBytes(Writes write, byte[] bytes){
        write.s((short)bytes.length);
        write.b(bytes);
    }

    public static byte[] readBytes(Reads read){
        short length = read.s();
        return read.b(new byte[length]);
    }

    public static void writeTraceInfo(Writes write, TraceInfo trace){
        writeString(write, trace.ip);
        writeString(write, trace.uuid);
        write.b(trace.modded ? (byte)1 : 0);
        write.b(trace.mobile ? (byte)1 : 0);
    }

    public static TraceInfo readTraceInfo(Reads read){
        return new TraceInfo(readString(read), readString(read), read.b() == 1, read.b() == 1);
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
