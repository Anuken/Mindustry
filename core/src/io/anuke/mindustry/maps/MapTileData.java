package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.IntIntMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Structs;

import java.nio.ByteBuffer;

public class MapTileData{
    /**
     * Tile size: 4 bytes. <br>
     * 0: ground tile <br>
     * 1: wall tile <br>
     * 2: rotation + team <br>
     * 3: link (x/y) <br>
     * 4: elevation <br>
     */
    private final static int TILE_SIZE = 5;

    private final ByteBuffer buffer;
    private final int width, height;
    private final boolean readOnly;

    private IntIntMap map;

    public MapTileData(int width, int height){
        this.width = width;
        this.height = height;
        this.map = null;
        this.readOnly = false;
        buffer = ByteBuffer.allocate(width * height * TILE_SIZE);
    }

    public MapTileData(byte[] bytes, int width, int height, IntIntMap mapping, boolean readOnly){
        buffer = ByteBuffer.wrap(bytes);
        this.width = width;
        this.height = height;
        this.map = mapping;
        this.readOnly = readOnly;

        if(mapping != null && !readOnly){
            buffer.position(0);
            TileDataMarker marker = new TileDataMarker();
            for(int i = 0; i < width * height; i++){
                read(marker);

                //strip blockparts from map data, as they can be invalid
                if(marker.wall == Blocks.blockpart.id){
                    marker.wall = Blocks.air.id;
                }

                buffer.position(i * TILE_SIZE);

                //write mapped marker
                write(marker);
            }

            buffer.position(0);
            for(int x = 0; x < width; x ++){
                for(int y = 0; y < height; y ++){
                    //add missing blockparts
                    Block drawBlock = Vars.content.block(read(x, y, DataPosition.wall));
                    if(drawBlock.isMultiblock()){
                        int offsetx = -(drawBlock.size - 1) / 2;
                        int offsety = -(drawBlock.size - 1) / 2;
                        for(int dx = 0; dx < drawBlock.size; dx++){
                            for(int dy = 0; dy < drawBlock.size; dy++){
                                int worldx = dx + offsetx + x;
                                int worldy = dy + offsety + y;

                                if(Structs.inBounds(worldx, worldy, width, height) && !(dx + offsetx == 0 && dy + offsety == 0)){
                                    write(worldx, worldy, DataPosition.wall, Blocks.blockpart.id);
                                    write(worldx, worldy, DataPosition.link, Bits.packByte((byte) (dx + offsetx + 8), (byte) (dy + offsety + 8)));
                                }
                            }
                        }
                    }
                }
            }
            buffer.position(0);
            this.map = null;
        }
    }

    public byte[] toArray(){
        return buffer.array();
    }

    public int width(){
        return width;
    }

    public int height(){
        return height;
    }

    /**
     * Write a byte to a specific position.
     */
    public void write(int x, int y, DataPosition position, byte data){
        buffer.put((x + width * y) * TILE_SIZE + position.ordinal(), data);
    }

    /**
     * Gets a byte at a specific position.
     */
    public byte read(int x, int y, DataPosition position){
        return buffer.get((x + width * y) * TILE_SIZE + position.ordinal());
    }

    /**
     * Reads and returns the next tile data.
     */
    public TileDataMarker read(TileDataMarker marker){
        marker.read(buffer);
        return marker;
    }

    /**
     * Writes this tile data marker.
     */
    public void write(TileDataMarker marker){
        marker.write(buffer);
    }

    /**
     * Sets read position to the specified coordinates
     */
    public void position(int x, int y){
        buffer.position((x + width * y) * TILE_SIZE);
    }

    public TileDataMarker newDataMarker(){
        return new TileDataMarker();
    }

    public enum DataPosition{
        floor, wall, link, rotationTeam, elevation
    }

    public class TileDataMarker{
        public byte floor, wall;
        public byte link;
        public byte rotation;
        public byte team;
        public byte elevation;

        public void read(ByteBuffer buffer){
            floor = buffer.get();
            wall = buffer.get();
            link = buffer.get();
            byte rt = buffer.get();
            elevation = buffer.get();
            rotation = Bits.getLeftByte(rt);
            team = Bits.getRightByte(rt);

            if(map != null){
                floor = (byte) map.get(floor, Blocks.stone.id);
                wall = (byte) map.get(wall, 0);
            }
        }

        public void write(ByteBuffer buffer){
            if(readOnly) throw new IllegalArgumentException("This data is read-only.");
            buffer.put(floor);
            buffer.put(wall);
            buffer.put(link);
            buffer.put(Bits.packByte(rotation, team));
            buffer.put(elevation);
        }
    }
}
