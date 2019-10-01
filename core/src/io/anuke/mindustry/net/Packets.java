package io.anuke.mindustry.net;

import io.anuke.arc.Core;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.serialization.Base64Coder;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.io.TypeIO;

import java.nio.ByteBuffer;

/**
 * Class for storing all packets.
 */
public class Packets{

    public enum KickReason{
        kick, clientOutdated, serverOutdated, banned, gameover(true), recentKick,
        nameInUse, idInUse, nameEmpty, customClient, serverClose, vote, typeMismatch, whitelist, playerLimit;

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

    public static class ConnectPacket implements Packet{
        public int version;
        public String versionType;
        public Array<String> mods;
        public String name, uuid, usid;
        public boolean mobile;
        public int color;

        @Override
        public void write(ByteBuffer buffer){
            buffer.putInt(Version.build);
            TypeIO.writeString(buffer, versionType);
            TypeIO.writeString(buffer, name);
            TypeIO.writeString(buffer, usid);
            buffer.put(mobile ? (byte)1 : 0);
            buffer.putInt(color);
            buffer.put(Base64Coder.decode(uuid));
            buffer.putInt(mods.size);
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
            mobile = buffer.get() == 1;
            color = buffer.getInt();
            byte[] idbytes = new byte[8];
            buffer.get(idbytes);
            uuid = new String(Base64Coder.encode(idbytes));
            int totalMods = buffer.getInt();
            mods = new Array<>(totalMods);
            for(int i = 0; i < totalMods; i++){
                mods.add(TypeIO.readString(buffer));
            }
        }
    }

    public static class InvokePacket implements Packet{
        public byte type, priority;

        public ByteBuffer writeBuffer;
        public int writeLength;

        @Override
        public void read(ByteBuffer buffer){
            type = buffer.get();
            priority = buffer.get();
            writeLength = buffer.getShort();
            byte[] bytes = new byte[writeLength];
            buffer.get(bytes);
            writeBuffer = ByteBuffer.wrap(bytes);
        }

        @Override
        public void write(ByteBuffer buffer){
            buffer.put(type);
            buffer.put(priority);
            buffer.putShort((short)writeLength);

            writeBuffer.position(0);
            for(int i = 0; i < writeLength; i++){
                buffer.put(writeBuffer.get());
            }
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
}
