package mindustry.io;

import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;
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

    //TODO read/write enums like commands!

    public static void writeObject(Writes write, Object object){
        if(object == null){
            write.b((byte)0);
        }else if(object instanceof Integer){
            write.b((byte)1);
            write.i((Integer)object);
        }else if(object instanceof Long){
            write.b((byte)2);
            write.l((Long)object);
        }else if(object instanceof Float){
            write.b((byte)3);
            write.f((Float)object);
        }else if(object instanceof String){
            write.b((byte)4);
            writeString(write, (String)object);
            writeString(write, (String)object);
        }else if(object instanceof Content){
            Content map = (Content)object;
            write.b((byte)5);
            write.b((byte)map.getContentType().ordinal());
            write.s(map.id);
        }else if(object instanceof IntArray){
            write.b((byte)6);
            IntArray arr = (IntArray)object;
            write.s((short)arr.size);
            for(int i = 0; i < arr.size; i++){
                write.i(arr.items[i]);
            }
        }else if(object instanceof Point2){
            write.b((byte)7);
            write.i(((Point2)object).x);
            write.i(((Point2)object).y);
        }else if(object instanceof Point2[]){
            write.b((byte)8);
            write.b(((Point2[])object).length);
            for(int i = 0; i < ((Point2[])object).length; i++){
                write.i(((Point2[])object)[i].pack());
            }
        }else{
            throw new IllegalArgumentException("Unknown object type: " + object.getClass());
        }
    }

    public static Object readObject(Reads read){
        byte type = read.b();
        switch(type){
            case 0: return null;
            case 1: return read.i();
            case 2: return read.l();
            case 3: return read.f();
            case 4: return readString(read);
            case 5: return content.getByID(ContentType.all[read.b()], read.s());
            case 6: short length = read.s(); IntArray arr = new IntArray(); for(int i = 0; i < length; i ++) arr.add(read.i()); return arr;
            case 7: return new Point2(read.i(), read.i());
            case 8: byte len = read.b(); Point2[] out = new Point2[len]; for(int i = 0; i < len; i ++) out[i] = Point2.unpack(read.i()); return out;
            default: throw new IllegalArgumentException("Unknown object type: " + type);
        }
    }

    public static void writeEntity(Writes write, Entityc entity){
        write.i(entity == null ? -1 : entity.id());
    }

    public static <T extends Entityc> T readEntity(Reads read){
        return (T)Groups.all.getByID(read.i());
    }

    public static void writeTilec(Writes write, Tilec tile){
        write.i(tile == null ? -1 : tile.pos());
    }

    public static Tilec readTilec(Reads read){
        return world.ent(read.i());
    }

    public static void writeTile(Writes write, Tile tile){
        write.i(tile == null ? Point2.pack(-1, -1) : tile.pos());
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
        if(requests == null){
            write.s(-1);
            return;
        }

        write.s((short)requests.length);
        for(BuildRequest request : requests){
            write.b(request.breaking ? (byte)1 : 0);
            write.i(Point2.pack(request.x, request.y));
            if(!request.breaking){
                write.s(request.block.id);
                write.b((byte)request.rotation);
                write.b(request.hasConfig ? (byte)1 : 0);
                writeObject(write, request.config);
            }
        }
    }

    public static BuildRequest[] readRequests(Reads read){
        short reqamount = read.s();
        if(reqamount == -1){
            return null;
        }

        BuildRequest[] reqs = new BuildRequest[reqamount];
        for(int i = 0; i < reqamount; i++){
            byte type = read.b();
            int position = read.i();
            BuildRequest currentRequest;

            if(world.tile(position) == null){
                continue;
            }

            if(type == 1){ //remove
                currentRequest = new BuildRequest(Point2.x(position), Point2.y(position));
            }else{ //place
                short block = read.s();
                byte rotation = read.b();
                boolean hasConfig = read.b() == 1;
                Object config = readObject(read);
                currentRequest = new BuildRequest(Point2.x(position), Point2.y(position), rotation, content.block(block));
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
        write.b(reason.id);
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
        write.i(color.rgba());
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

    public static void writeWeather(Writes write, Weather item){
        write.s(item == null ? -1 : item.id);
    }

    public static Weather readWeather(Reads read){
        short id = read.s();
        return id == -1 ? null : content.getByID(ContentType.weather, id);
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
