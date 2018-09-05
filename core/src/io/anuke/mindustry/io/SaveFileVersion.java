package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.MappableContent;
import io.anuke.mindustry.type.ContentType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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

    public ObjectMap<ContentType, IntMap<MappableContent>> readContentHeader(DataInputStream stream) throws IOException{
        ObjectMap<ContentType, IntMap<MappableContent>> map = new ObjectMap<>();

        byte mapped = stream.readByte();
        for (int i = 0; i < mapped; i++) {
            ContentType type = ContentType.values()[stream.readByte()];
            map.put(type, new IntMap<>());
            short total = stream.readShort();
            for (int j = 0; j < total; j++) {
                byte id = stream.readByte();
                String name = stream.readUTF();
                if(ContentLoader.getContentMap().get(type).size == 0) continue;
                map.get(type).put(id, ContentLoader.getByName(type, name));
            }
        }

        return map;
    }

    public void writeContentHeader(DataOutputStream stream) throws IOException{
        ObjectMap<ContentType, Array<Content>> map = ContentLoader.getContentMap();

        int mappable = 0;
        for(Entry<ContentType, Array<Content>> entry : map.entries()){
            if(entry.value.size > 0 && entry.value.first() instanceof MappableContent){
                mappable ++;
            }
        }

        stream.writeByte(mappable);
        for(Entry<ContentType, Array<Content>> entry : map.entries()){
            if(entry.value.size > 0 && entry.value.first() instanceof MappableContent){
                stream.writeByte(entry.value.first().getContentType().ordinal());
                stream.writeShort(entry.value.size);
                for(Content c : entry.value){
                    MappableContent m = (MappableContent)c;
                    if(m.getID() >= 128) throw new RuntimeException("Content " + c + " has ID > 127!");
                    stream.writeByte(m.getID());
                    stream.writeUTF(m.getContentName());
                }
            }
        }
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
