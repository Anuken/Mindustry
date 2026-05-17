package mindustry.io.versions;

import arc.func.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

public class ShortChunkSaveVersion extends SaveVersion{

    public ShortChunkSaveVersion(int version){
        super(version);
    }

    @Override
    public void readWorldEntities(DataInput stream, Prov[] mapping) throws IOException{

        int amount = stream.readInt();
        for(int j = 0; j < amount; j++){
            readLegacyShortChunk(stream, (in, len) -> {
                int typeid = in.ub();
                if(mapping[typeid] == null){
                    in.skip(len - 1);
                    return;
                }

                int id = in.i();

                Entityc entity = (Entityc)mapping[typeid].get();
                EntityGroup.checkNextId(id);
                entity.id(id);
                entity.read(in);
                entity.add();
            });
        }

        Groups.all.each(Entityc::afterReadAll);
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
                boolean isCenter = true;
                byte packedCheck = stream.readByte();
                boolean hadEntity = (packedCheck & 1) != 0;
                //old data format (bit 2): 1 byte only if no building is present
                //new data format (bit 3): 7 bytes (3x block-specific bytes + 1x 4-byte extra data int)
                boolean hadDataOld = (packedCheck & 2) != 0, hadDataNew = (packedCheck & 4) != 0;

                byte data = 0, floorData = 0, overlayData = 0;
                int extraData = 0;

                if(hadDataNew){
                    data = stream.readByte();
                    floorData = stream.readByte();
                    overlayData = stream.readByte();
                    extraData = stream.readInt();
                }

                if(hadEntity){
                    isCenter = stream.readBoolean();
                }

                //set block only if this is the center; otherwise, it's handled elsewhere
                if(isCenter){
                    tile.setBlock(block);
                    if(tile.build != null) tile.build.enabled = true;
                }

                //must be assigned after setBlock, because that can reset data
                if(hadDataNew){
                    tile.data = data;
                    tile.floorData = floorData;
                    tile.overlayData = overlayData;
                    tile.extraData = extraData;
                    context.onReadTileData();
                }

                if(hadEntity){
                    if(isCenter){ //only read entity for center blocks
                        if(block.hasBuilding()){
                            try{
                                readLegacyShortChunk(stream, (in, len) -> {
                                    byte revision = in.b();
                                    tile.build.readAll(in, revision);
                                });
                            }catch(Throwable e){
                                throw new IOException("Failed to read tile entity of block: " + block, e);
                            }
                        }else{
                            //skip the entity region, as the entity and its IO code are now gone
                            skipLegacyShortChunk(stream);
                        }

                        context.onReadBuilding();
                    }
                }else if(hadDataOld || hadDataNew){ //never read consecutive blocks if there's any kind of data
                    if(hadDataOld){
                        tile.setBlock(block);
                        //the old data format was only read in the case where there is no building, and only contained a single byte
                        tile.data = stream.readByte();
                    }
                }else{
                    int consecutives = stream.readUnsignedByte();

                    for(int j = i + 1; j < i + 1 + consecutives; j++){
                        context.tile(j).setBlock(block);
                    }

                    i += consecutives;
                }
            }
        }finally{
            if(!generating) context.end();
        }
    }
}
