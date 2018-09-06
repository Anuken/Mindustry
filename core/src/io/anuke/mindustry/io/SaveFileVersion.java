package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.type.ContentType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.content;

public abstract class SaveFileVersion{
    public final int version;

    public SaveFileVersion(int version){
        this.version = version;
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong();
        long playtime = stream.readLong();
        int build = stream.readInt();
        int sector = stream.readInt();
        byte mode = stream.readByte();
        String map = stream.readUTF();
        int wave = stream.readInt();
        byte difficulty = stream.readByte();
        return new SaveMeta(version, time, playtime, build, sector, mode, map, wave, Difficulty.values()[difficulty]);
    }

    public IntMap<IntMap<MappableContent>> readContentHeader(DataInputStream stream) throws IOException{
        IntMap<IntMap<MappableContent>> map = new IntMap<>();

        byte mapped = stream.readByte();
        for (int i = 0; i < mapped; i++) {
            ContentType type = ContentType.values()[stream.readByte()];
            map.put(type.ordinal(), new IntMap<>());
            short total = stream.readShort();
            for (int j = 0; j < total; j++) {
                int id = stream.readUnsignedByte();
                String name = stream.readUTF();
                if(content.getContentMap()[type.ordinal()].size == 0) continue;
                map.get(type.ordinal()).put(id, content.getByName(type, name));
            }
        }

        return map;
    }

    public void writeContentHeader(DataOutputStream stream) throws IOException{
        Array<Content>[] map = content.getContentMap();

        int mappable = 0;
        for(int i =0; i < map.length; i ++){
            Array<Content> arr = map[i];
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                mappable ++;
            }
        }

        stream.writeByte(mappable);
        for(int i =0; i < map.length; i ++){
            Array<Content> arr = map[i];
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                stream.writeByte(arr.first().getContentType().ordinal());
                stream.writeShort(arr.size);
                for(Content c : arr){
                    MappableContent m = (MappableContent)c;
                    if(m.id > 255) throw new RuntimeException("Content " + c + " has ID > 255!");
                    stream.writeByte(m.id);
                    stream.writeUTF(m.getContentName());
                }
            }
        }
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
