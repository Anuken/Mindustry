package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;

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
            buffer.put((byte)name.getBytes().length);
            buffer.put(name.getBytes());
            buffer.put(android ? (byte)1 : 0);
            buffer.putInt(color);
            buffer.put(uuid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            version = buffer.getInt();
            byte length = buffer.get();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            name = new String(bytes);
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
        public byte[] data;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(data);
        }

        @Override
        public void read(ByteBuffer buffer) {
            data = new byte[SyncEntity.getWriteSize(Player.class) + 8];
            buffer.get(data);
        }
    }

    //not a real packet.
    public static class EffectPacket{
        public int id;
        public float x, y, rotation;
        public int color;
    }

    public static class ShootPacket implements Packet{
        public byte weaponid;
        public float x, y, rotation;
        public int playerid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(weaponid);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(rotation);
            buffer.putInt(playerid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            weaponid = buffer.get();
            x = buffer.getFloat();
            y = buffer.getFloat();
            rotation = buffer.getFloat();
            playerid = buffer.getInt();
        }
    }

    public static class BulletPacket implements Packet{
        public int type, owner;
        public float x, y, angle;
        public short damage;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putShort((short)type);
            buffer.putInt(owner);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(angle);
            buffer.putShort(damage);
        }

        @Override
        public void read(ByteBuffer buffer) {
            type = buffer.getShort();
            owner = buffer.getInt();
            x = buffer.getFloat();
            y = buffer.getFloat();
            angle = buffer.getFloat();
            damage = buffer.getShort();
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

    public static class EnemyDeathPacket implements Packet{
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
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
            buffer.putShort((short)text.getBytes().length);
            buffer.put(text.getBytes());
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
            short tlength = buffer.getShort();
            byte[] t = new byte[tlength];
            buffer.get(t);

            id = buffer.getInt();
            text = new String(t);
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
        public byte left, right;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(playerid);
            buffer.put(left);
            buffer.put(right);
        }

        @Override
        public void read(ByteBuffer buffer) {
            playerid = buffer.getInt();
            left = buffer.get();
            right = buffer.get();
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

    public static class PlayerDeathPacket implements Packet{
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
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

    public static class ItemTransferPacket implements Packet, UnimportantPacket{
        public int position;
        public byte rotation;
        public byte itemid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt((rotation) | (position << 2));
            buffer.put(itemid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            int i = buffer.getInt();
            rotation = (byte)(i & 0x3);
            position = i >> 2;
            itemid = buffer.get();
        }
    }

    public static class ItemSetPacket implements Packet, UnimportantPacket{
        public int position;
        public byte itemid, amount;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(position);
            buffer.put(itemid);
            buffer.put(amount);
        }

        @Override
        public void read(ByteBuffer buffer) {
            position = buffer.getInt();
            itemid = buffer.get();
            amount = buffer.get();
        }
    }

    public static class ItemOffloadPacket implements Packet{
        public int position;
        public byte itemid;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(position);
            buffer.put(itemid);
        }

        @Override
        public void read(ByteBuffer buffer) {
            position = buffer.getInt();
            itemid = buffer.get();
        }
    }

    public static class NetErrorPacket implements Packet{
        public String message;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putShort((short)message.getBytes().length);
            buffer.put(message.getBytes());
        }

        @Override
        public void read(ByteBuffer buffer) {
            short length = buffer.getShort();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            message = new String(bytes);
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
