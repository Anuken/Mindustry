package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.util.IOUtils;

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

    public static class WorldData extends Streamable{

    }

    public static class SyncPacket implements Packet, UnimportantPacket{
        public byte[] data;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putShort((short)data.length);
            buffer.put(data);
        }

        @Override
        public void read(ByteBuffer buffer) {
            data = new byte[buffer.getShort()];
            buffer.get(data);
        }
    }

    public static class BlockSyncPacket extends Streamable{

    }

    public static class ConnectPacket implements Packet{
        public int version;
        public String name;
        public boolean android;
        public int color;
        public byte[] uuid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(Version.build);
            IOUtils.writeString(buffer, name);
            buffer.put(android ? (byte)1 : 0);
            buffer.putInt(color);
            buffer.put(uuid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            version = buffer.getInt();
            name = IOUtils.readString(buffer);
            android = buffer.get() == 1;
            color = buffer.getInt();
            uuid = new byte[8];
            buffer.get(uuid);
        }
    }

    public static class ConnectConfirmPacket implements Packet{
        @Override
        public void write(ByteBuffer buffer) { }

        @Override
        public void read(ByteBuffer buffer) { }
    }

    public static class DisconnectPacket implements Packet{
        public int playerid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(playerid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            playerid = buffer.getInt();
        }
    }

    public static class StateSyncPacket implements Packet, UnimportantPacket{
        public int[] items;
        public float countdown, time;
        public int enemies, wave;
        public long timestamp;

        @Override
        public void write(ByteBuffer buffer) {
            for(int i = 0; i < items.length; i ++){
                buffer.putInt(items[i]);
            }

            buffer.putFloat(countdown);
            buffer.putFloat(time);
            buffer.putShort((short)enemies);
            buffer.putShort((short)wave);
            buffer.putLong(timestamp);
        }

        @Override
        public void read(ByteBuffer buffer) {
            items = new int[Item.getAllItems().size];

            for(int i = 0; i < items.length; i ++){
                items[i] = buffer.getInt();
            }

            countdown = buffer.getFloat();
            time = buffer.getFloat();
            enemies = buffer.getShort();
            wave = buffer.getShort();
            timestamp = buffer.getLong();
        }
    }

    public static class PositionPacket implements Packet{
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

    public static class EntityShootPacket implements Packet{
        public float x, y, rotation;
        public short bulletid;
        public byte groupid;
        public short data;
        public int entityid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(groupid);
            buffer.putInt(entityid);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(rotation);
            buffer.putShort(bulletid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            groupid = buffer.get();
            entityid = buffer.getInt();
            x = buffer.getFloat();
            y = buffer.getFloat();
            rotation = buffer.getFloat();
            bulletid = buffer.getShort();
        }
    }

    public static class PlacePacket implements Packet{
        public int playerid;
        public byte rotation;
        public short x, y;
        public int block;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(playerid);
            buffer.put(rotation);
            buffer.putShort(x);
            buffer.putShort(y);
            buffer.putInt(block);
        }

        @Override
        public void read(ByteBuffer buffer) {
            playerid = buffer.getInt();
            rotation = buffer.get();
            x = buffer.getShort();
            y = buffer.getShort();
            block = buffer.getInt();
        }
    }

    public static class BreakPacket implements Packet{
        public int playerid;
        public short x, y;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(playerid);
            buffer.putShort(x);
            buffer.putShort(y);
        }

        @Override
        public void read(ByteBuffer buffer) {
            playerid = buffer.getInt();
            x = buffer.getShort();
            y = buffer.getShort();
        }
    }

    public static class EntitySpawnPacket implements Packet{
        public SyncEntity entity;
        public EntityGroup<?> group;

        @Override
        public void write(ByteBuffer buffer){
            buffer.put((byte)group.getID());
            buffer.putInt(entity.id);
            entity.writeSpawn(buffer);
        }

        @Override
        public void read(ByteBuffer buffer) {
            byte groupid = buffer.get();
            int id = buffer.getInt();
            group = Entities.getGroup(groupid);
            try {
                entity = (SyncEntity) ClassReflection.newInstance(group.getType());
                entity.id = id;
                entity.readSpawn(buffer);
                entity.setNet(entity.x, entity.y);
            }catch (ReflectionException e){
                throw new RuntimeException(e);
            }
        }
    }

    public static class EntityDeathPacket implements Packet{
        public byte group;
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(group);
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            group = buffer.get();
            id = buffer.getInt();
        }
    }

    public static class BlockDestroyPacket implements Packet{
        public int position;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(position);
        }

        @Override
        public void read(ByteBuffer buffer) {
            position = buffer.getInt();
        }
    }

    public static class BlockUpdatePacket implements Packet{
        public int health, position;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putShort((short)health);
            buffer.putInt(position);
        }

        @Override
        public void read(ByteBuffer buffer) {
            health = buffer.getShort();
            position = buffer.getInt();
        }
    }

    public static class ChatPacket implements Packet{
        public String name;
        public String text;
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            if(name != null) {
                buffer.putShort((short) name.getBytes().length);
                buffer.put(name.getBytes());
            }else{
                buffer.putShort((short)-1);
            }
            IOUtils.writeString(buffer, text);
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            short nlength = buffer.getShort();
            if(nlength != -1) {
                byte[] n = new byte[nlength];
                buffer.get(n);
                name = new String(n);
            }

            text = IOUtils.readString(buffer);
            id = buffer.getInt();
        }
    }

    public static class KickPacket implements Packet, ImportantPacket{
        public KickReason reason;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put((byte)reason.ordinal());
        }

        @Override
        public void read(ByteBuffer buffer) {
            reason = KickReason.values()[buffer.get()];
        }
    }

    public enum KickReason{
        kick, invalidPassword, clientOutdated, serverOutdated, banned, gameover(true), recentKick;
        public final boolean quiet;

        KickReason(){ quiet = false; }

        KickReason(boolean quiet){
            this.quiet = quiet;
        }
    }

    public static class UpgradePacket implements Packet{
        public byte id; //weapon ID only, currently

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.get();
        }
    }

    public static class WeaponSwitchPacket implements Packet{
        public int playerid;
        public byte weapon;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(playerid);
            buffer.put(weapon);
        }

        @Override
        public void read(ByteBuffer buffer) {
            playerid = buffer.getInt();
            weapon = buffer.get();
        }
    }

    public static class BlockTapPacket implements Packet{
        public int position;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(position);
        }

        @Override
        public void read(ByteBuffer buffer) {
            position = buffer.getInt();
        }
    }

    public static class BlockConfigPacket implements Packet{
        public int position;
        public byte data;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(position);
            buffer.put(data);
        }

        @Override
        public void read(ByteBuffer buffer) {
            position = buffer.getInt();
            data = buffer.get();
        }
    }

    public static class EntityRequestPacket implements Packet{
        public int id;
        public byte group;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
            buffer.put(group);
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
            group = buffer.get();
        }
    }

    public static class GameOverPacket implements Packet{
        @Override
        public void write(ByteBuffer buffer) { }

        @Override
        public void read(ByteBuffer buffer) { }
    }

    public static class FriendlyFireChangePacket implements Packet{
        public boolean enabled;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(enabled ? 1 : (byte)0);
        }

        @Override
        public void read(ByteBuffer buffer) {
            enabled = buffer.get() == 1;
        }
    }

    public static class CustomMapPacket extends Streamable{

    }

    public static class MapAckPacket implements Packet{
        @Override
        public void write(ByteBuffer buffer) { }

        @Override
        public void read(ByteBuffer buffer) { }
    }

    public static class NetErrorPacket implements Packet{
        public String message;

        @Override
        public void write(ByteBuffer buffer) {
            IOUtils.writeString(buffer, message);
        }

        @Override
        public void read(ByteBuffer buffer) {
            message = IOUtils.readString(buffer);
        }
    }

    public static class PlayerAdminPacket implements Packet{
        public boolean admin;
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(admin ? (byte)1 : 0);
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            admin = buffer.get() == 1;
            id = buffer.getInt();
        }
    }

    public static class AdministerRequestPacket implements Packet{
        public AdminAction action;
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put((byte)action.ordinal());
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            action = AdminAction.values()[buffer.get()];
            id = buffer.getInt();
        }
    }

    public enum AdminAction{
        kick, ban, trace
    }

    public static class TracePacket implements Packet{
        public TraceInfo info;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(info.playerid);
            buffer.putShort((short)info.ip.getBytes().length);
            buffer.put(info.ip.getBytes());
            buffer.put(info.modclient ? (byte)1 : 0);
            buffer.put(info.android ? (byte)1 : 0);

            buffer.putInt(info.totalBlocksBroken);
            buffer.putInt(info.structureBlocksBroken);
            buffer.putInt(info.lastBlockBroken.id);

            buffer.putInt(info.totalBlocksPlaced);
            buffer.putInt(info.lastBlockPlaced.id);
            buffer.put(Base64Coder.decode(info.uuid));
        }

        @Override
        public void read(ByteBuffer buffer) {
            int id = buffer.getInt();
            short iplen = buffer.getShort();
            byte[] ipb = new byte[iplen];
            buffer.get(ipb);

            info = new TraceInfo(new String(ipb));

            info.playerid = id;
            info.modclient = buffer.get() == 1;
            info.android = buffer.get() == 1;
            info.totalBlocksBroken = buffer.getInt();
            info.structureBlocksBroken = buffer.getInt();
            info.lastBlockBroken = Block.getByID(buffer.getInt());
            info.totalBlocksPlaced = buffer.getInt();
            info.lastBlockPlaced = Block.getByID(buffer.getInt());
            byte[] uuid = new byte[8];
            buffer.get(uuid);

            info.uuid = new String(Base64Coder.encode(uuid));
        }
    }
}
