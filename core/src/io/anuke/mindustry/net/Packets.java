package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.ucore.io.IOUtils;

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
        //snapshot meta
        public int lastSnapshot;
        public int snapid;
        public long timeSent;
        //player snapshot data
        public float x, y, rotation, baseRotation;

        @Override
        public void write(ByteBuffer buffer) {
            Player player = Vars.players[0];

            buffer.putInt(lastSnapshot);
            buffer.putInt(snapid);
            buffer.putLong(TimeUtils.millis());

            buffer.putFloat(player.x);
            buffer.putFloat(player.y);
            //saving 4 bytes, yay?
            buffer.putShort((short)(player.rotation*2));
            buffer.putShort((short)(player.baseRotation*2));
        }

        @Override
        public void read(ByteBuffer buffer) {
            lastSnapshot = buffer.getInt();
            snapid = buffer.getInt();
            timeSent = buffer.getLong();

            x = buffer.getFloat();
            y = buffer.getFloat();
            rotation = buffer.getShort()/2f;
            baseRotation = buffer.getShort()/2f;
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
