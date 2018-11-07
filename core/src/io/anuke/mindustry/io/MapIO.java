package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapMeta;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.DataPosition;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.LegacyColorMapper;
import io.anuke.mindustry.world.LegacyColorMapper.LegacyBlock;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Structs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static io.anuke.mindustry.Vars.content;

/**
 * Reads and writes map files.
 */
public class MapIO{
    private static final int version = 0;
    private static IntIntMap defaultBlockMap = new IntIntMap();

    private static void loadDefaultBlocks(){
        for(Block block : content.blocks()){
            defaultBlockMap.put(block.id, block.id);
        }
    }

    public static Pixmap generatePixmap(MapTileData data){
        Pixmap pixmap = new Pixmap(data.width(), data.height(), Format.RGBA8888);
        data.position(0, 0);

        TileDataMarker marker = data.newDataMarker();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);
                byte elev = y >= data.height() - 1 ? 0 : data.read(x, y + 1, DataPosition.elevation);
                Block floor = content.block(marker.floor);
                Block wall = content.block(marker.wall);
                int color = ColorMapper.colorFor(floor, wall, Team.all[marker.team], marker.elevation + 1, elev > marker.elevation ? (byte)(1 << 6) : (byte)0);
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, color);
            }
        }

        data.position(0, 0);

        return pixmap;
    }

    /**Reads a pixmap in the old (3.5) map format.*/
    public static MapTileData readLegacyPixmap(Pixmap pixmap){
        MapTileData data = new MapTileData(pixmap.getWidth(), pixmap.getHeight());

        for(int x = 0; x < data.width(); x++){
            for(int y = 0; y < data.height(); y++){
                int color = pixmap.getPixel(x, pixmap.getHeight() - 1 - y);
                LegacyBlock block = LegacyColorMapper.get(color);

                data.write(x, y, DataPosition.floor, block.floor.id);
                data.write(x, y, DataPosition.elevation, (byte)block.elevation);

                //place spawn
                if(color == Color.rgba8888(Color.RED)){
                    data.write(x, y, DataPosition.wall, Blocks.spawn.id);
                }

                //place core
                if(color == Color.rgba8888(Color.GREEN)){
                    for(int dx = 0; dx < 3; dx++){
                        for(int dy = 0; dy < 3; dy++){
                            int worldx = dx - 1 + x;
                            int worldy = dy - 1 + y;

                            if(Structs.inBounds(worldx, worldy, pixmap.getWidth(), pixmap.getHeight())){
                                data.write(worldx, worldy, DataPosition.wall, Blocks.blockpart.id);
                                data.write(worldx, worldy, DataPosition.rotationTeam, Bits.packByte((byte)0, (byte)Team.blue.ordinal()));
                                data.write(worldx, worldy, DataPosition.link, Bits.packByte((byte) (dx - 1 + 8), (byte) (dy - 1 + 8)));
                            }
                        }
                    }

                    data.write(x, y, DataPosition.wall, StorageBlocks.core.id);
                    data.write(x, y, DataPosition.rotationTeam, Bits.packByte((byte)0, (byte)Team.blue.ordinal()));
                }
            }
        }

        return data;
    }

    public static void writeMap(OutputStream stream, ObjectMap<String, String> tags, MapTileData data) throws IOException{
        if(defaultBlockMap == null){
            loadDefaultBlocks();
        }

        MapMeta meta = new MapMeta(version, tags, data.width(), data.height(), defaultBlockMap);

        DataOutputStream ds = new DataOutputStream(stream);

        writeMapMeta(ds, meta);
        ds.write(data.toArray());

        ds.close();
    }

    /**
     * Reads tile data, skipping meta.
     */
    public static MapTileData readTileData(DataInputStream stream, boolean readOnly) throws IOException{
        MapMeta meta = readMapMeta(stream);
        return readTileData(stream, meta, readOnly);
    }


    /**
     * Does not skip meta. Call after reading meta.
     */
    public static MapTileData readTileData(DataInputStream stream, MapMeta meta, boolean readOnly) throws IOException{
        byte[] bytes = new byte[stream.available()];
        stream.readFully(bytes);
        return new MapTileData(bytes, meta.width, meta.height, meta.blockMap, readOnly);
    }

    /**
     * Reads tile data, skipping meta tags.
     */
    public static MapTileData readTileData(Map map, boolean readOnly){
        try(DataInputStream ds = new DataInputStream(map.stream.get())){
            return MapIO.readTileData(ds, readOnly);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static MapMeta readMapMeta(DataInputStream stream) throws IOException{
        ObjectMap<String, String> tags = new ObjectMap<>();
        IntIntMap map = new IntIntMap();

        int version = stream.readInt();

        byte tagAmount = stream.readByte();

        for(int i = 0; i < tagAmount; i++){
            String name = stream.readUTF();
            String value = stream.readUTF();
            tags.put(name, value);
        }

        short blocks = stream.readShort();
        for(int i = 0; i < blocks; i++){
            short id = stream.readShort();
            String name = stream.readUTF();
            Block block = content.getByName(ContentType.block, name);
            if(block == null){
                //Log.info("Map load info: No block with name {0} found.", name);
                block = Blocks.air;
            }
            map.put(id, block.id);
        }

        int width = stream.readShort();
        int height = stream.readShort();

        return new MapMeta(version, tags, width, height, map);
    }

    public static void writeMapMeta(DataOutputStream stream, MapMeta meta) throws IOException{
        stream.writeInt(meta.version);
        stream.writeByte((byte) meta.tags.size);

        for(Entry<String, String> entry : meta.tags.entries()){
            stream.writeUTF(entry.key);
            stream.writeUTF(entry.value);
        }

        stream.writeShort(content.blocks().size);
        for(Block block : content.blocks()){
            stream.writeShort(block.id);
            stream.writeUTF(block.name);
        }

        stream.writeShort(meta.width);
        stream.writeShort(meta.height);
    }
}
