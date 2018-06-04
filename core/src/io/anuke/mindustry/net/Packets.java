package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;

import java.nio.ByteBuffer;

/**Class for storing all packets.*/
public class Packets {

    public static class Connect implements ImportantPacket{
        public int id;
        public String addressTCP;
    }

    public static class Disconnect implements ImportantPacket{
        public int id;
        public String addressTCP;
    }

    public static class WorldStream extends Streamable{

    }

    public static class InvokePacket implements Packet{
        public byte type;

        public ByteBuffer writeBuffer;
        public int writeLength;

        @Override
        public void read(ByteBuffer buffer) {
            type = buffer.get();

            if(Net.client()){
                //TODO implement
                //CallClient.readPacket(buffer, type);
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
            buffer.putInt(player.id);
            buffer.putLong(TimeUtils.millis());
            player.write(buffer);
        }

        @Override
        public void read(ByteBuffer buffer) {
            int id = buffer.getInt();
            long time = buffer.getLong();
            player = Vars.playerGroup.getByID(id);
            player.read(buffer, time);
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
