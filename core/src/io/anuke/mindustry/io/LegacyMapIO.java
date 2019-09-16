package io.anuke.mindustry.io;

import io.anuke.arc.collection.*;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.Pack;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.serialization.Json;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapIO.TileProvider;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.LegacyColorMapper.LegacyBlock;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;

import java.io.*;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.*;

/** Map IO for the "old" .mmap format.
 * Differentiate between legacy maps and new maps by checking the extension (or the header).*/
public class LegacyMapIO{
    private static final ObjectMap<String, String> fallback = ObjectMap.of("alpha-dart-mech-pad", "dart-mech-pad");
    private static final Json json = new Json();

    /* Convert a map from the old format to the new format. */
    public static void convertMap(FileHandle in, FileHandle out) throws IOException{
        Map map = readMap(in, true);

        String waves = map.tags.get("waves", "[]");
        Array<SpawnGroup> groups = new Array<>(json.fromJson(SpawnGroup[].class, waves));

        Tile[][] tiles = world.createTiles(map.width, map.height);
        for(int x = 0; x < map.width; x++){
            for(int y = 0; y < map.height; y++){
                tiles[x][y] = new CachedTile();
                tiles[x][y].x = (short)x;
                tiles[x][y].y = (short)y;
            }
        }
        state.rules.spawns = groups;
        readTiles(map, tiles);
        MapIO.writeMap(out, map);
    }

    public static Map readMap(FileHandle file, boolean custom) throws IOException{
        try(DataInputStream stream = new DataInputStream(file.read(1024))){
            StringMap tags = new StringMap();

            //meta is uncompressed
            int version = stream.readInt();
            if(version != 1){
                throw new IOException("Outdated legacy map format");
            }
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
                    byte mapped = stream.readByte();
                    IntMap<Block> idmap = new IntMap<>();
                    IntMap<String> namemap = new IntMap<>();

                    for(int i = 0; i < mapped; i++){
                        byte type = stream.readByte();
                        short total = stream.readShort();

                        for(int j = 0; j < total; j++){
                            String name = stream.readUTF();
                            if(type == 1){
                                Block res = content.getByName(ContentType.block, fallback.get(name, name));
                                idmap.put(j, res == null ? Blocks.air : res);
                                namemap.put(j, fallback.get(name, name));
                            }
                        }
                    }

                    //read floor and create tiles first
                    for(int i = 0; i < width * height; i++){
                        int x = i % width, y = i / width;
                        int floorid = stream.readUnsignedByte();
                        int oreid = stream.readUnsignedByte();
                        int consecutives = stream.readUnsignedByte();

                        Tile tile = tiles.get(x, y);
                        tile.setFloor((Floor)idmap.get(floorid));
                        tile.setOverlay(idmap.get(oreid));

                        for(int j = i + 1; j < i + 1 + consecutives; j++){
                            int newx = j % width, newy = j / width;
                            Tile newTile = tiles.get(newx, newy);
                            newTile.setFloor((Floor)idmap.get(floorid));
                            newTile.setOverlay(idmap.get(oreid));
                        }

                        i += consecutives;
                    }

                    //read blocks
                    for(int i = 0; i < width * height; i++){
                        int x = i % width, y = i / width;
                        int id = stream.readUnsignedByte();
                        Block block = idmap.get(id);
                        if(block == null) block = Blocks.air;

                        Tile tile = tiles.get(x, y);
                        //the spawn block is saved in the block tile layer in older maps, shift it to the overlay
                        if(block != Blocks.spawn){
                            tile.setBlock(block);
                        }else{
                            tile.setOverlay(block);
                        }

                        if(namemap.get(id, "").equals("part")){
                            stream.readByte(); //link
                        }else if(tile.entity != null){
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
                if(color == Color.rgba8888(Color.green)){
                    for(int dx = 0; dx < 3; dx++){
                        for(int dy = 0; dy < 3; dy++){
                            int worldx = dx - 1 + x;
                            int worldy = dy - 1 + y;

                            //multiblock parts
                            if(Structs.inBounds(worldx, worldy, pixmap.getWidth(), pixmap.getHeight())){
                                Tile write = tiles[worldx][worldy];
                                write.setBlock(BlockPart.get(dx - 1, dy - 1));
                                write.setTeam(Team.sharded);
                            }
                        }
                    }

                    //actual core parts
                    tile.setBlock(Blocks.coreShard);
                    tile.setTeam(Team.sharded);
                }
            }
        }
    }
}
