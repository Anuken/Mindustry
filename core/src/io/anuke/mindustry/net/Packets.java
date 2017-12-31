package io.anuke.mindustry.net;

import io.anuke.mindustry.entities.Player;

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

    public static class EntityDataPacket{
        public Player[] players;
        public int playerid;
    }

    public static class SyncPacket{
        public int[] ids;
        public float[][] data;
    }

    public static class BlockSyncPacket extends Streamable{

    }

    public static class StateSyncPacket {
        public int[] items;
        public float countdown;
        public int enemies, wave;
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
}
