package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.ucore.io.ByteBufferInput;
import io.anuke.ucore.io.ByteBufferOutput;
import io.anuke.ucore.io.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**Class for storing all packets.*/
public class Packets {

    public static class Connect implements ImportantPacket{
        public int id;
        public String addressTCP;
    }

    public static class Disconnect implements ImportantPacket{
        public int id;
    }

    public static class WorldStream extends Streamable{

    }

    public static class ConnectPacket implements Packet{
        public int version;
        public int players;
        public String name;
        public boolean mobile;
        public int color;
        public byte[] uuid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(Version.build);
            IOUtils.writeString(buffer, name);
            buffer.put(mobile ? (byte)1 : 0);
            buffer.putInt(color);
            buffer.put(uuid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            version = buffer.getInt();
            name = IOUtils.readString(buffer);
            mobile = buffer.get() == 1;
            color = buffer.getInt();
            uuid = new byte[8];
            buffer.get(uuid);
        }
    }

    public static class InvokePacket implements Packet{
        public byte type;

        public ByteBuffer writeBuffer;
        public int writeLength;

        @Override
        public void read(ByteBuffer buffer) {
            type = buffer.get();
            writeLength = buffer.getShort();
            byte[] bytes = new byte[writeLength];
            buffer.get(bytes);
            writeBuffer = ByteBuffer.wrap(bytes);
        }

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(type);
            buffer.putShort((short)writeLength);

            writeBuffer.position(0);
            for(int i = 0; i < writeLength; i ++){
                buffer.put(writeBuffer.get());
            }
        }
    }

    public static class SnapshotPacket implements Packet, UnimportantPacket{
        @Override
        public void read(ByteBuffer buffer) {

        }

        @Override
        public void write(ByteBuffer buffer) {

        }
    }

    public static class ClientSnapshotPacket implements Packet{
        /**For writing only.*/
        public Player player;

        public int lastSnapshot;
        public int snapid;
        public int length;
        public long timeSent;

        public byte[] bytes;
        public ByteBuffer result;
        public ByteBufferInput in;

        @Override
        public void write(ByteBuffer buffer) {
            ByteBufferOutput out = new ByteBufferOutput(buffer);

            buffer.putInt(lastSnapshot);
            buffer.putInt(snapid);
            buffer.putLong(TimeUtils.millis());

            int position = buffer.position();
            try {
                player.write(out);
                length = buffer.position() - position;
                buffer.position(position);
                buffer.putInt(length);
                player.write(out);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void read(ByteBuffer buffer) {

            lastSnapshot = buffer.getInt();
            snapid = buffer.getInt();
            timeSent = buffer.getLong();
            length = buffer.getInt();

            if(bytes == null || bytes.length != length){
                bytes = new byte[length];
                result = ByteBuffer.wrap(bytes);
                in = new ByteBufferInput(result);
            }

            buffer.get(bytes);
            result.position(0);
        }
    }

    public enum KickReason{
        kick, invalidPassword, clientOutdated, serverOutdated, banned, gameover(true), recentKick, nameInUse, idInUse, fastShoot;
        public final boolean quiet;

        KickReason(){ quiet = false; }

        KickReason(boolean quiet){
            this.quiet = quiet;
        }
    }

    public enum AdminAction{
        kick, ban, trace
    }
}
