package io.anuke.mindustry.io;

import io.anuke.ucore.util.Bits;

import java.nio.ByteBuffer;

public class MapTileData {
    /**Tile size: 3 bytes.
     * 0: ground tile
     * 1: wall tile
     * 2: rotation + team*/
    private final static int TILE_SIZE = 3;

    private final ByteBuffer buffer;
    private final TileDataWriter tile = new TileDataWriter();
    private final int width, height;

    public MapTileData(int width, int height){
        this.width = width;
        this.height = height;
        buffer = ByteBuffer.allocate(width * height * TILE_SIZE);
    }

    public MapTileData(byte[] bytes, int width, int height){
        buffer = ByteBuffer.wrap(bytes);
        this.width = width;
        this.height = height;
    }

    public int width(){
        return width;
    }

    public int height(){
        return height;
    }

    /**Reads and returns the next tile data.*/
    public TileDataWriter read(){
        tile.read(buffer);
        return tile;
    }

    /**Reads and returns the next tile data.*/
    public TileDataWriter readAt(int x, int y){
        position(x, y);
        tile.read(buffer);
        return tile;
    }

    /**Writes tile data at a specified position.*/
    public void write(int x, int y, TileDataWriter writer){
        position(x, y);
        writer.write(buffer);
    }

    /**Sets read position to the specified coordinates*/
    public void position(int x, int y){
        buffer.position((x + width * y) * TILE_SIZE);
    }

    public static class TileDataWriter {
        public byte floor, wall;
        public byte rotation;
        public byte team;

        public void read(ByteBuffer buffer){
            floor = buffer.get();
            wall = buffer.get();
            byte rt = buffer.get();
            rotation = Bits.getLeftByte(rt);
            team = Bits.getRightByte(rt);
        }

        public void write(ByteBuffer buffer){
            buffer.put(floor);
            buffer.put(wall);
            byte rt = Bits.packByte(rotation, team);
            buffer.put(rt);
        }
    }
}
