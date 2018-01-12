package io.anuke.mindustry.net;

/**Class for storing all packets.*/
public class Packets {

    public static class Connect {
        public int id;
        public String addressTCP;
    }

    public static class Disconnect {
        public int id;
        public String addressTCP;
    }

    public static class WorldData extends Streamable{

    }

    public static class SyncPacket{
        public int[] ids;
        public float[][] data;
        public short enemyStart;
    }

    public static class BlockSyncPacket extends Streamable{

    }

    public static class ConnectPacket{
        public String name;
        public boolean android;
    }

    public static class ConnectConfirmPacket{

    }

    public static class DisconnectPacket{
        public int playerid;
    }

    public static class StateSyncPacket{
        public int[] items;
        public float countdown, time;
        public int enemies, wave;
        public long timestamp;
    }

    public static class PositionPacket{
        public float[] data;
    }

    public static class EffectPacket{
        public int id;
        public float x, y, rotation;
        public int color;
    }

    public static class ShootPacket{
        public byte weaponid;
        public float x, y, rotation;
        public int playerid;
    }

    public static class BulletPacket{
        public int type, owner;
        public float x, y, angle;
        public short damage;
    }

    public static class PlacePacket{
        public int playerid;
        public byte rotation;
        public short x, y;
        public int block;
    }

    public static class BreakPacket{
        public int playerid;
        public short x, y;
    }

    public static class EnemySpawnPacket{
        public byte type, lane, tier;
        public float x, y;
        public short health;
        public int id;
    }

    public static class EnemyDeathPacket{
        public int id;
    }

    public static class BlockDestroyPacket{
        public int position;
    }

    public static class BlockUpdatePacket{
        public int health, position;
    }

    public static class ChatPacket{
        public String name;
        public String text;
        public int id;
    }

    public static class KickPacket{
        public byte reason;
    }

    public enum KickReason{
        kick, invalidPassword
    }

    public static class UpgradePacket{
        public byte id; //weapon ID only, currently
    }

    public static class WeaponSwitchPacket{
        public int playerid;
        public byte left, right;
    }

    public static class BlockTapPacket{
        public int position;
    }

    public static class BlockConfigPacket{
        public int position;
        public byte data;
    }

    public static class EntityRequestPacket{
        public int id;
    }
}
