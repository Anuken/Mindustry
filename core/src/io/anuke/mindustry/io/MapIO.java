package io.anuke.mindustry.io;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.anuke.mindustry.Vars.customMapDirectory;
import static io.anuke.mindustry.Vars.mapExtension;

/**Reads and writes map files.*/
//TODO GWT support
public class MapIO {
    private static final int version = 0;

    public static Pixmap generatePixmap(MapTileData data){
        Pixmap pixmap = new Pixmap(data.width(), data.height(), Format.RGBA8888);

        for(int x = 0; x < data.width(); x ++){
            for(int y = 0; y < data.height(); y ++){
                TileDataMarker marker = data.read();
                Block floor = Block.getByID(marker.floor);
                Block wall = Block.getByID(marker.wall);
                int wallc = ColorMapper.getColor(wall);
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, wallc == 0 ? ColorMapper.getColor(floor) : wallc);
            }
        }

        return pixmap;
    }

    public static void writeMap(FileHandle file, ObjectMap<String, String> tags, MapTileData data) throws IOException{
        MapMeta meta = new MapMeta(version, tags, data.width(), data.height());

        DataOutputStream ds = new DataOutputStream(file.write(false));

        writeMapMeta(ds, meta);
        ds.write(data.toArray());

        ds.close();
    }

    /**Reads tile data, skipping meta.*/
    public static MapTileData readTileData(DataInputStream stream) throws IOException {
        MapMeta meta = readMapMeta(stream);
        byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        return new MapTileData(bytes, meta.width, meta.height);
    }

    /**Does not skip meta. Call after reading meta.*/
    public static MapTileData readTileData(DataInputStream stream, MapMeta meta) throws IOException {
        byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        return new MapTileData(bytes, meta.width, meta.height);
    }

    /**Reads tile data, skipping meta tags.*/
    public static MapTileData readTileData(Map map){
        try {
            InputStream stream;

            if (!map.custom) {
                stream = Gdx.files.local("maps/" + map.name + "." + mapExtension).read();
            } else {
                stream = customMapDirectory.child(map.name + "." + mapExtension).read();
            }

            DataInputStream ds = new DataInputStream(stream);
            MapTileData data = MapIO.readTileData(ds);
            ds.close();
            return data;
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static MapMeta readMapMeta(DataInputStream stream) throws IOException{
        ObjectMap<String, String> tags = new ObjectMap<>();

        int version = stream.readInt();

        byte tagAmount = stream.readByte();

        for(int i = 0; i < tagAmount; i ++){
            String name = stream.readUTF();
            String value = stream.readUTF();
            tags.put(name, value);
        }

        int width = stream.readShort();
        int height = stream.readShort();

        return new MapMeta(version, tags, width, height);
    }

    public static void writeMapMeta(DataOutputStream stream, MapMeta meta) throws IOException{
        stream.writeInt(meta.version);
        stream.writeByte((byte)meta.tags.size);

        for(Entry<String, String> entry : meta.tags.entries()){
            stream.writeUTF(entry.key);
            stream.writeUTF(entry.value);
        }

        stream.writeShort(meta.width);
        stream.writeShort(meta.height);
    }
}
