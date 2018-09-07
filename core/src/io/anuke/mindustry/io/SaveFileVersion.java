package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Array;
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

    public MappableContent[][] readContentHeader(DataInputStream stream) throws IOException{

        byte mapped = stream.readByte();

        MappableContent[][] map = new MappableContent[ContentType.values().length][0];

        for (int i = 0; i < mapped; i++) {
            ContentType type = ContentType.values()[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];

            for (int j = 0; j < total; j++) {
                String name = stream.readUTF();
                map[type.ordinal()][j] = content.getByName(type, name);
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
                    stream.writeUTF(((MappableContent)c).getContentName());
                }
            }
        }
    }

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;
}
