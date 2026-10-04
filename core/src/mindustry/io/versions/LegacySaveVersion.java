package mindustry.io.versions;

import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

public abstract class LegacySaveVersion extends LegacyRegionSaveVersion{

    public LegacySaveVersion(int version){
        super(version);
    }

    @Override
    public void readMap(DataInput stream, SaveLoadContext context) throws IOException{
        int width = stream.readUnsignedShort();
        int height = stream.readUnsignedShort();

        boolean generating = context.isGenerating();

        if(!generating) context.begin();
        try{
            context.resize(width, height);

            //read floor and create tiles first
            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                Block floor = context.reads.content(ContentType.block, stream.readShort());
                Block overlay = context.reads.content(ContentType.block, stream.readShort());
                int consecutives = stream.readUnsignedByte();
                if(floor == Blocks.air) floor = Blocks.stone;
                int floorid = floor == null ? Blocks.stone.id : floor.id, oreid = overlay == null ? Blocks.air.id : overlay.id;

                context.create(x, y, floorid, oreid, (short)0);

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    context.create(newx, newy, floorid, oreid, (short)0);
                }

                i += consecutives;
            }

            //read blocks
            for(int i = 0; i < width * height; i++){
                Block block = context.reads.content(ContentType.block, stream.readShort());
                Tile tile = context.tile(i);
                if(block == null) block = Blocks.air;

                //occupied by multiblock part
                boolean occupied = tile.build != null && !tile.isCenter() && (tile.build.block == block || block == Blocks.air);

                //do not override occupied cells
                if(!occupied){
                    tile.setBlock(block);
                    if(tile.build != null){
                        if(!context.preview) context.allBuildings.add(tile.build);
                        tile.build.enabled = true;
                    }
                }

                if(block.hasBuilding()){
                    try{
                        readLegacyShortChunk(stream, context.reads, (in, len) -> {
                            byte version = in.b();
                            //legacy impl of Building#read()
                            tile.build.health = stream.readUnsignedShort();
                            byte packedrot = stream.readByte();
                            byte team = Pack.leftByte(packedrot) == 8 ? stream.readByte() : Pack.leftByte(packedrot);
                            byte rotation = Pack.rightByte(packedrot);

                            tile.setTeam(Team.get(team));
                            tile.build.rotation = rotation;

                            if(tile.build.items != null) tile.build.items.read(in, true);
                            if(tile.build.power != null) tile.build.power.read(in, true);
                            if(tile.build.liquids != null) tile.build.liquids.read(in, true);
                            //skip cons.valid boolean, it's not very important here
                            stream.readByte();

                            //read only from subclasses!
                            tile.build.read(in, version);
                        });
                    }catch(Throwable e){
                        throw new IOException("Failed to read tile entity of block: " + block, e);
                    }

                    context.onReadBuilding();
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
                skipLegacyShortChunk(stream);
            }
        }
    }
}
