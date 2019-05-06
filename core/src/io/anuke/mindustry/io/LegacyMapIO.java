package io.anuke.mindustry.io;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.Pack;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO.TileProvider;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.LegacyColorMapper.LegacyBlock;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;

import java.io.*;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.bufferSize;
import static io.anuke.mindustry.Vars.content;

/** Map IO for the "old" .mmap format.
 * Differentiate between legacy maps and new maps by checking the extension (or the header).*/
public class LegacyMapIO{

    public static Map readMap(FileHandle file, boolean custom) throws IOException{
        try(DataInputStream stream = new DataInputStream(file.read(1024))){
            ObjectMap<String, String> tags = new ObjectMap<>();

            //meta is uncompressed
            int version = stream.readInt();
            int build = stream.readInt();
            short width = stream.readShort(), height = stream.readShort();
            byte tagAmount = stream.readByte();

            for(int i = 0; i < tagAmount; i++){
                String name = stream.readUTF();
                String value = stream.readUTF();
                tags.put(name, value);
            }

            return new Map(file, width, height, tags, custom, version, build);
        }
    }

    public static void readTiles(Map map, Tile[][] tiles) throws IOException{
        readTiles(map, (x, y) -> tiles[x][y]);
    }

    public static void readTiles(Map map, TileProvider tiles) throws IOException{
        readTiles(map.file, map.width, map.height, tiles);
    }

    private static void readTiles(FileHandle file, int width, int height, Tile[][] tiles) throws IOException{
        readTiles(file, width, height, (x, y) -> tiles[x][y]);
    }

    private static void readTiles(FileHandle file, int width, int height, TileProvider tiles) throws IOException{
        try(BufferedInputStream input = file.read(bufferSize)){

            //read map
            {
                DataInputStream stream = new DataInputStream(input);

                stream.readInt(); //version
                stream.readInt(); //build
                stream.readInt(); //width + height
                byte tagAmount = stream.readByte();

                for(int i = 0; i < tagAmount; i++){
                    stream.readUTF(); //key
                    stream.readUTF(); //val
                }
            }

            try(DataInputStream stream = new DataInputStream(new InflaterInputStream(input))){

                try{
                    SaveIO.getSaveWriter().readContentHeader(stream);

                    //read floor and create tiles first
                    for(int i = 0; i < width * height; i++){
                        int x = i % width, y = i / width;
                        byte floorid = stream.readByte();
                        byte oreid = stream.readByte();
                        int consecutives = stream.readUnsignedByte();

                        Tile tile = tiles.get(x, y);
                        tile.setFloor((Floor)content.block(floorid));
                        tile.setOverlay(content.block(oreid));

                        for(int j = i + 1; j < i + 1 + consecutives; j++){
                            int newx = j % width, newy = j / width;
                            Tile newTile = tiles.get(newx, newy);
                            newTile.setFloor((Floor)content.block(floorid));
                            newTile.setOverlay(content.block(oreid));
                        }

                        i += consecutives;
                    }

                    //read blocks
                    for(int i = 0; i < width * height; i++){
                        int x = i % width, y = i / width;
                        Block block = content.block(stream.readByte());

                        Tile tile = tiles.get(x, y);
                        tile.setBlock(block);

                        if(tile.entity != null){
                            byte tr = stream.readByte();
                            stream.readShort(); //read health (which is actually irrelevant)

                            byte team = Pack.leftByte(tr);
                            byte rotation = Pack.rightByte(tr);

                            tile.setTeam(Team.all[team]);
                            tile.entity.health = tile.block().health;
                            tile.rotation(rotation);

                            if(tile.block() == Blocks.liquidSource || tile.block() == Blocks.unloader || tile.block() == Blocks.sorter){
                                stream.readByte(); //these blocks have an extra config byte, read it
                            }
                        }else{ //no entity/part, read consecutives
                            int consecutives = stream.readUnsignedByte();

                            for(int j = i + 1; j < i + 1 + consecutives; j++){
                                int newx = j % width, newy = j / width;
                                tiles.get(newx, newy).setBlock(block);
                            }

                            i += consecutives;
                        }
                    }

                }finally{
                    content.setTemporaryMapper(null);
                }
            }
        }
    }

    /** Reads a pixmap in the 3.5 pixmap format. */
    public static void readPixmap(Pixmap pixmap, Tile[][] tiles){
        for(int x = 0; x < pixmap.getWidth(); x++){
            for(int y = 0; y < pixmap.getHeight(); y++){
                int color = pixmap.getPixel(x, pixmap.getHeight() - 1 - y);
                LegacyBlock block = LegacyColorMapper.get(color);
                Tile tile = tiles[x][y];

                tile.setFloor(block.floor);
                tile.setBlock(block.wall);
                if(block.ore != null) tile.setOverlay(block.ore);

                //place core
                if(color == Color.rgba8888(Color.GREEN)){
                    for(int dx = 0; dx < 3; dx++){
                        for(int dy = 0; dy < 3; dy++){
                            int worldx = dx - 1 + x;
                            int worldy = dy - 1 + y;

                            //multiblock parts
                            if(Structs.inBounds(worldx, worldy, pixmap.getWidth(), pixmap.getHeight())){
                                Tile write = tiles[worldx][worldy];
                                write.setBlock(BlockPart.get(dx - 1, dy - 1));
                                write.setTeam(Team.blue);
                            }
                        }
                    }

                    //actual core parts
                    tile.setBlock(Blocks.coreShard);
                    tile.setTeam(Team.blue);
                }
            }
        }
    }
}
