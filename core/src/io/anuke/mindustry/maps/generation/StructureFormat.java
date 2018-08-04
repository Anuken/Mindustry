package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectIntMap.Entry;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.world.Block;

import java.io.*;

public class StructureFormat{

    public static byte[] write(StructBlock[][] blocks){
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(out);

            ObjectIntMap<Block> mapping = new ObjectIntMap<>();
            int lastid = 1;
            mapping.put(Blocks.air, 0);

            for(int x = 0; x < blocks.length; x++){
                for(int y = 0; y < blocks[0].length; y++){
                    Block block = blocks[x][y].block;
                    if(!mapping.containsKey(block)){
                        mapping.put(block, lastid++);
                    }
                }
            }

            stream.writeByte(mapping.size);
            for(Entry<Block> entry : mapping){
                stream.writeByte(entry.value);
                stream.writeUTF(entry.key.name);
            }

            stream.writeByte(blocks.length);
            stream.writeByte(blocks[0].length);

            for(int x = 0; x < blocks.length; x++){
                for(int y = 0; y < blocks[0].length; y++){
                    StructBlock block = blocks[x][y];
                    stream.writeByte(mapping.get(block.block, 0));
                    stream.writeByte(block.rotation);
                }
            }

            return out.toByteArray();

        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static String writeBase64(StructBlock[][] blocks){
        return new String(Base64Coder.encode(write(blocks)));
    }

    public static StructBlock[][] read(byte[] bytes){
        try{

            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes));
            byte size = stream.readByte();
            IntMap<Block> map = new IntMap<>();
            for(int i = 0; i < size; i++){
                map.put(stream.readByte(), Block.getByName(stream.readUTF()));
            }
            
            byte width = stream.readByte(), height = stream.readByte();
            StructBlock[][] blocks = new StructBlock[width][height];
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    blocks[x][y] = new StructBlock(map.get(stream.readByte()), stream.readByte());
                }
            }

            return blocks;
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    public static StructBlock[][] read(String base64){
        return read(Base64Coder.decode(base64));
    }

    public static class StructBlock{
        public final Block block;
        public final byte rotation;

        public StructBlock(Block block, byte rotation){
            this.block = block;
            this.rotation = rotation;
        }
    }
}
