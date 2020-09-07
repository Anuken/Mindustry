package mindustry.net;

import arc.*;
import arc.struct.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.core.*;
import mindustry.io.*;

import java.io.*;
import java.nio.*;
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

    public static class Connect implements Packet{
        public String addressTCP;

        @Override
        public boolean isImportant(){
            return true;
        }
    }

    public static class Disconnect implements Packet{
        public String reason;

        @Override
        public boolean isImportant(){
            return true;
        }
    }

    public static class WorldStream extends Streamable{

    }

    public static class InvokePacket implements Packet{
        private static ReusableByteInStream bin;
        private static Reads read = new Reads(new DataInputStream(bin = new ReusableByteInStream()));

        public byte type, priority;

        public byte[] bytes;
        public int length;

        @Override
        public void read(ByteBuffer buffer){
            type = buffer.get();
            priority = buffer.get();
            short writeLength = buffer.getShort();
            bytes = new byte[writeLength];
            buffer.get(bytes);
        }

        @Override
        public void write(ByteBuffer buffer){
            buffer.put(type);
            buffer.put(priority);
            buffer.putShort((short)length);
            buffer.put(bytes, 0, length);
        }

        @Override
        public void reset(){
            priority = 0;
        }

        @Override
        public boolean isImportant(){
            return priority == 1;
        }

        @Override
        public boolean isUnimportant(){
            return priority == 2;
        }

        public Reads reader(){
            bin.setBytes(bytes);
            return read;
        }
    }

    /** Marks the beginning of a stream. */
    public static class StreamBegin implements Packet{
        private static int lastid;

        public int id = lastid++;
        public int total;
        public byte type;

        @Override
        public void write(ByteBuffer buffer){
            buffer.putInt(id);
            buffer.putInt(total);
            buffer.put(type);
        }

        @Override
        public void read(ByteBuffer buffer){
            id = buffer.getInt();
            total = buffer.getInt();
            type = buffer.get();
        }
    }

    public static class StreamChunk implements Packet{
        public int id;
        public byte[] data;

        @Override
        public void write(ByteBuffer buffer){
            buffer.putInt(id);
            buffer.putShort((short)data.length);
            buffer.put(data);
        }

        @Override
        public void read(ByteBuffer buffer){
            id = buffer.getInt();
            data = new byte[buffer.getShort()];
            buffer.get(data);
        }
    }

    public static class ConnectPacket implements Packet{
        public int version;
        public String versionType;
        public Seq<String> mods;
        public String name, uuid, usid;
        public boolean mobile;
        public int color;

        @Override
        public void write(ByteBuffer buffer){
            buffer.putInt(Version.build);
            TypeIO.writeString(buffer, versionType);
            TypeIO.writeString(buffer, name);
            TypeIO.writeString(buffer, usid);

            buffer.put(Base64Coder.decode(uuid));
            CRC32 crc = new CRC32();
            crc.update(Base64Coder.decode(uuid));
            buffer.putLong(crc.getValue());

            buffer.put(mobile ? (byte)1 : 0);
            buffer.putInt(color);
            buffer.put((byte)mods.size);
            for(int i = 0; i < mods.size; i++){
                TypeIO.writeString(buffer, mods.get(i));
            }
        }

        @Override
        public void read(ByteBuffer buffer){
            version = buffer.getInt();
            versionType = TypeIO.readString(buffer);
            name = TypeIO.readString(buffer);
            usid = TypeIO.readString(buffer);
            byte[] idbytes = new byte[16];
            buffer.get(idbytes);
            uuid = new String(Base64Coder.encode(idbytes));
            mobile = buffer.get() == 1;
            color = buffer.getInt();
            int totalMods = buffer.get();
            mods = new Seq<>(totalMods);
            for(int i = 0; i < totalMods; i++){
                mods.add(TypeIO.readString(buffer));
            }
        }
    }
}
