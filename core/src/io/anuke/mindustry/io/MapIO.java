package io.anuke.mindustry.io;

import io.anuke.arc.collection.IntIntMap;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.LegacyColorMapper.LegacyBlock;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;

import java.io.*;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static io.anuke.mindustry.Vars.bufferSize;
import static io.anuke.mindustry.Vars.content;

/** Reads and writes map files. */
public class MapIO{
    public static final int version = 1;

    private static final int[] pngHeader = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static ObjectMap<String, Block> missingBlocks;

    private static void initBlocks(){
        if(missingBlocks != null) return;

        //stainedRocks, stainedRocksRed, stainedRocksYellow, stainedStoneYellow, stainedBoulder,
        missingBlocks = ObjectMap.of(
        "stained-stone", Blocks.shale,
        "stained-stone-red", Blocks.shale,
        "stained-stone-yellow", Blocks.shale,
        "stained-rocks", Blocks.shaleRocks,
        "stained-boulder", Blocks.shaleBoulder,
        "stained-rocks-red", Blocks.shaleRocks,
        "stained-rocks-yellow", Blocks.shaleRocks
        );
    }

    public static boolean isImage(FileHandle file){
        try(InputStream stream = file.read(32)){
            for(int i1 : pngHeader){
                if(stream.read() != i1){
                    return false;
                }
            }
            return true;
        }catch(IOException e){
            return false;
        }
    }

    public static Pixmap generatePreview(Map map) throws IOException{
        Time.mark();
        Pixmap floors = new Pixmap(map.width, map.height, Format.RGBA8888);
        Pixmap walls = new Pixmap(map.width, map.height, Format.RGBA8888);
        int black = Color.rgba8888(Color.BLACK);
        int shade = Color.rgba8888(0f, 0f, 0f, 0.5f);
        CachedTile tile = new CachedTile(){
            @Override
            public void setFloor(Floor type){
                floors.drawPixel(x, floors.getHeight() - 1 - y, colorFor(type, Blocks.air, Blocks.air, getTeam()));
            }

            @Override
            public void setOverlayID(byte b){
                if(b != 0)
                    floors.drawPixel(x, floors.getHeight() - 1 - y, colorFor(floor(), Blocks.air, content.block(b), getTeam()));
            }

            @Override
            protected void changed(){
                super.changed();
                int c = colorFor(Blocks.air, block(), Blocks.air, getTeam());
                if(c != black){
                    walls.drawPixel(x, floors.getHeight() - 1 - y, c);
                    floors.drawPixel(x, floors.getHeight() - 1 - y + 1, shade);
                }
            }
        };
        readTiles(map, (x, y) -> {
            tile.x = (short)x;
            tile.y = (short)y;
            return tile;
        });
        floors.drawPixmap(walls, 0, 0);
        walls.dispose();
        return floors;
    }

