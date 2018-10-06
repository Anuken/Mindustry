package io.anuke.mindustry.io;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapMeta;
import io.anuke.mindustry.maps.MapTileData;
import io.anuke.mindustry.maps.MapTileData.DataPosition;
import io.anuke.mindustry.maps.MapTileData.TileDataMarker;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;

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
        Color color = new Color();

        for(int y = 0; y < data.height(); y++){
            for(int x = 0; x < data.width(); x++){
                data.read(marker);
                Block floor = content.block(marker.floor);
                Block wall = content.block(marker.wall);
                int wallc = ColorMapper.getBlockColor(wall);
                if(wallc == 0 && (wall.update || wall.solid || wall.breakable)) wallc = Team.all[marker.team].intColor;
                wallc = wallc == 0 ? ColorMapper.getBlockColor(floor) : wallc;
                if(marker.elevation > 0){
                    float scaling = 1f + marker.elevation / 8f;
                    color.set(wallc);
                    color.mul(scaling, scaling, scaling, 1f);
                    wallc = Color.rgba8888(color);
                }
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, wallc);
            }
        }

        data.position(0, 0);

        return pixmap;
    }

    public static MapTileData readPixmap(Pixmap pixmap){
        MapTileData data = new MapTileData(pixmap.getWidth(), pixmap.getHeight());

        for(int x = 0; x < data.width(); x++){
            for(int y = 0; y < data.height(); y++){
                Block block = ColorMapper.getByColor(pixmap.getPixel(y, pixmap.getWidth() - 1 - x));

                if(block == null){
                    data.write(x, y, DataPosition.floor, Blocks.stone.id);
                }else{
                    data.write(x, y, DataPosition.floor, block.id);
                }

                data.write(x, y, DataPosition.wall, Blocks.air.id);
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
