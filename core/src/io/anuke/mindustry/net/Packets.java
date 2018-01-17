package io.anuke.mindustry.net;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.resource.Item;

import java.nio.ByteBuffer;

/**Class for storing all packets.*/
public class Packets {

    public static class Connect{
        public int id;
        public String addressTCP;
    }

    public static class Disconnect {
        public int id;
        public String addressTCP;
    }

    public static class WorldData extends Streamable{

    }

    public static class SyncPacket implements Packet{
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
        public String name;
        public boolean android;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put((byte)name.length());
            buffer.put(name.getBytes());
            buffer.put(android ? (byte)1 : 0);
        }

        @Override
        public void read(ByteBuffer buffer) {
            byte length = buffer.get();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            name = new String(bytes);
            android = buffer.get() == 1;
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

    public static class StateSyncPacket implements Packet{
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
            data = new byte[SyncEntity.getWriteSize(Player.class)];
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

    public static class EnemySpawnPacket implements Packet{
        public byte type, lane, tier;
        public float x, y;
        public short health;
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put(type);
            buffer.put(lane);
            buffer.put(tier);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putShort(health);
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            type = buffer.get();
            lane = buffer.get();
            tier = buffer.get();
            x = buffer.getFloat();
            y = buffer.getFloat();
            health = buffer.getShort();
            id = buffer.getInt();
        }
    }

    public static class PlayerSpawnPacket implements Packet{
        public byte weaponleft, weaponright;
        public boolean android;
        public String name;
        public float x, y;
        public int id;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put((byte)name.length());
            buffer.put(name.getBytes());
            buffer.put(weaponleft);
            buffer.put(weaponright);
            buffer.put(android ? 1 : (byte)0);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            byte nlength = buffer.get();
            byte[] n = new byte[nlength];
            buffer.get(n);
            name = new String(n);
            weaponleft = buffer.get();
            weaponright = buffer.get();
            android = buffer.get() == 1;
            x = buffer.getFloat();
            y = buffer.getFloat();
            id = buffer.getInt();
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
                buffer.putShort((short) name.length());
                buffer.put(name.getBytes());
            }else{
                buffer.putShort((short)-1);
            }
            buffer.putShort((short)text.length());
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

    public static class KickPacket implements Packet{
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
        kick, invalidPassword
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

        @Override
        public void write(ByteBuffer buffer) {
            buffer.putInt(id);
        }

        @Override
        public void read(ByteBuffer buffer) {
            id = buffer.getInt();
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
}