    public static Pixmap generatePreview(Tile[][] tiles){
        Pixmap pixmap = new Pixmap(tiles.length, tiles[0].length, Format.RGBA8888);
        for(int x = 0; x < pixmap.getWidth(); x++){
            for(int y = 0; y < pixmap.getHeight(); y++){
                Tile tile = tiles[x][y];
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, colorFor(tile.floor(), tile.block(), tile.overlay(), tile.getTeam()));
            }
        }
        return pixmap;
    }

    public static int colorFor(Block floor, Block wall, Block ore, Team team){
        if(wall.synthetic()){
            return team.intColor;
        }
        return Color.rgba8888(wall.solid ? wall.color : ore == Blocks.air ? floor.color : ore.color);
    }

    public static void writeMap(FileHandle file, Map map, Tile[][] tiles) throws IOException{
        OutputStream output = file.write(false, bufferSize);

        {
            DataOutputStream stream = new DataOutputStream(output);
            stream.writeInt(version);
            stream.writeInt(Version.build);
            stream.writeShort(tiles.length);
            stream.writeShort(tiles[0].length);
            stream.writeByte((byte)map.tags.size);

            for(Entry<String, String> entry : map.tags.entries()){
                stream.writeUTF(entry.key);
                stream.writeUTF(entry.value);
            }
        }

        try(DataOutputStream stream = new DataOutputStream(new DeflaterOutputStream(output))){
            int width = map.width, height = map.height;

            SaveIO.getSaveWriter().writeContentHeader(stream);

            //floor first
            for(int i = 0; i < tiles.length * tiles[0].length; i++){
                Tile tile = tiles[i % width][i / width];
                stream.writeByte(tile.getFloorID());
                stream.writeByte(tile.getOverlayID());
                int consecutives = 0;

                for(int j = i + 1; j < width * height && consecutives < 255; j++){
                    Tile nextTile = tiles[j % width][j / width];

                    if(nextTile.getFloorID() != tile.getFloorID() || nextTile.block() != Blocks.air || nextTile.getOverlayID() != tile.getOverlayID()){
                        break;
                    }

                    consecutives++;
                }

                stream.writeByte(consecutives);
                i += consecutives;
            }

            //then blocks
            for(int i = 0; i < tiles.length * tiles[0].length; i++){
                Tile tile = tiles[i % width][i / width];
                stream.writeByte(tile.getBlockID());

                if(tile.block() instanceof BlockPart){
                    stream.writeByte(tile.getLinkByte());
                }else if(tile.entity != null){
                    stream.writeByte(Pack.byteByte(tile.getTeamID(), tile.getRotation())); //team + rotation
                    stream.writeShort(/*(short)tile.entity.health*/tile.block().health); //health
                    tile.entity.writeConfig(stream);
                }else{
                    //write consecutive non-entity blocks
                    int consecutives = 0;

                    for(int j = i + 1; j < width * height && consecutives < 255; j++){
                        Tile nextTile = tiles[j % width][j / width];

                        if(nextTile.block() != tile.block()){
                            break;
                        }

                        consecutives++;
                    }

                    stream.writeByte(consecutives);
                    i += consecutives;
                }
            }
        }
    }

    public static Map readMap(FileHandle file, boolean custom) throws IOException{
        try(DataInputStream stream = new DataInputStream(file.read(1024))){
            ObjectMap<String, String> tags = new ObjectMap<>();

            //meta is uncompressed
            int version = stream.readInt();
            if(version == 0){
                return readLegacyMap(file, custom);
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

    /** Reads tiles from a map, version-agnostic. */
    public static void readTiles(Map map, Tile[][] tiles) throws IOException{
        readTiles(map, (x, y) -> tiles[x][y]);
    }

    /** Reads tiles from a map, version-agnostic. */
    public static void readTiles(Map map, TileProvider tiles) throws IOException{
        if(map.version == 0){
            readLegacyMmapTiles(map.file, tiles);
        }else if(map.version == version){
            readTiles(map.file, map.width, map.height, tiles);
        }else{
            throw new IOException("Unknown map version. What?");
        }
    }

    /** Reads tiles from a map in the new build-65 format. */
    private static void readTiles(FileHandle file, int width, int height, Tile[][] tiles) throws IOException{
        readTiles(file, width, height, (x, y) -> tiles[x][y]);
    }

    /** Reads tiles from a map in the new build-65 format. */
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

                MappableContent[][] c = SaveIO.getSaveWriter().readContentHeader(stream);

                try{
                    content.setTemporaryMapper(c);

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

                        if(block == Blocks.part){
                            tile.setLinkByte(stream.readByte());
                        }else if(tile.entity != null){
                            byte tr = stream.readByte();
                            short health = stream.readShort();

                            byte team = Pack.leftByte(tr);
                            byte rotation = Pack.rightByte(tr);

                            tile.setTeam(Team.all[team]);
                            tile.entity.health = /*health*/tile.block().health;
                            tile.setRotation(rotation);

                            tile.entity.readConfig(stream);
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

    //region legacy IO

    /** Reads a pixmap in the 3.5 pixmap format. */
    public static void readLegacyPixmap(Pixmap pixmap, Tile[][] tiles){
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
                                write.setBlock(Blocks.part);
                                write.setTeam(Team.blue);
                                write.setLinkByte(Pack.byteByte((byte)(dx - 1 + 8), (byte)(dy - 1 + 8)));
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

    /** Reads a pixmap in the old 4.0 .mmap format. */
    private static void readLegacyMmapTiles(FileHandle file, Tile[][] tiles) throws IOException{
        readLegacyMmapTiles(file, (x, y) -> tiles[x][y]);
    }

    /** Reads a mmap in the old 4.0 .mmap format. */
    private static void readLegacyMmapTiles(FileHandle file, TileProvider tiles) throws IOException{
        try(DataInputStream stream = new DataInputStream(file.read(bufferSize))){
            stream.readInt(); //version
            byte tagAmount = stream.readByte();

            for(int i = 0; i < tagAmount; i++){
                stream.readUTF(); //key
                stream.readUTF(); //val
            }

            initBlocks();

            //block id -> real id map
            IntIntMap map = new IntIntMap();
            IntIntMap oreMap = new IntIntMap();

            short blocks = stream.readShort();
            for(int i = 0; i < blocks; i++){
                short id = stream.readShort();
                String name = stream.readUTF();
                Block block = content.getByName(ContentType.block, name);
                if(block == null){
                    //substitute for replacement in missingBlocks if possible
                    if(missingBlocks.containsKey(name)){
                        block = missingBlocks.get(name);
                    }else if(name.startsWith("ore-")){ //an ore floor combination
                        String[] split = name.split("-");
                        String itemName = split[1], floorName = Strings.join("-", Arrays.copyOfRange(split, 2, split.length));
                        Item item = content.getByName(ContentType.item, itemName);
                        Block oreBlock = item == null ? null : content.getByName(ContentType.block, "ore-" + item.name);
                        Block floor = missingBlocks.get(floorName, content.getByName(ContentType.block, floorName));
                        if(oreBlock != null && floor != null){
                            oreMap.put(id, oreBlock.id);
                            block = floor;
                        }else{
                            block = Blocks.air;
                        }
                    }else{
                        block = Blocks.air;
                    }

                }
                map.put(id, block.id);
            }
            short width = stream.readShort(), height = stream.readShort();

            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    Tile tile = tiles.get(x, y);
                    byte floorb = stream.readByte();
                    byte blockb = stream.readByte();
                    byte link = stream.readByte();
                    byte rotTeamb = stream.readByte();
                    stream.readByte();//unused stuff

                    tile.setFloor((Floor)content.block(map.get(floorb, 0)));
                    tile.setBlock(content.block(map.get(blockb, 0)));
                    tile.setRotation(Pack.leftByte(rotTeamb));
                    if(tile.block().synthetic()){
                        tile.setTeam(Team.all[Mathf.clamp(Pack.rightByte(rotTeamb), 0, Team.all.length)]);
                    }

                    if(tile.block() == Blocks.part){
                        tile.setLinkByte(link);
                    }

                    if(oreMap.containsKey(floorb)){
                        tile.setOverlay(content.block(oreMap.get(floorb, 0)));
                    }
                }
            }
        }
    }

    private static Map readLegacyMap(FileHandle file, boolean custom) throws IOException{
        try(DataInputStream stream = new DataInputStream(file.read(bufferSize))){
            ObjectMap<String, String> tags = new ObjectMap<>();

            int version = stream.readInt();
            if(version != 0) throw new IOException("Attempted to read non-legacy map in legacy method!");
            byte tagAmount = stream.readByte();

            for(int i = 0; i < tagAmount; i++){
                String name = stream.readUTF();
                String value = stream.readUTF();
                tags.put(name, value);
            }

            short blocks = stream.readShort();
            for(int i = 0; i < blocks; i++){
                stream.readShort();
                stream.readUTF();
            }
            short width = stream.readShort(), height = stream.readShort();

            //note that build 64 is the default build of all maps <65; while this can be inaccurate it's better than nothing
            return new Map(file, width, height, tags, custom, 0, 64);
        }
    }

    //endregion

    interface TileProvider{
        Tile get(int x, int y);
    }
}