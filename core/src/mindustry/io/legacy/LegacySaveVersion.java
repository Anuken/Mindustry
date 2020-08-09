package mindustry.io.legacy;

import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.content;

public abstract class LegacySaveVersion extends SaveVersion{

    public LegacySaveVersion(int version){
        super(version);
    }

    @Override
    public void readMap(DataInput stream, WorldContext context) throws IOException{
        int width = stream.readUnsignedShort();
        int height = stream.readUnsignedShort();

        boolean generating = context.isGenerating();

        if(!generating) context.begin();
        try{

            context.resize(width, height);

            //read floor and create tiles first
            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                short floorid = stream.readShort();
                short oreid = stream.readShort();
                int consecutives = stream.readUnsignedByte();
                if(content.block(floorid) == Blocks.air) floorid = Blocks.stone.id;

                context.create(x, y, floorid, oreid, (short)0);

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    context.create(newx, newy, floorid, oreid, (short)0);
                }

                i += consecutives;
            }

            //read blocks
            for(int i = 0; i < width * height; i++){
                Block block = content.block(stream.readShort());
                Tile tile = context.tile(i);
                if(block == null) block = Blocks.air;

                //occupied by multiblock part
                boolean occupied = tile.build != null && !tile.isCenter() && (tile.build.block() == block || block == Blocks.air);

                //do not override occupied cells
                if(!occupied){
                    tile.setBlock(block);
                }

                if(block.hasEntity()){
                    try{
                        readChunk(stream, true, in -> {
                            byte version = in.readByte();
                            //legacy impl of Building#read()
                            tile.build.health = stream.readUnsignedShort();
                            byte packedrot = stream.readByte();
                            byte team = Pack.leftByte(packedrot) == 8 ? stream.readByte() : Pack.leftByte(packedrot);
                            byte rotation = Pack.rightByte(packedrot);

                            tile.setTeam(Team.get(team));
                            tile.build.rotation = rotation;

                            if(tile.build.items != null) tile.build.items.read(Reads.get(stream));
                            if(tile.build.power != null) tile.build.power.read(Reads.get(stream));
                            if(tile.build.liquids != null) tile.build.liquids.read(Reads.get(stream));
                            if(tile.build.cons != null) tile.build.cons.read(Reads.get(stream));

                            //read only from subclasses!
                            tile.build.read(Reads.get(in), version);
                        });
                    }catch(Throwable e){
                        throw new IOException("Failed to read tile entity of block: " + block, e);
                    }
                }else{
                    int consecutives = stream.readUnsignedByte();

                    //air is a waste of time and may mess up multiblocks
                    if(block != Blocks.air){
                        for(int j = i + 1; j < i + 1 + consecutives; j++){
                            context.tile(j).setBlock(block);
                        }
                    }

                    i += consecutives;
                }
            }
        }finally{
            if(!generating) context.end();
        }
    }

    public void readLegacyEntities(DataInput stream) throws IOException{
        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                //simply skip all the entities
                skipChunk(stream, true);
            }
        }
    }
}
