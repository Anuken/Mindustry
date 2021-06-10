package mindustry.net;

import arc.*;
import arc.struct.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.core.*;
import mindustry.io.*;

import java.util.zip.*;

/**
 * Class for storing all packets.
 */
public class Packets{

    public enum KickReason{
        kick, clientOutdated, serverOutdated, banned, gameover(true), recentKick,
        nameInUse, idInUse, nameEmpty, customClient, serverClose, vote, typeMismatch,
        whitelist, playerLimit, serverRestarting;

        public final boolean quiet;

        KickReason(){
            this(false);
        }

        KickReason(boolean quiet){
            this.quiet = quiet;
        }

        @Override
        public String toString(){
            return Core.bundle.get("server.kicked." + name());
        }

        public String extraText(){
            return Core.bundle.getOrNull("server.kicked." + name() + ".text");
        }
    }

    public enum AdminAction{
        kick, ban, trace, wave
    }

    /** Generic client connection event. */
    public static class Connect extends Packet{
        public String addressTCP;

        @Override
        public int getPriority(){
            return priorityHigh;
        }
    }

    /** Generic client disconnection event. */
    public static class Disconnect extends Packet{
        public String reason;

        @Override
        public int getPriority(){
            return priorityHigh;
        }
    }

    public static class WorldStream extends Streamable{

    }

    /** Marks the beginning of a stream. */
    public static class StreamBegin extends Packet{
        private static int lastid;

        public int id = lastid++;
        public int total;
        public byte type;

        @Override
        public void write(Writes buffer){
            buffer.i(id);
            buffer.i(total);
            buffer.b(type);
        }

        @Override
        public void read(Reads buffer){
            id = buffer.i();
            total = buffer.i();
            type = buffer.b();
        }
    }

    public static class StreamChunk extends Packet{
        public int id;
        public byte[] data;

        @Override
        public void write(Writes buffer){
            buffer.i(id);
            buffer.s((short)data.length);
            buffer.b(data);
        }

        @Override
        public void read(Reads buffer){
            id = buffer.i();
            data = buffer.b(buffer.s());
        }
    }

    public static class ConnectPacket extends Packet{
        public int version;
        public String versionType;
        public Seq<String> mods;
        public String name, locale, uuid, usid;
        public boolean mobile;
        public int color;

        @Override
        public void write(Writes buffer){
            buffer.i(Version.build);
            TypeIO.writeString(buffer, versionType);
            TypeIO.writeString(buffer, name);
            TypeIO.writeString(buffer, locale);
            TypeIO.writeString(buffer, usid);

            byte[] b = Base64Coder.decode(uuid);
            buffer.b(b);
            CRC32 crc = new CRC32();
            crc.update(Base64Coder.decode(uuid), 0, b.length);
            buffer.l(crc.getValue());

            buffer.b(mobile ? (byte)1 : 0);
            buffer.i(color);
            buffer.b((byte)mods.size);
            for(int i = 0; i < mods.size; i++){
                TypeIO.writeString(buffer, mods.get(i));
            }
        }

        @Override
        public void read(Reads buffer){
            version = buffer.i();
            versionType = TypeIO.readString(buffer);
            name = TypeIO.readString(buffer);
            locale = TypeIO.readString(buffer);
            usid = TypeIO.readString(buffer);
            byte[] idbytes =  buffer.b(16);
            uuid = new String(Base64Coder.encode(idbytes));
            mobile = buffer.b() == 1;
            color = buffer.i();
            int totalMods = buffer.b();
            mods = new Seq<>(totalMods);
            for(int i = 0; i < totalMods; i++){
                mods.add(TypeIO.readString(buffer));
            }
        }
    }
}
