package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.RemoteReadClient;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.ucore.io.ByteBufferOutput;
import io.anuke.ucore.io.IOUtils;
import io.anuke.ucore.io.ByteBufferInput;

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

            if(Net.client()){
                RemoteReadClient.readPacket(buffer, type);
            }else{
                byte[] bytes = new byte[writeLength];
                buffer.get(bytes);
                writeBuffer = ByteBuffer.wrap(bytes);
            }
        }

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(type);
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
        public Player player;

        @Override
        public void write(ByteBuffer buffer) {
            ByteBufferOutput out = new ByteBufferOutput(buffer);

            buffer.putInt(player.id);
            buffer.putLong(TimeUtils.millis());
            player.write(out);
        }

        @Override
        public void read(ByteBuffer buffer) {
            ByteBufferInput in = new ByteBufferInput(buffer);

            int id = buffer.getInt();
            long time = buffer.getLong();
            player = Vars.playerGroup.getByID(id);
            player.read(in, time);
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
